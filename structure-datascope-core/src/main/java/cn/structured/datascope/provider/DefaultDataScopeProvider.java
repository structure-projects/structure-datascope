package cn.structured.datascope.provider;

import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.rule.DataRule;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地数据权限提供器（抽象类）
 * <p>
 * 用户需要继承此类并实现抽象方法，从本地数据源获取权限信息
 * </p>
 */
@Slf4j
public abstract class DefaultDataScopeProvider implements DataScopeProvider {

    @Override
    public DataScopeInfo getScopeInfo(String userId) {
        if (log.isDebugEnabled()) {
            log.debug("Fetching data scope info from local provider for user: {}", userId);
        }
        return doGetScopeInfo(userId);
    }


    /**
     * 子类实现：从本地数据源获取用户的数据范围信息
     *
     * @param userId 用户ID
     * @return DataScopeInfo 对象
     */
    protected abstract DataScopeInfo doGetScopeInfo(String userId);
}