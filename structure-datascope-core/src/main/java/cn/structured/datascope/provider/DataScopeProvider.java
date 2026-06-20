package cn.structured.datascope.provider;

import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.rule.DataRule;

import java.util.List;
import java.util.Map;

/**
 * 数据权限提供器接口
 * <p>
 * 支持两种实现模式：
 * <ul>
 *     <li>远程模式：调用数据权限服务获取权限信息</li>
 *     <li>本地模式：用户自行实现，从本地数据源（数据库/缓存）获取</li>
 * </ul>
 * </p>
 */
public interface DataScopeProvider {

    /**
     * 获取当前用户的数据范围上下文信息
     *
     * @param userId 用户ID
     * @return DataScopeInfo 对象，包含角色、权限、组织ID、部门ID等信息
     */
    DataScopeInfo getScopeInfo(String userId);
}