package org.dromara.easyos.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义查询 SQL（OpenSearch SQL），支持 {@code #{param}} 占位符。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OsSelect {
    /**
     * OpenSearch SQL 文本，例如 {@code SELECT * FROM article WHERE status = #{status}}。
     */
    String value();
}
