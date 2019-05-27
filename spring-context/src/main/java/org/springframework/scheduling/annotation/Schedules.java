package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 聚合了多个{@link Scheduled}注解的容器注解.
 *
 * <p>可以原生使用, 声明几个嵌套的{@link Scheduled}注解.
 * 也可以与Java 8对可重复注解的支持结合使用, 其中{@link Scheduled}可以简单地在同一方法上多次声明, 隐式生成此容器注解.
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Schedules {

	Scheduled[] value();

}
