package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AliasFor;

/**
 * 与{@link org.springframework.stereotype.Component @Component}一起用作类型级别注解时,
 * {@code @Scope}表示带注解的类型实例的作用域的名称.
 *
 * <p>与{@link Bean @Bean}一起用作方法级注解时,
 * {@code @Scope} 表示要从方法返回的实例使用的作用域的名称.
 *
 * <p><b>NOTE:</b> {@code @Scope} 注解仅在具体bean类 (用于带注解的组件) 或工厂方法 (用于{@code @Bean}方法)上进行内省.
 * 与XML bean定义相比, 没有bean定义继承的概念, 类级别的继承层次结构与元数据目的无关.
 *
 * <p>在此上下文中, <em>scope</em>表示实例的生命周期, 例如{@code singleton}, {@code prototype}, 等等.
 * 可以使用{@link ConfigurableBeanFactory}和{@code WebApplicationContext}接口中提供的 {@code SCOPE_*}常量,
 * 来引用Spring中提供的开箱即用的作用域.
 *
 * <p>要注册其他自定义作用域, 请参阅{@link org.springframework.beans.factory.config.CustomScopeConfigurer CustomScopeConfigurer}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

	/**
	 * Alias for {@link #scopeName}.
	 */
	@AliasFor("scopeName")
	String value() default "";

	/**
	 * 指定要用于带注解的组件/bean的作用域的名称.
	 * <p>默认为空字符串 ({@code ""}), 暗示{@link ConfigurableBeanFactory#SCOPE_SINGLETON SCOPE_SINGLETON}.
	 */
	@AliasFor("value")
	String scopeName() default "";

	/**
	 * 是否应将组件配置为作用域代理, 如果是, 则指定代理应基于接口还是基于子类.
	 * <p>默认 {@link ScopedProxyMode#DEFAULT}, 通常表示不应创建作用域代理, 除非在组件扫描指令级别配置了不同的默认值.
	 * <p>类似于Spring XML中的 {@code <aop:scoped-proxy/>}支持.
	 */
	ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

}
