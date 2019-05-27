package org.springframework.context.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.annotation.AliasFor;

/**
 * 将方法标记为应用程序事件的监听器的注解.
 *
 * <p>如果被注解的方法支持单个事件类型, 则该方法可以声明反映要监听的事件类型的单个参数.
 * 如果被注解的方法支持多种事件类型, 则此批注可以使用{@code classes}属性引用一个或多个受支持的事件类型.
 * 有关更多详细信息, 请参阅{@link #classes} javadoc.
 *
 * <p>事件可以是{@link ApplicationEvent}实例以及任意对象.
 *
 * <p>{@code @EventListener}注释的处理是通过内部{@link EventListenerMethodProcessor} bean执行的,
 * 该bean在使用Java配置时自动注册, 
 * 或在使用XML配置时通过 {@code <context:annotation-config/>} 或 {@code <context:component-scan/>}元素手动注册.
 *
 * <p>带注解的方法可能具有非{@ code void}返回类型.
 * 当它们执行时, 方法调用的结果将作为新事件发送.
 * 如果返回类型是数组或集合, 则每个元素都将作为新的单个事件发送.
 *
 * <p>还可以定义调用特定事件的监听器的顺序.
 * 为此, 请在此事件监听器注解旁边添加Spring的常用 {@link org.springframework.core.annotation.Order @Order}注解.
 *
 * <p>虽然事件监听器声明它可能会抛出任意异常类型,
 * 从事件监听器抛出的任何受检异常都将包装在 {@link java.lang.reflect.UndeclaredThrowableException}中,
 * 因为事件发布者只能处理运行时异常.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

	/**
	 * Alias for {@link #classes}.
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * 此监听器处理的事件类.
	 * <p>如果使用单个值指定此属性, 则被注解的方法可以选择接受单个参数.
	 * 但是如果使用多个值指定此属性, 则被注解的方法不得声明任何参数.
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

	/**
	 * Spring Expression Language (SpEL) 属性, 用于使事件处理成为条件.
	 * <p>默认 {@code ""}, 意味着始终处理事件.
	 * <p>SpEL表达式针对提供以下元数据的专用上下文进行评估:
	 * <ul>
	 * <li>{@code #root.event}, {@code #root.args} 分别引用{@link ApplicationEvent}和方法参数.</li>
	 * <li>方法参数可以通过索引访问. 例如, 第一个参数可以通过 {@code #root.args[0]}, {@code #p0} 或 {@code #a0}访问.
	 * 如果该信息可用, 也可以通过名称访问参数.</li>
	 * </ul>
	 */
	String condition() default "";

}
