package cn.structured.datascope.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeField {
    
    String name() default "";
    
    boolean visible() default true;
    
    String[] visibleIfRoleIn() default {};
    
    String[] hiddenIfRoleIn() default {};
}