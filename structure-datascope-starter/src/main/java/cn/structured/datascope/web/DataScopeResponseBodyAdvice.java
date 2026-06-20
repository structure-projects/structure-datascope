package cn.structured.datascope.web;

import cn.structured.datascope.annotation.DataScopeRule;
import cn.structured.datascope.cache.DataScopeClassCache;
import cn.structured.datascope.cache.DataScopeResourceMeta;
import cn.structured.datascope.config.DataScopeProperties;
import cn.structured.datascope.engine.DataRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Collection;

/**
 * 数据权限响应体处理器
 * <p>
 * 在 Controller 返回响应之前，自动根据用户权限过滤敏感字段。
 * 对使用者透明，无需手动调用 filter() 方法。
 * </p>
 * <p>
 * 使用方式：
 * <ul>
 *   <li>在 DTO 类上添加 @DataScopeRule 注解标记资源类型</li>
 *   <li>字段可见性通过 DataScopeInfo.hiddenFields 配置</li>
 *   <li>Controller 返回对象时，框架自动进行字段过滤</li>
 * </ul>
 * </p>
 * <p>
 * 注意：
 * <ul>
 *   <li>只处理带有 @DataScopeRule 注解的对象（通过共享缓存判断）</li>
 *   <li>使用共享缓存避免运行时反射检查注解</li>
 *   <li>实际的字段过滤使用反射设置 null，由 Jackson 序列化时处理</li>
 *   <li>支持配置响应包装类，会对其内层 data 字段进行过滤（通过 structure.data-scope.result-wrapper-class 配置）</li>
 * </ul>
 * </p>
 */
@Slf4j
@ControllerAdvice
public class DataScopeResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final DataRuleEngine ruleEngine;
    private final DataScopeProperties properties;

    public DataScopeResponseBodyAdvice(DataRuleEngine ruleEngine, DataScopeProperties properties) {
        this.ruleEngine = ruleEngine;
        this.properties = properties;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查返回类型是否需要处理
        Class<?> bodyType = returnType.getParameterType();

        // 只处理已缓存的类（带有 @DataScopeRule 注解的类）
        if (bodyType.isAnnotationPresent(DataScopeRule.class)) {
            return true;
        }

        // 检查是否是集合类型
        if (Collection.class.isAssignableFrom(bodyType)) {
            return true;
        }

        // 如果配置了包装类，检查是否是该包装类型
        String wrapperClass = properties.getResultWrapperClass();
        if (wrapperClass != null && !wrapperClass.isEmpty() && wrapperClass.equals(bodyType.getName())) {
            return true;
        }

        // 如果类已在缓存中，也需要处理
        String className = bodyType.getName();
        if (DataScopeClassCache.contains(className)) {
            return true;
        }

        return false;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> converterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        // 如果配置了包装类，处理包装类型
        String wrapperClass = properties.getResultWrapperClass();
        if (wrapperClass != null && !wrapperClass.isEmpty() && wrapperClass.equals(body.getClass().getName())) {
            processResultWrapper(body);
            return body;
        }

        // 如果是集合类型，遍历处理每个元素
        if (body instanceof Collection) {
            log.debug("Processing collection of items for resource: {}");
            Collection<?> collection = (Collection<?>) body;
            for (Object item : collection) {
                filterIfAnnotated(item);
            }
        } else {
            // 单个对象处理
            filterIfAnnotated(body);
        }

        return body;
    }

    /**
     * 处理响应包装类型，提取 data 字段并对其内层数据进行过滤
     */
    private void processResultWrapper(Object wrapper) {
        String dataMethod = properties.getResultWrapperDataMethod();
        if (dataMethod == null || dataMethod.isEmpty()) {
            dataMethod = "getData";
        }

        try {
            Object data = wrapper.getClass().getMethod(dataMethod).invoke(wrapper);
            if (data == null) {
                return;
            }

            if (data instanceof Collection) {
                Collection<?> collection = (Collection<?>) data;
                for (Object item : collection) {
                    filterIfAnnotated(item);
                }
            } else {
                filterIfAnnotated(data);
            }
        } catch (Exception e) {
            log.warn("Failed to process result wrapper for data scope filtering", e);
        }
    }

    /**
     * 如果对象有 @DataScopeRule 注解，则进行过滤（通过缓存判断）
     */
    private void filterIfAnnotated(Object item) {
        if (item == null) {
            return;
        }

        String className = item.getClass().getName();

        // 通过缓存检查是否需要处理（避免运行时反射检查注解）
        DataScopeResourceMeta meta = DataScopeClassCache.get(className);
        if (meta == null) {
            // 没有缓存，不处理
            return;
        }

        try {
            // 使用规则引擎过滤对象中的敏感字段
            ruleEngine.filter(item, meta.getResource());
            log.debug("Filtered data scope fields for resource: {}", meta.getResource());
        } catch (Exception e) {
            log.warn("Failed to filter data scope fields for resource: {}", meta.getResource(), e);
        }
    }
}
