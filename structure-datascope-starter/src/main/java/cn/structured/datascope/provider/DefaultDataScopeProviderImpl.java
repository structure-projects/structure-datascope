package cn.structured.datascope.provider;

import cn.structured.datascope.DataScopeInfo;
import cn.structured.security.context.UserContext;
import cn.structured.security.entity.UserContextEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Set;

/**
 * 默认数据权限提供器
 * <p>
 * 当未配置远程权限服务时使用的默认实现，
 * 子类可继承此类并重写 {@link #doGetScopeInfo(String)} 方法实现自定义逻辑
 * </p>
 * <p>
 * 支持测试模式，通过设置系统属性或环境变量 {@code data.scope.test.mode=true} 启用
 * </p>
 */
@Slf4j
public class DefaultDataScopeProviderImpl extends DefaultDataScopeProvider {

    @Override
    protected DataScopeInfo doGetScopeInfo(String userId) {

        UserContextEntity userContextEntity = UserContext.get();

        if (null != userContextEntity) {
            log.debug("使用用户上下文获取数据权限信息");
            DataScopeInfo scopeInfo = new DataScopeInfo();
            Set<String> roles = userContextEntity.getRoles();
            scopeInfo.setRoles(roles == null ? new ArrayList<>() : new ArrayList<>(roles));
            Set<String> permissions = userContextEntity.getPermissions();
            scopeInfo.setPermissions(permissions == null ? new ArrayList<>() : new ArrayList<>(permissions));
            //String deptId = userContextEntity.getDeptId();
            String tenantId = userContextEntity.getTenantId();
            scopeInfo.setOrgId(tenantId);
            Set<String> deptIds = userContextEntity.getDeptIds();
            scopeInfo.setDeptIds(deptIds == null ? new ArrayList<>() : new ArrayList<>(deptIds));
            scopeInfo.setUserId(userId);
            return scopeInfo;
        }
        return null;
    }
}
