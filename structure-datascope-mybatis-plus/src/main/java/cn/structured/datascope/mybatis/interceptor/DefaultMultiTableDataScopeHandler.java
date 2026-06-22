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
     * 如果 SQL 没有 WHERE 子句，在最后添加 WHERE 条件
     * </p>
     */
    private String appendConditionToEnd(String originalSql, String dataScopeCondition) {
        String lowerSql = originalSql.toLowerCase();
        
        // 查找 SQL 末尾的 WHERE 子句位置（忽略子查询中的 WHERE）
        int lastWhereIndex = findLastWhereBeforeOrderBy(lowerSql);
        
        if (lastWhereIndex == -1) {
            // 没有 WHERE，在 SQL 最后添加 WHERE
            return originalSql + " WHERE " + dataScopeCondition;
        } else {
            // 已有 WHERE，在 WHERE 条件后添加 AND
            int afterWherePos = lastWhereIndex + 7; // " where " 长度为 7
            return originalSql.substring(0, afterWherePos) + "(" + dataScopeCondition + ") AND " + originalSql.substring(afterWherePos);
        }
    }
    
    /**
     * 查找最后一个 WHERE 子句的位置（在 ORDER BY、LIMIT 等之前）
     */
    private int findLastWhereBeforeOrderBy(String lowerSql) {
        // 查找所有可能的关键字位置
        int[] keywordPositions = {
            findKeywordEnd(lowerSql, "order by"),
            findKeywordEnd(lowerSql, "group by"),
            findKeywordEnd(lowerSql, "having "),
            findKeywordEnd(lowerSql, "limit "),
            findKeywordEnd(lowerSql, "union ")
        };
        
        // 找到最靠前的关键字位置
        int minKeywordPos = lowerSql.length();
        for (int pos : keywordPositions) {
            if (pos > 0 && pos < minKeywordPos) {
                minKeywordPos = pos;
            }
        }
        
        // 在 ORDER BY 等关键字之前查找最后一个 WHERE
        String beforeKeyword = lowerSql.substring(0, minKeywordPos);
        int lastWhereIndex = beforeKeyword.lastIndexOf(" where ");
        
        return lastWhereIndex;
    }
    
    /**
     * 查找关键字的结束位置
     */
    private int findKeywordEnd(String lowerSql, String keyword) {
        int index = lowerSql.indexOf(keyword);
        return index > 0 ? index + keyword.length() : -1;
    }
}