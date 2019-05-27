package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 容器注解, 聚合了多个{@link ComponentScan}注解.
 *
 * <p>可以本机使用, 声明几个嵌套的{@link ComponentScan}注解.
 * 也可以与Java 8对可重复注解的支持结合使用, 其中{@link ComponentScan}可以在同一方法上简单地多次声明, 隐式的生成此容器注解.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ComponentScans {

	ComponentScan[] value();

}
