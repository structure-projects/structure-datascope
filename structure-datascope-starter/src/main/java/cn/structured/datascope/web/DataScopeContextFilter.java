package cn.structured.datascope.web;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.provider.DataScopeProvider;
import cn.structured.security.context.UserContext;
import cn.structured.security.entity.UserContextEntity;
import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 数据范围上下文过滤器
 * <p>
 * 在 UserContextFilter 之后执行，从用户上下文中获取用户ID，
 * 通过 DataScopeProvider 获取数据权限信息并初始化 DataScopeContext。
 * </p>
 * <p>
 * 注意：此过滤器必须在 UserContextFilter 之后执行，以确保 UserContext 已初始化。
 * 通过 FilterRegistrationBean.setOrder(101) 确保在 UserContextFilter（假设为100）之后执行。
 * </p>
 */
@Slf4j
public class DataScopeContextFilter implements Filter {

    private final DataScopeProvider dataScopeProvider;

    public DataScopeContextFilter(DataScopeProvider dataScopeProvider) {
        this.dataScopeProvider = dataScopeProvider;
        log.info("DataScopeContextFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 从用户上下文中获取用户ID
            UserContextEntity userContext = UserContext.get();
            if (userContext != null && userContext.getUserId() != null) {
                String userId = userContext.getUserId();
                
                // 通过 DataScopeProvider 获取数据权限信息
                DataScopeInfo scopeInfo = dataScopeProvider.getScopeInfo(userId);
                
                if (scopeInfo != null) {
                    // 设置数据范围上下文
                    DataScopeContext.set(scopeInfo);
                    if (log.isDebugEnabled()) {
                        log.debug("DataScope context initialized for user: {}, roles: {}, deptIds: {}", 
                                userId, scopeInfo.getRoles(), scopeInfo.getDeptIds());
                    }
                } else {
                    log.debug("No data scope info found for user: {}", userId);
                }
            }
            
            // 继续过滤器链
            chain.doFilter(request, response);
        } finally {
            // 请求结束时清理上下文，避免内存泄漏
            DataScopeContext.remove();
        }
    }
}