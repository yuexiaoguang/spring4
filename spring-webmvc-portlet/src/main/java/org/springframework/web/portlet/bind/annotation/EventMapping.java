package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.Mapping;

/**
 * 用于将Portlet事件请求映射到处理器方法的注解.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping()
public @interface EventMapping {

	/**
	 * 要处理的事件的名称.
	 * 此名称唯一标识portlet模式中的事件.
	 * <p>通常, 事件的本地名称, 但具有"{...}"命名空间部分的完全限定名称也将正确映射.
	 * <p>如果未指定, 将在其常规映射中为任何事件请求调用处理器方法.
	 */
	String value() default "";

}
