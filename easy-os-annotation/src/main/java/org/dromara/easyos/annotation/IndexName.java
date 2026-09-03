package org.dromara.easyos.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IndexName {
    String value();

    String alias() default "";
}
