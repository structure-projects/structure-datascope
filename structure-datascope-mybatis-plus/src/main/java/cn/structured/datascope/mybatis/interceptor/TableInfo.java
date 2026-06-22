package cn.structured.datascope.mybatis.interceptor;

import lombok.Getter;

/**
 * 表信息类
 * <p>
 * 用于存储从SQL中解析出的表名、别名和插入位置信息
 * </p>
 */
@Getter
public class TableInfo {
    
    /**
     * 表名
     */
    private final String tableName;
    
    /**
     * 表别名（可能为null）
     */
    private final String alias;
    
    /**
     * 数据权限条件插入位置
     */
    private final int insertPosition;

    public TableInfo(String tableName, String alias, int insertPosition) {
        this.tableName = tableName;
        this.alias = alias;
        this.insertPosition = insertPosition;
    }

    public String getAliasOrTableName() {
        return alias != null ? alias : tableName;
    }

    @Override
    public String toString() {
        return "TableInfo{" +
                "tableName='" + tableName + '\'' +
                ", alias='" + alias + '\'' +
                ", insertPosition=" + insertPosition +
                '}';
    }
}