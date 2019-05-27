package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将类标记为Spring驱动配置.
 *
 * <p>通常与AspectJ {@code AnnotationBeanConfigurerAspect}一起使用.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configurable {

	/**
	 * 用作配置模板的bean定义的名称.
	 */
	String value() default "";

	/**
	 * 是否通过自动装配注入依赖关系?
	 */
	Autowire autowire() default Autowire.NO;

	/**
	 * 是否对配置的对象执行依赖性检查?
	 */
	boolean dependencyCheck() default false;

	/**
	 * 是否在构造对象之前注入依赖项?
	 */
	boolean preConstruction() default false;

}
