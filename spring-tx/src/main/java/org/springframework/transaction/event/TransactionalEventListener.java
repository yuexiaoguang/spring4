package org.springframework.transaction.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

/**
 * 根据{@link TransactionPhase}调用的{@link EventListener}.
 *
 * <p>如果事件未在托管事务的边界内发布, 则除非明确设置{@link #fallbackExecution}标志, 否则将丢弃该事件.
 * 如果事务正在运行, 则事件将根据其{@code TransactionPhase}进行处理.
 *
 * <p>将{@link org.springframework.core.annotation.Order @Order}添加到带注解的方法,
 * 允许在事务完成之前或之后运行的其他监听器中优先处理该监听器.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EventListener
public @interface TransactionalEventListener {

	/**
	 * 用于绑定事件处理的阶段.
	 * <p>默认阶段是{@link TransactionPhase#AFTER_COMMIT}.
	 * <p>如果没有正在进行的事务, 则除非已明确启用{@link #fallbackExecution}, 否则不会处理该事件.
	 */
	TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;

	/**
	 * 如果没有正在运行的事务, 是否应该处理事件.
	 */
	boolean fallbackExecution() default false;

	/**
	 * {@link #classes}的别名.
	 */
	@AliasFor(annotation = EventListener.class, attribute = "classes")
	Class<?>[] value() default {};

	/**
	 * 此监听器处理的事件类.
	 * <p>如果使用单个值指定此属性, 则带注解的方法可以选择接受单个参数.
	 * 但是, 如果使用多个值指定此属性, 则带注解的方法<em>不能</em>声明任何参数.
	 */
	@AliasFor(annotation = EventListener.class, attribute = "classes")
	Class<?>[] classes() default {};

	/**
	 * Spring Expression Language (SpEL)属性, 用于使事件处理成为条件.
	 * <p>默认{@code ""}, 表示始终处理事件.
	 */
	String condition() default "";

}
