package cn.structured.datascope.mybatis.interceptor;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限SQL拦截器
 * <p>
 * 自动将数据权限条件注入到所有SELECT语句中，对业务层完全透明
 * </p>
 *
 * @author chuck
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    private final DataScopeFieldConfig fieldConfig;

    public DataScopeInterceptor(DataScopeFieldConfig fieldConfig) {
        this.fieldConfig = fieldConfig;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 只拦截 SELECT 操作
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (sqlCommandType != SqlCommandType.SELECT) {
            return;
        }

        // 获取数据权限上下文
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        // 如果没有数据权限信息，直接放行
        if (orgId == null && (deptIds == null || deptIds.isEmpty())) {
            log.debug("No data scope context, bypass interception");
            return;
        }

        String originalSql = boundSql.getSql();
        TableInfo tableInfo = extractTableInfo(originalSql);

        if (tableInfo == null) {
            log.debug("Cannot extract table name from SQL, bypass interception");
            return;
        }

        // 构建数据权限条件，使用表名或别名
        String tableAlias = tableInfo.alias != null ? tableInfo.alias : tableInfo.tableName;
        String dataScopeCondition = buildDataScopeCondition(tableAlias);

        // 注入数据权限条件到SQL
        String modifiedSql = injectDataScopeCondition(originalSql, dataScopeCondition, tableInfo.insertPosition);

        log.debug("Original SQL: {}", originalSql);
        log.debug("Modified SQL: {}", modifiedSql);

        // 修改SQL（通过反射更新BoundSql）
        try {
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, modifiedSql);
        } catch (Exception e) {
            log.error("Failed to modify SQL", e);
        }
    }

    /**
     * 从SQL中提取表名和别名
     * <p>
     * 支持格式：
     * - FROM orders
     * - FROM orders o
     * - FROM orders AS o
     * - FROM orders, users
     * - FROM orders o LEFT JOIN users u ON ...
     * </p>
     */
    private TableInfo extractTableInfo(String sql) {
        String lowerSql = sql.toLowerCase();
        int fromIndex = lowerSql.indexOf(" from ");

        if (fromIndex == -1) {
            return null;
        }

        // 从 FROM 之后开始解析
        String afterFrom = sql.substring(fromIndex + 6).trim();
        int afterFromIndex = fromIndex + 6;

        // 提取表名（跳过引号和空白，找到第一个词）
        String tableName = extractFirstWord(afterFrom);

        if (tableName == null || tableName.isEmpty()) {
            return null;
        }

        // 检查是否提取到了关键字（错误情况）
        if (isSqlKeyword(tableName.toLowerCase())) {
            log.warn("Extracted table name '{}' is a SQL keyword, skipping", tableName);
            return null;
        }

        // 计算表名结束位置（在原始SQL中的位置）
        int tableNameEnd = tableName.length();
        String afterTable = afterFrom.substring(tableNameEnd).trim();
        String alias = null;
        int aliasEnd = -1;

        // 跳过 AS 关键字
        if (afterTable.toLowerCase().startsWith("as ")) {
            afterTable = afterTable.substring(3).trim();
        } else if (afterTable.toLowerCase().startsWith("as")) {
            afterTable = afterTable.substring(2).trim();
        }

        // 检查是否有别名（下一个词不是SQL关键字）
        if (!afterTable.isEmpty()) {
            String nextWord = extractFirstWord(afterTable);
            if (nextWord != null && !isSqlKeyword(nextWord.toLowerCase()) && !nextWord.equals(",")) {
                alias = nextWord;
                // 别名结束位置 = afterFrom中表名后的位置 + 别名在afterTable中的偏移量
                int afterTableNameOffset = tableNameEnd + (afterFrom.length() - afterTable.length());
                aliasEnd = afterFromIndex + afterTableNameOffset + nextWord.length();
            }
        }

        // 计算插入位置：表名之后（或别名之后）
        int insertPosition;
        if (alias != null && aliasEnd > 0) {
            insertPosition = aliasEnd;
        } else {
            insertPosition = afterFromIndex + tableNameEnd;
        }

        // 验证插入位置不超过SQL长度
        if (insertPosition > sql.length()) {
            insertPosition = sql.length();
        }

        return new TableInfo(tableName, alias, insertPosition);
    }

    /**
     * 提取字符串中的第一个单词
     */
    private String extractFirstWord(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        str = str.trim();

        // 跳过空白
        int start = 0;
        while (start < str.length() && Character.isWhitespace(str.charAt(start))) {
            start++;
        }

        if (start >= str.length()) {
            return null;
        }

        // 检查是否以引号开头
        char firstChar = str.charAt(start);
        if (firstChar == '`' || firstChar == '"' || firstChar == '\'') {
            int end = start + 1;
            while (end < str.length() && str.charAt(end) != firstChar) {
                end++;
            }
            if (end < str.length()) {
                return str.substring(start + 1, end);
            }
            return null;
        }

        // 普通单词：找到空白或标点符号
        int end = start;
        while (end < str.length() && !Character.isWhitespace(str.charAt(end))
                && str.charAt(end) != ',' && str.charAt(end) != '(' && str.charAt(end) != ')') {
            end++;
        }

        return str.substring(start, end);
    }

    /**
     * 检查是否是 SQL 关键字
     */
    private boolean isSqlKeyword(String word) {
        if (word == null) {
            return false;
        }
        // 精确匹配
        String lower = word.toLowerCase();
        return "from".equals(lower) || "select".equals(lower) ||
                "where".equals(lower) || "order".equals(lower) ||
                "group".equals(lower) || "having".equals(lower) ||
                "limit".equals(lower) || "join".equals(lower) ||
                "inner".equals(lower) || "left".equals(lower) ||
                "right".equals(lower) || "cross".equals(lower) ||
                "on".equals(lower) || "and".equals(lower) ||
                "or".equals(lower) || "union".equals(lower) ||
                "as".equals(lower) || "not".equals(lower) ||
                "in".equals(lower) || "exists".equals(lower) ||
                "between".equals(lower) || "like".equals(lower) ||
                "is".equals(lower) || "null".equals(lower);
    }

    /**
     * 构建数据权限条件
     */
    private String buildDataScopeCondition(String tableAlias) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        List<String> conditions = new ArrayList<>();

        // 组织ID条件
        if (orgId != null && !orgId.isEmpty()) {
            conditions.add(tableAlias + "." + fieldConfig.getOrgIdField() + " = '" + orgId + "'");
        }

        // 部门ID条件
        if (deptIds != null && !deptIds.isEmpty()) {
            String deptIdField = fieldConfig.getDeptIdField();
            String deptIdList = String.join("','", deptIds);
            conditions.add(tableAlias + "." + deptIdField + " IN ('" + deptIdList + "')");
        }

        return String.join(" AND ", conditions);
    }

    /**
     * 将数据权限条件注入到SQL中
     */
    private String injectDataScopeCondition(String originalSql, String dataScopeCondition, int afterTableIndex) {
        String lowerSql = originalSql.toLowerCase();
        int whereIndex = lowerSql.indexOf(" where ", afterTableIndex);

        if (whereIndex == -1) {
            // 没有WHERE子句，在表名/别名之后添加WHERE
            return originalSql.substring(0, afterTableIndex) + " WHERE " + dataScopeCondition + originalSql.substring(afterTableIndex);
        } else {
            // 有WHERE子句，在WHERE后面添加AND条件
            int afterWherePos = whereIndex + 7;
            return originalSql.substring(0, afterWherePos) + "(" + dataScopeCondition + ") AND " + originalSql.substring(afterWherePos);
        }
    }

    /**
     * 表信息内部类
     */
    private static class TableInfo {
        final String tableName;
        final String alias;
        final int insertPosition;

        TableInfo(String tableName, String alias, int insertPosition) {
            this.tableName = tableName;
            this.alias = alias;
            this.insertPosition = insertPosition;
        }
    }
}
