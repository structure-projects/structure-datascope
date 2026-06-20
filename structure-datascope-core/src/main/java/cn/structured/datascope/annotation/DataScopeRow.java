package cn.structured.datascope.annotation;

import java.lang.annotation.*;

/**
 * 行级数据权限规则注解
 * <p>
 * 用于标记需要进行行级数据权限过滤的字段，配合 @DataScopeRule 使用
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * &#64;DataScopeRule(resource = "order")
 * &#64;DataScopeRow(fields = {"org_id", "dept_id"}, ops = {"=", "IN"})
 * public class Order {
 *     // ...
 * }
 * </pre>
 * </p>
 * <p>
 * 字段与操作的默认映射：
 * <ul>
 *   <li>org_id - 使用 "=" 操作符，值来自 DataScopeContext.getOrgId()</li>
 *   <li>dept_id - 使用 "IN" 操作符，值来自 DataScopeContext.getDeptIds()</li>
 *   <li>user_id - 使用 "=" 操作符，值来自 DataScopeContext.getUserId()</li>
 * </ul>
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeRow {

    /**
     * 需要进行行级过滤的字段名数组
     */
    String[] fields() default {};

    /**
     * 对应字段的操作符数组
     * <p>
     * 支持的操作符：
     * <ul>
     *   <li>"=" - 等于</li>
     *   <li>"!=" - 不等于</li>
     *   <li>"IN" - 在列表中</li>
     *   <li>"NOT_IN" - 不在列表中</li>
     *   <li>"LIKE" - 模糊匹配</li>
     * </ul>
     * </p>
     * <p>
     * 如果不指定，将根据字段名自动推断：
     * <ul>
     *   <li>org_id, user_id 等使用 "="</li>
     *   <li>dept_id 等使用 "IN"</li>
     * </ul>
     * </p>
     */
    String[] ops() default {};

    /**
     * 资源名称，默认为空，使用 @DataScopeRule 中的 resource
     */
    String resource() default "";
}
