package cn.structured.datascope.provider;

import cn.structured.datascope.DataScopeInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认数据权限提供器
 * <p>
 * 当未配置远程权限服务时使用的默认实现，
 * 子类可继承此类并重写 {@link #doGetScopeInfo(String)} 方法实现自定义逻辑
 * </p>
 */
@Slf4j
public class DefaultDataScopeProviderImpl extends DefaultDataScopeProvider {

    @Override
    protected DataScopeInfo doGetScopeInfo(String userId) {
        log.warn("DefaultDataScopeProviderImpl is in use. Please override doGetScopeInfo() to provide actual data scope info for user: {}", userId);
        DataScopeInfo scopeInfo = new DataScopeInfo();
        scopeInfo.setUserId(userId);
        return scopeInfo;
    }
}
