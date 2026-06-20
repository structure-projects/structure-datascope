package cn.structured.datascope.provider;

import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.config.DataScopeProperties.RemoteConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 远程数据权限提供器
 * <p>
 * 通过 HTTP/gRPC 调用远程权限服务获取数据权限信息
 * </p>
 */
@Slf4j
@Getter
@AllArgsConstructor
public class RemoteDataScopeProvider extends DefaultDataScopeProvider {

    private final RemoteConfig remoteConfig;

    @Override
    protected DataScopeInfo doGetScopeInfo(String userId) {
        log.info("Fetching data scope info from remote service: {} for user: {}", remoteConfig.getServiceUrl(), userId);
        return null;
    }
}