package org.dromara.easyos.annotation;

import java.lang.annotation.*;

/**
 * Maps OpenSearch relevance {@code _score} onto an entity field.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Score {
}
