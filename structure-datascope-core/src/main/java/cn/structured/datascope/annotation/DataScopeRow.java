package cn.structured.datascope.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeRow {
    
    String[] fields() default {};
    
    String resource() default "";
}
