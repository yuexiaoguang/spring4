package org.springframework.web.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code @ApplicationScope}是{@link Scope @Scope}的细化, 用于生命周期绑定到当前Web应用程序的组件.
 *
 * <p>具体来说, {@code @ApplicationScope}是一个<em>组合注解</em>, 它充当{@code @Scope("application")}的快捷方式,
 * {@link #proxyMode}默认设置为{@link ScopedProxyMode#TARGET_CLASS TARGET_CLASS}.
 *
 * <p>{@code @ApplicationScope}可用作元注解来创建自定义组合注解.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(WebApplicationContext.SCOPE_APPLICATION)
public @interface ApplicationScope {

	/**
	 * Alias for {@link Scope#proxyMode}.
	 * <p>默认{@link ScopedProxyMode#TARGET_CLASS}.
	 */
	@AliasFor(annotation = Scope.class)
	ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
