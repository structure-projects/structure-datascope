package cn.structured.datascope.mybatis.plus.handler;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.mybatis.properties.DataScopeMybatisProperties;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

@Slf4j
@AllArgsConstructor
public class StructureTenantLineHandler implements TenantLineHandler {

    private final DataScopeMybatisProperties dataScopeMybatisProperties;

    @Override
    public Expression getTenantId() {
        // 默认租户为1
        try {
            // 次处租户使用自己的上下文主要是要兼容租户识别的方式
            String tenantId = DataScopeContext.getOrgId();
            return new LongValue(tenantId);
        } catch (Exception e) {
            return new LongValue(dataScopeMybatisProperties.getDefaultTenantId());
        }
    }

    @Override
    public String getTenantIdColumn() {
        return dataScopeMybatisProperties.getTenantIdColumn();
    }

    // 这是 default 方法,默认返回 false 表示所有表都需要拼多租户条件
    @Override
    public boolean ignoreTable(String tableName) {
        // 忽略表
        String excludeTableName = dataScopeMybatisProperties.getExcludeTables().stream().filter(tableName::equals).findFirst().orElse(null);
        // 忽略表
        if (null != excludeTableName) {
            return true;
        }

        // 忽略字段
        TableInfo tableInfo = TableInfoHelper.getTableInfos().stream().filter(table -> table.getTableName().equals(tableName)).findFirst().orElse(null);
        if (null == tableInfo) {
            return true;
        }
        // 忽略字段
        TableFieldInfo tableField = tableInfo.getFieldList().stream().filter(tableFieldInfo -> tableFieldInfo.getColumn().equals(dataScopeMybatisProperties.getTenantIdColumn())).findFirst().orElse(null);
        return (null == tableField);
    }
}
