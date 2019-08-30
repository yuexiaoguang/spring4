package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.Mapping;

/**
 * 用于将Portlet资源请求映射到处理器方法的注解.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping()
public @interface ResourceMapping {

	/**
	 * 要处理的资源的ID.
	 * 此id唯一标识portlet模式中的资源.
	 * <p>如果未指定, 将在其常规映射中为任何资源请求调用处理器方法.
	 */
	String value() default "";

}
