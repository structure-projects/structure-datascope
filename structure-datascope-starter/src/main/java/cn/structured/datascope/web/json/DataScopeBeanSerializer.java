//package cn.structured.datascope.web.json;
//
//import cn.structured.datascope.DataScopeContext;
//import cn.structured.datascope.DataScopeInfo;
//import cn.structured.datascope.cache.DataScopeClassCache;
//import cn.structured.datascope.cache.DataScopeResourceMeta;
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import lombok.extern.slf4j.Slf4j;
//
//import java.io.IOException;
//import java.lang.reflect.Field;
//import java.util.Map;
//
///**
// * 数据权限 JSON 序列化器
// * <p>
// * 在 JSON 序列化时，根据 DataScopeInfo.hiddenFields 配置决定哪些字段需要隐藏
// * </p>
// * <p>
// * 使用方式：
// * <ul>
// *   <li>在 DTO 类上添加 @JsonSerializer(clazz = DataScopeBeanSerializer.class)</li>
// *   <li>或通过 Jackson ObjectMapper 注册全局 serializer</li>
// * </ul>
// * </p>
// * <p>
// * 注意：此序列化器通过检查 hiddenFields 配置来决定字段是否输出，
// * 相比反射设置 null 的方式，更符合 JSON 序列化规范
// * </p>
// */
//@Slf4j
//public class DataScopeBeanSerializer extends JsonSerializer<Object> {
//
//    @Override
//    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//        if (value == null) {
//            gen.writeNull();
//            return;
//        }
//
//        Class<?> clazz = value.getClass();
//        String className = clazz.getName();
//
//        // 获取缓存的元信息
//        DataScopeResourceMeta meta = DataScopeClassCache.get(className);
//        if (meta == null) {
//            // 没有缓存信息，使用默认序列化
//            writeObjectFields(value, gen, serializers, null, clazz);
//            return;
//        }
//
//        // 获取当前用户的隐藏字段配置
//        DataScopeInfo scopeInfo = DataScopeContext.getInfo();
//        java.util.List<String> hiddenFields = getHiddenFields(meta, scopeInfo);
//
//        // 使用元信息和隐藏字段配置进行序列化
//        writeObjectFields(value, gen, serializers, hiddenFields, clazz);
//    }
//
//    /**
//     * 获取需要隐藏的字段列表
//     */
//    private java.util.List<String> getHiddenFields(DataScopeResourceMeta meta, DataScopeInfo scopeInfo) {
//        // 如果没有 scopeInfo，使用缓存中定义的隐藏字段
//        if (scopeInfo == null || scopeInfo.getHiddenFields() == null || scopeInfo.getHiddenFields().isEmpty()) {
//            return meta.getHiddenFields();
//        }
//
//        // 从 scopeInfo 获取资源对应的隐藏字段
//        String resource = meta.getResource();
//        java.util.List<String> scopeHiddenFields = scopeInfo.getHiddenFields().get(resource);
//
//        // 如果 scopeInfo 中没有定义，使用缓存中的定义
//        if (scopeHiddenFields == null || scopeHiddenFields.isEmpty()) {
//            return meta.getHiddenFields();
//        }
//
//        // 合并缓存和 scopeInfo 中的隐藏字段
//        java.util.Set<String> merged = new java.util.HashSet<>();
//        if (meta.getHiddenFields() != null) {
//            merged.addAll(meta.getHiddenFields());
//        }
//        merged.addAll(scopeHiddenFields);
//
//        return new java.util.ArrayList<>(merged);
//    }
//
//    /**
//     * 写入对象的字段
//     */
//    private void writeObjectFields(Object value, JsonGenerator gen, SerializerProvider serializers,
//                                   java.util.List<String> hiddenFields, Class<?> clazz) throws IOException {
//        gen.writeStartObject();
//
//        Field[] fields = clazz.getDeclaredFields();
//        for (Field field : fields) {
//            String fieldName = field.getName();
//
//            // 检查字段是否需要隐藏
//            if (hiddenFields != null && hiddenFields.contains(fieldName)) {
//                continue; // 跳过隐藏的字段
//            }
//
//            try {
//                field.setAccessible(true);
//                Object fieldValue = field.get(value);
//
//                // 序列化字段
//                gen.writeFieldName(fieldName);
//                serializers.defaultSerializeValue(fieldValue, gen);
//            } catch (IllegalAccessException e) {
//                log.debug("Cannot access field: {}", fieldName);
//            }
//        }
//
//        gen.writeEndObject();
//    }
//}
