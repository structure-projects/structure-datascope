package cn.structured.datascope.filter;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.config.DataScopeProperties;
import cn.structured.datascope.provider.DataScopeProvider;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 数据范围上下文过滤器
 * <p>
 * 从DataScopeProvider获取当前用户的数据权限信息，并设置到DataScopeContext。
 * 请求处理完成后自动清理上下文，防止内存泄漏。
 * </p>
 *
 * @see DataScopeContext
 * @see DataScopeInfo
 * @see DataScopeProvider
 */
@Slf4j
public class DataScopeContextFilter implements Filter {

    private final DataScopeProperties properties;
    private final DataScopeProvider dataScopeProvider;

    public DataScopeContextFilter(DataScopeProperties properties, DataScopeProvider dataScopeProvider) {
        this.properties = properties;
        this.dataScopeProvider = dataScopeProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 从DataScopeProvider获取数据权限信息
            DataScopeInfo scopeInfo = extractScopeInfo(httpRequest);

            if (log.isDebugEnabled()) {
                log.debug("Got DataScope info from provider: {}", scopeInfo);
            }

            // 设置到上下文
            DataScopeContext.setInfo(scopeInfo);

            // 继续处理请求
            chain.doFilter(request, response);

        } finally {
            // 请求处理完成后清理上下文
            DataScopeContext.remove();
            if (log.isDebugEnabled()) {
                log.debug("DataScope context cleared after request processing");
            }
        }
    }

    /**
     * 从DataScopeProvider获取数据权限信息
     */
    private DataScopeInfo extractScopeInfo(HttpServletRequest request) {
        // 从DataScopeProvider获取
        if (dataScopeProvider != null) {
            String userId = request.getHeader(properties.getUserId());
            if (userId != null && !userId.isEmpty()) {
                DataScopeInfo providerInfo = dataScopeProvider.getScopeInfo(userId);
                if (providerInfo != null) {
                    log.debug("Got DataScope info from provider for user: {}", userId);
                    return providerInfo;
                }
            }
        }

        log.debug("No DataScope info available, returning empty info");
        return new DataScopeInfo();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("DataScopeContextFilter initialized with properties: {}, provider: {}",
                properties, dataScopeProvider != null ? dataScopeProvider.getClass() : "null");
    }

    @Override
    public void destroy() {
        log.info("DataScopeContextFilter destroyed");
    }
}
