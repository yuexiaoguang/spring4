package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 容器注解, 聚合了多个{@link PropertySource}注解.
 *
 * <p>可以原生使用, 声明几个嵌套的{@link PropertySource}注解.
 * 也可以与Java 8对<em>可重复注解</em>的支持结合使用,
 * 其中{@link PropertySource}可以简单地在同一{@linkplain ElementType#TYPE type}上多次声明, 隐式生成此容器注解.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PropertySources {

	PropertySource[] value();

}
