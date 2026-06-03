package cn.structured.datascope.config;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 数据范围自动配置类
 * <p>
 * 仅在 Web 应用环境下启用
 * </p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DataScopeProperties.class)
@ConditionalOnWebApplication
public class DataScopeAutoConfiguration {

    private final DataScopeProperties properties;

    public DataScopeAutoConfiguration(DataScopeProperties properties) {
        this.properties = properties;
        log.info("DataScopeAutoConfiguration initialized with properties: {}", properties);
    }

    /**
     * 注册数据范围过滤器
     * <p>
     * 该过滤器会自动从请求头中提取数据范围信息并设置到上下文中，
     * 请求结束后自动清除上下文避免内存泄漏
     * </p>
     */
    @Bean
    public Filter dataScopeFilter() {
        log.info("Registering DataScopeFilter...");
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                try {
                    HttpServletRequest httpRequest = (HttpServletRequest) request;

                    DataScopeInfo info = new DataScopeInfo();

                    // 从请求头中提取数据范围ID
                    String dataScopeId = httpRequest.getHeader(properties.getHeaderName());
                    if (dataScopeId != null) {
                        info.setDataScopeId(dataScopeId);
                    }

                    // 从请求头中提取角色列表
                    String rolesHeader = httpRequest.getHeader(properties.getRoleHeaderName());
                    if (rolesHeader != null && !rolesHeader.isEmpty()) {
                        List<String> roles = Arrays.asList(rolesHeader.split(","));
                        info.setRoles(roles);
                    } else {
                        info.setRoles(Collections.emptyList());
                    }

                    // 从请求头中提取组织ID
                    String orgId = httpRequest.getHeader(properties.getOrgIdHeaderName());
                    if (orgId != null) {
                        info.setOrgId(orgId);
                    }

                    // 从请求头中提取部门ID列表
                    String deptIdsHeader = httpRequest.getHeader(properties.getDeptIdsHeaderName());
                    if (deptIdsHeader != null && !deptIdsHeader.isEmpty()) {
                        List<String> deptIds = Arrays.asList(deptIdsHeader.split(","));
                        info.setDeptIds(deptIds);
                    }

                    // 从请求头中提取用户ID
                    String userId = httpRequest.getHeader(properties.getUserIdHeaderName());
                    if (userId != null) {
                        info.setUserId(userId);
                    }

                    // 设置数据范围上下文
                    DataScopeContext.set(info);

                    if (log.isDebugEnabled()) {
                        log.debug("DataScope context set from headers: dataScopeId={}, roles={}, orgId={}, userId={}",
                                info.getDataScopeId(), info.getRoles(), info.getOrgId(), info.getUserId());
                    }

                    // 继续执行请求链
                    chain.doFilter(request, response);
                } finally {
                    // 请求结束后清除上下文，避免内存泄漏
                    DataScopeContext.remove();
                    if (log.isTraceEnabled()) {
                        log.trace("DataScope context cleared after request completion");
                    }
                }
            }
        };
    }
}