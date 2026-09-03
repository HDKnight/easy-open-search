package org.dromara.easyos.annotation;

import org.dromara.easyos.annotation.rely.IdType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IndexId {
    /**
     * 字段在索引中的名称；空则用字段名（可配合驼峰转下划线）。
     */
    String value() default "";

    /**
     * 主键策略；默认 {@link IdType#NONE} 表示跟随全局 {@code db-config.id-type}。
     */
    IdType type() default IdType.NONE;

    /**
     * 是否将主键写入 _source。
     */
    boolean writeToSource() default true;
}
