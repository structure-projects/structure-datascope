package cn.structured.datascope.mybatis.interceptor;

import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.DataScopeContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 默认多表数据权限处理器
 * <p>
 * 策略：只对第一个表添加数据权限条件
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
        
        // 注入条件
        return injectDataScopeCondition(sql, dataScopeCondition, firstTable.getInsertPosition());
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
     * 将数据权限条件注入到SQL中
     */
    private String injectDataScopeCondition(String originalSql, String dataScopeCondition, int afterTableIndex) {
        String lowerSql = originalSql.toLowerCase();
        int whereIndex = lowerSql.indexOf(" where ", afterTableIndex);

        if (whereIndex == -1) {
            return originalSql.substring(0, afterTableIndex) + " WHERE " + dataScopeCondition + originalSql.substring(afterTableIndex);
        } else {
            int afterWherePos = whereIndex + 7;
            return originalSql.substring(0, afterWherePos) + "(" + dataScopeCondition + ") AND " + originalSql.substring(afterWherePos);
        }
    }
}