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
import java.util.Collections;
import java.util.List;

/**
 * 数据权限SQL拦截器
 * <p>
 * 自动将部门数据权限条件注入到所有SELECT语句中，对业务层完全透明
 * <p>
 * 支持多表查询场景，可通过 {@link MultiTableDataScopeHandler} 自定义处理逻辑
 * </p>
 *
 * @author chuck
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    private final DataScopeFieldConfig fieldConfig;
    private MultiTableDataScopeHandler multiTableDataScopeHandler;

    public DataScopeInterceptor(DataScopeFieldConfig fieldConfig) {
        this.fieldConfig = fieldConfig;
        this.multiTableDataScopeHandler = new DefaultMultiTableDataScopeHandler();
    }

    /**
     * 设置自定义多表数据权限处理器
     *
     * @param multiTableDataScopeHandler 多表处理器
     */
    public void setMultiTableDataScopeHandler(MultiTableDataScopeHandler multiTableDataScopeHandler) {
        this.multiTableDataScopeHandler = multiTableDataScopeHandler;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (sqlCommandType != SqlCommandType.SELECT) {
            return;
        }

        List<String> deptIds = DataScopeContext.getDeptIds();
        if (deptIds == null || deptIds.isEmpty()) {
            log.debug("No department data scope context, bypass interception");
            return;
        }

        String originalSql = boundSql.getSql();
        List<TableInfo> tableInfos = extractAllTableInfo(originalSql);

        if (tableInfos.isEmpty()) {
            log.debug("Cannot extract table name from SQL, bypass interception");
            return;
        }

        log.debug("Extracted tables: {}", tableInfos);

        // 使用多表处理器处理数据权限
        String modifiedSql = multiTableDataScopeHandler.handleMultiTable(originalSql, tableInfos, fieldConfig);

        log.debug("Original SQL: {}", originalSql);
        log.debug("Modified SQL: {}", modifiedSql);

        try {
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, modifiedSql);
        } catch (Exception e) {
            log.error("Failed to modify SQL", e);
        }
    }

    /**
     * 提取SQL中的所有表信息
     * <p>
     * 支持格式：
     * - FROM orders
     * - FROM orders o
     * - FROM orders AS o
     * - FROM orders, users
     * - FROM orders o LEFT JOIN users u ON ...
     * - SELECT * FROM (SELECT * FROM orders) t
     * </p>
     */
    private List<TableInfo> extractAllTableInfo(String sql) {
        List<TableInfo> tableInfos = new ArrayList<>();
        String lowerSql = sql.toLowerCase();

        List<Integer> fromPositions = new ArrayList<>();

        int index = 0;
        while ((index = lowerSql.indexOf(" from ", index)) != -1) {
            fromPositions.add(index);
            index += 6;
        }

        String[] joinKeywords = {" inner join ", " left join ", " right join ", " full join ", " cross join ", " join "};
        for (String joinKeyword : joinKeywords) {
            index = 0;
            while ((index = lowerSql.indexOf(joinKeyword, index)) != -1) {
                fromPositions.add(index);
                index += joinKeyword.length();
            }
        }

        Collections.sort(fromPositions);

        for (int fromPos : fromPositions) {
            int keywordLength = getKeywordLength(lowerSql, fromPos);
            String afterFrom = sql.substring(fromPos + keywordLength).trim();
            int afterFromIndex = fromPos + keywordLength;

            String tableName = extractFirstWord(afterFrom);

            if (tableName == null || tableName.isEmpty()) {
                continue;
            }

            if (isSqlKeyword(tableName.toLowerCase())) {
                continue;
            }

            int tableNameEnd = tableName.length();
            String afterTable = afterFrom.substring(tableNameEnd).trim();
            String alias = null;
            int aliasEnd = -1;

            if (afterTable.toLowerCase().startsWith("as ")) {
                afterTable = afterTable.substring(3).trim();
            } else if (afterTable.toLowerCase().startsWith("as")) {
                afterTable = afterTable.substring(2).trim();
            }

            if (!afterTable.isEmpty()) {
                String nextWord = extractFirstWord(afterTable);
                if (nextWord != null && !isSqlKeyword(nextWord.toLowerCase()) && !nextWord.equals(",")) {
                    alias = nextWord;
                    int afterTableNameOffset = tableNameEnd + (afterFrom.length() - afterTable.length());
                    aliasEnd = afterFromIndex + afterTableNameOffset + nextWord.length();
                }
            }

            int insertPosition;
            if (alias != null && aliasEnd > 0) {
                insertPosition = aliasEnd;
            } else {
                insertPosition = afterFromIndex + tableNameEnd;
            }

            if (insertPosition > sql.length()) {
                insertPosition = sql.length();
            }

            tableInfos.add(new TableInfo(tableName, alias, insertPosition));
        }

        return tableInfos;
    }

    /**
     * 获取SQL关键字的长度
     */
    private int getKeywordLength(String lowerSql, int position) {
        String[] keywords = {" from ", " inner join ", " left join ", " right join ", " full join ", " cross join ", " join "};
        for (String keyword : keywords) {
            if (lowerSql.startsWith(keyword, position)) {
                return keyword.length();
            }
        }
        return 6;
    }

    /**
     * 提取字符串中的第一个单词
     */
    private String extractFirstWord(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        str = str.trim();

        int start = 0;
        while (start < str.length() && Character.isWhitespace(str.charAt(start))) {
            start++;
        }

        if (start >= str.length()) {
            return null;
        }

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
}