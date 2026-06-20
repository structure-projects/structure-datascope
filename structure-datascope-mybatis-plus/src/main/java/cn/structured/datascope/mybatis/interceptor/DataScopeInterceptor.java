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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 匹配 FROM 子句后的表名（支持 schema.table 和别名）
     */
    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile(
            "\\s+from\\s+([`\"']?\\w+[`\"']?)(?:\\s+(?:as\\s+)?[`\"']?\\w+[`\"']?)?\\s*",
            Pattern.CASE_INSENSITIVE
    );

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
        String modifiedSql = injectDataScopeCondition(originalSql, dataScopeCondition, tableInfo.whereStartIndex);

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
     */
    private TableInfo extractTableInfo(String sql) {
        Matcher matcher = FROM_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1).replace("`", "").replace("\"", "").replace("'", "");
            int whereStartIndex = matcher.end();
            // 检查是否有别名
            String afterFrom = sql.substring(matcher.start(), matcher.end());
            String alias = null;
            
            // 尝试匹配别名
            Pattern aliasPattern = Pattern.compile(
                    "\\s+(?:as\\s+)?([`\"']?\\w+[`\"']?)\\s+",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher aliasMatcher = aliasPattern.matcher(afterFrom);
            if (aliasMatcher.find()) {
                alias = aliasMatcher.group(1).replace("`", "").replace("\"", "").replace("'", "");
            }
            
            return new TableInfo(tableName, alias, whereStartIndex);
        }
        return null;
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
    private String injectDataScopeCondition(String originalSql, String dataScopeCondition, int afterFromIndex) {
        String lowerSql = originalSql.toLowerCase();
        int whereIndex = lowerSql.indexOf(" where ", afterFromIndex);

        if (whereIndex == -1) {
            // 没有WHERE子句，在FROM后面添加WHERE
            int insertPos = findInsertPosition(originalSql, afterFromIndex);
            return originalSql.substring(0, insertPos) + " WHERE " + dataScopeCondition + originalSql.substring(insertPos);
        } else {
            // 有WHERE子句，在WHERE后面添加AND条件
            int afterWherePos = whereIndex + 7;
            return originalSql.substring(0, afterWherePos) + "(" + dataScopeCondition + ") AND " + originalSql.substring(afterWherePos);
        }
    }

    /**
     * 找到在FROM子句后的合适插入位置
     */
    private int findInsertPosition(String sql, int afterFromIndex) {
        // 跳过表名和可能的别名，找到下一个关键字的位置
        String remaining = sql.substring(afterFromIndex).toLowerCase();
        
        String[] keywords = {"where", "order by", "group by", "limit", "having", "union", "inner join", "left join", "right join", "cross join"};
        int earliestPos = sql.length();
        
        for (String keyword : keywords) {
            int pos = remaining.indexOf(keyword);
            if (pos != -1 && pos < earliestPos) {
                earliestPos = afterFromIndex + pos;
            }
        }
        
        return earliestPos;
    }

    /**
     * 表信息内部类
     */
    private static class TableInfo {
        final String tableName;
        final String alias;
        final int whereStartIndex;

        TableInfo(String tableName, String alias, int whereStartIndex) {
            this.tableName = tableName;
            this.alias = alias;
            this.whereStartIndex = whereStartIndex;
        }
    }
}
