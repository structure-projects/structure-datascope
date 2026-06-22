package cn.structured.datascope.mybatis.interceptor;

import cn.structured.datascope.config.DataScopeFieldConfig;

import java.util.List;

/**
 * 多表数据权限处理器接口
 * <p>
 * 用于处理复杂的多表查询场景，允许用户自定义数据权限条件的生成逻辑
 * </p>
 */
public interface MultiTableDataScopeHandler {

    /**
     * 处理多表查询的数据权限
     *
     * @param sql         原始SQL
     * @param tableInfos  解析出的所有表信息
     * @param fieldConfig 字段配置
     * @return 处理后的SQL
     */
    String handleMultiTable(String sql, List<TableInfo> tableInfos, DataScopeFieldConfig fieldConfig);
}