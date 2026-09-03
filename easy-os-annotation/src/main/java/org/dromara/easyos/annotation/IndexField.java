package org.dromara.easyos.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IndexField {
    String value() default "";

    /**
     * OpenSearch mapping type, e.g. text, keyword, integer, long, double, boolean, date.
     */
    String type() default "";

    boolean index() default true;
}
