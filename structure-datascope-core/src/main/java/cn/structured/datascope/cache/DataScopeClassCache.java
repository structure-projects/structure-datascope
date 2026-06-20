package cn.structured.datascope.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限类缓存
 * <p>
 * 在类扫描时缓存带有 @DataScopeRule 注解的类信息，避免运行时反射
 * </p>
 */
public class DataScopeClassCache {

    /**
     * 类级别缓存：存储类名到资源元信息的映射
     */
    private static final Map<String, DataScopeResourceMeta> CLASS_META_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取资源元信息
     *
     * @param className 完整类名
     * @return 资源元信息，如果未找到返回 null
     */
    public static DataScopeResourceMeta get(String className) {
        return CLASS_META_CACHE.get(className);
    }

    /**
     * 检查类是否已缓存
     *
     * @param className 完整类名
     * @return true 如果已缓存
     */
    public static boolean contains(String className) {
        return CLASS_META_CACHE.containsKey(className);
    }

    /**
     * 注册类到缓存
     *
     * @param meta 资源元信息
     */
    public static void register(DataScopeResourceMeta meta) {
        if (meta != null && meta.getClassName() != null) {
            CLASS_META_CACHE.put(meta.getClassName(), meta);
        }
    }

    /**
     * 注册类到缓存（简便方法）
     *
     * @param className 完整类名
     * @param resource 资源名称
     * @param hiddenFields 隐藏字段列表
     */
    public static void register(String className, String resource, java.util.List<String> hiddenFields) {
        register(new DataScopeResourceMeta(className, resource, hiddenFields));
    }

    /**
     * 清除所有缓存
     */
    public static void clear() {
        CLASS_META_CACHE.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存的类数量
     */
    public static int size() {
        return CLASS_META_CACHE.size();
    }
}
