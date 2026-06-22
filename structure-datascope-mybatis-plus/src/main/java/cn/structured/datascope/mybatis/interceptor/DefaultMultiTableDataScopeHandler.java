package cn.structured.datascope.mybatis.interceptor;

import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.DataScopeContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 默认多表数据权限处理器
 * <p>
 * 策略：只对第一个表添加数据权限条件，将条件放在 SQL 最后面
 * </p>
 */
@Slf4j
public class DefaultMultiTableDataScopeHandler implements MultiTableDataScopeHandler {

    @Override
    public String handleMultiTable(String sql, List<TableInfo> tableInfos, DataScopeFieldConfig fieldConfig) {
        if (tableInfos == null || tableInfos.isEmpty()) {
            return sql;
        }

        // 默认策略：只对第一个表添加数据权限条件
        TableInfo firstTable = tableInfos.get(0);
        String tableAlias = firstTable.getAliasOrTableName();
        
        // 构建数据权限条件
        String dataScopeCondition = buildDataScopeCondition(tableAlias, fieldConfig);
        
        // 将条件放在 SQL 最后面
        return appendConditionToEnd(sql, dataScopeCondition);
    }

    /**
     * 构建部门数据权限条件
     */
    private String buildDataScopeCondition(String tableAlias, DataScopeFieldConfig fieldConfig) {
        List<String> deptIds = DataScopeContext.getDeptIds();
        String deptIdField = fieldConfig.getDeptIdField();
        String deptIdList = String.join("','", deptIds);
        return tableAlias + "." + deptIdField + " IN ('" + deptIdList + "')";
    }

    /**
     * 将数据权限条件追加到 SQL 末尾
     * <p>
     * 如果 SQL 已有 WHERE 子句，在其条件后添加 AND 条件<br>
     * 如果 SQL 没有 WHERE 子句，在 ORDER BY/GROUP BY/LIMIT 等之前添加 WHERE 条件
     * </p>
     */
    private String appendConditionToEnd(String originalSql, String dataScopeCondition) {
        String lowerSql = originalSql.toLowerCase();
        
        // 查找 ORDER BY、GROUP BY、HAVING、LIMIT、UNION 等关键字的位置
        int orderByPos = lowerSql.indexOf(" order by ");
        int groupByPos = lowerSql.indexOf(" group by ");
        int havingPos = lowerSql.indexOf(" having ");
        int limitPos = lowerSql.indexOf(" limit ");
        int unionPos = lowerSql.indexOf(" union ");
        
        // 找到第一个出现的位置（最靠前的关键字）
        int firstKeywordPos = lowerSql.length();
        if (orderByPos > 0 && orderByPos < firstKeywordPos) firstKeywordPos = orderByPos;
        if (groupByPos > 0 && groupByPos < firstKeywordPos) firstKeywordPos = groupByPos;
        if (havingPos > 0 && havingPos < firstKeywordPos) firstKeywordPos = havingPos;
        if (limitPos > 0 && limitPos < firstKeywordPos) firstKeywordPos = limitPos;
        if (unionPos > 0 && unionPos < firstKeywordPos) firstKeywordPos = unionPos;
        
        // 在这些关键字之前查找 WHERE
        String beforeKeyword = lowerSql.substring(0, firstKeywordPos);
        int wherePos = beforeKeyword.lastIndexOf(" where ");
        
        if (wherePos == -1) {
            // 没有 WHERE，在第一个关键字之前插入 WHERE
            String beforePart = originalSql.substring(0, firstKeywordPos);
            String afterPart = originalSql.substring(firstKeywordPos);
            return beforePart + " WHERE " + dataScopeCondition + " " + afterPart;
        } else {
            // 已有 WHERE，在 WHERE 条件后添加 AND
            int afterWherePos = wherePos + 7; // " where " 长度为 7
            return originalSql.substring(0, afterWherePos) + "(" + dataScopeCondition + ") AND " + originalSql.substring(afterWherePos);
        }
    }
}