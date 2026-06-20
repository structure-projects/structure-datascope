package cn.structured.datascope.serialize;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.cache.DataScopeClassCache;
import cn.structured.datascope.cache.DataScopeResourceMeta;
import com.alibaba.fastjson.serializer.BeforeFilter;
import com.alibaba.fastjson.serializer.PropertyFilter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 数据权限序列化过滤器
 * <p>
 * 在 JSON 序列化时，根据用户权限过滤敏感字段。
 * </p>
 * <p>
 * 使用方式：
 * <ul>
 *   <li>在 DTO 类上添加 @DataScopeRule 注解标记资源类型</li>
 *   <li>字段可见性通过 DataScopeInfo.hiddenFields 配置</li>
 *   <li>通过 FastJsonHttpMessageConverter 配置此过滤器自动生效</li>
 * </ul>
 * </p>
 * <p>
 * 注意：
 * <ul>
 *   <li>只处理已缓存的类（带有 @DataScopeRule 注解的类）</li>
 *   <li>使用共享缓存避免运行时反射检查注解</li>
 *   <li>支持 ResResultVO 包装类型，会对其内层 data 字段进行过滤</li>
 * </ul>
 * </p>
 */
public class DataScopeSerializeFilter extends BeforeFilter implements PropertyFilter {

    @Override
    public boolean apply(Object object, String name, Object value) {
        if (object == null) {
            return true;
        }

        Class<?> clazz = object.getClass();
        String className = clazz.getName();

        // 检查是否是缓存的类
        DataScopeResourceMeta meta = DataScopeClassCache.get(className);
        if (meta == null) {
            // 没有缓存，不做过滤
            return true;
        }

        // 获取当前用户的隐藏字段配置
        Set<String> hiddenFields = getHiddenFields(meta);
        if (hiddenFields == null || hiddenFields.isEmpty()) {
            return true;
        }

        // 如果当前字段在隐藏列表中，返回 false（不序列化）
        return !hiddenFields.contains(name);
    }

    @Override
    public void writeBefore(Object object) {
        if (object == null) {
            return;
        }

        Class<?> clazz = object.getClass();
        String className = clazz.getName();


        // 直接处理缓存的类
        DataScopeResourceMeta meta = DataScopeClassCache.get(className);
        if (meta != null) {
            filterObject(object, meta);
        }
    }

    /**
     * 处理 ResResultVO，提取 data 字段并对其内层数据进行处理
     */
    private void processResResultVO(Object resResultVO) {
        try {
            // 尝试通过 getData() 方法获取
            Object data = resResultVO.getClass().getMethod("getData").invoke(resResultVO);
            if (data == null) {
                return;
            }

            if (data instanceof List) {
                List<?> list = (List<?>) data;
                for (Object item : list) {
                    if (item != null) {
                        DataScopeResourceMeta meta = DataScopeClassCache.get(item.getClass().getName());
                        if (meta != null) {
                            filterObject(item, meta);
                        }
                    }
                }
            } else {
                // 单个对象
                DataScopeResourceMeta meta = DataScopeClassCache.get(data.getClass().getName());
                if (meta != null) {
                    filterObject(data, meta);
                }
            }
        } catch (Exception e) {
            // 获取 data 失败，忽略
        }
    }

    /**
     * 过滤对象中的敏感字段
     */
    private void filterObject(Object obj, DataScopeResourceMeta meta) {
        if (obj == null) {
            return;
        }

        Set<String> hiddenFields = getHiddenFields(meta);
        if (hiddenFields == null || hiddenFields.isEmpty()) {
            return;
        }

        Class<?> clazz = obj.getClass();
        for (String fieldName : hiddenFields) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, null);
            } catch (NoSuchFieldException e) {
                // 字段不存在，跳过
            } catch (IllegalAccessException e) {
                // 无法访问，跳过
            }
        }
    }

    /**
     * 获取需要隐藏的字段集合
     */
    private Set<String> getHiddenFields(DataScopeResourceMeta meta) {
        Set<String> hiddenFields = new HashSet<>();

        // 1. 获取缓存中定义的隐藏字段
        List<String> cachedHiddenFields = meta.getHiddenFields();
        if (cachedHiddenFields != null) {
            hiddenFields.addAll(cachedHiddenFields);
        }

        // 2. 获取当前用户上下文中配置的隐藏字段
        DataScopeInfo scopeInfo = DataScopeContext.getInfo();
        if (scopeInfo != null) {
            Map<String, List<String>> scopeHiddenFields = scopeInfo.getHiddenFields();
            if (scopeHiddenFields != null && !scopeHiddenFields.isEmpty()) {
                List<String> resourceHiddenFields = scopeHiddenFields.get(meta.getResource());
                if (resourceHiddenFields != null) {
                    hiddenFields.addAll(resourceHiddenFields);
                }
            }
        }

        return hiddenFields.isEmpty() ? null : hiddenFields;
    }
}
