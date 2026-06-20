package cn.structured.datascope.cache;

import java.util.List;

/**
 * 数据权限资源元信息（贫血模型）
 * <p>
 * 存储资源的元信息，包括类名、资源名称、字段列表等
 * </p>
 */
public class DataScopeResourceMeta {

    /**
     * 完整类名
     */
    private String className;

    /**
     * 资源名称
     */
    private String resource;

    /**
     * 需要过滤的字段名列表
     */
    private List<String> hiddenFields;

    public DataScopeResourceMeta() {
    }

    public DataScopeResourceMeta(String className, String resource, List<String> hiddenFields) {
        this.className = className;
        this.resource = resource;
        this.hiddenFields = hiddenFields;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public List<String> getHiddenFields() {
        return hiddenFields;
    }

    public void setHiddenFields(List<String> hiddenFields) {
        this.hiddenFields = hiddenFields;
    }

    /**
     * 检查字段是否需要隐藏
     *
     * @param fieldName 字段名
     * @return true 如果字段需要隐藏
     */
    public boolean isFieldHidden(String fieldName) {
        return hiddenFields != null && hiddenFields.contains(fieldName);
    }
}
