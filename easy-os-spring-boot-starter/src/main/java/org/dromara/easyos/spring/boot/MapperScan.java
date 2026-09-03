package org.dromara.easyos.spring.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MapperScannerRegistrar.class)
public @interface MapperScan {

    /**
     * Alias for {@link #basePackages()}.
     */
    String[] value() default {};

    /**
     * Base packages to scan for interfaces that extend BaseMapper.
     */
    String[] basePackages() default {};
}
