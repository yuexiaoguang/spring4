package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记要调度的方法的注解.
 * 必须指定{@link #cron()}, {@link #fixedDelay()}, {@link #fixedRate()}属性中的一个.
 *
 * <p>带注解的方法必须没有参数.
 * 它通常具有{@code void}返回类型; 如果不是, 则通过调度程序调用时将忽略返回的值.
 *
 * <p>通过注册{@link ScheduledAnnotationBeanPostProcessor}来执行{@code @Scheduled}注解的处理.
 * 这可以通过{@code <task:annotation-driven/>} 元素或 @{@link EnableScheduling}注解手动完成, 也可以更方便地完成.
 *
 * <p>此注解可用作<em>元注解</em>以使用属性覆盖创建自定义<em>组合注解</em>.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Schedules.class)
public @interface Scheduled {

	/**
	 * 一个类似cron的表达式, 扩展了通常的 UN*X 定义, 包括秒, 分, 小时, 日期, 月份和星期几的触发器.
	 * <p>E.g. {@code "0 * * * * MON-FRI"} 星期一到星期五平均每分钟一次 (在分钟的顶部 - 第0秒).
	 * 
	 * @return 一个可以解析为cron计划的表达式
	 */
	String cron() default "";

	/**
	 * 将解析cron表达式的时区.
	 * 默认情况下, 此属性为空字符串 (i.e. 将使用服务器的本地时区).
	 * 
	 * @return {@link java.util.TimeZone#getTimeZone(String)}接受的区域ID, 或空字符串表示服务器的默认时区
	 */
	String zone() default "";

	/**
	 * 在上一次调用结束和下一次调用开始之间以固定周期(以毫秒为单位)执行带注解的方法.
	 * 
	 * @return 延迟, 以毫秒为单位
	 */
	long fixedDelay() default -1;

	/**
	 * 在上一次调用结束和下一次调用开始之间以固定周期(以毫秒为单位)执行带注解的方法.
	 * 
	 * @return 作为String值的延迟(以毫秒为单位), e.g. 占位符
	 */
	String fixedDelayString() default "";

	/**
	 * 在调用之间以固定的周期(以毫秒为单位)执行带注解的方法.
	 * 
	 * @return 周期, 以毫秒为单位
	 */
	long fixedRate() default -1;

	/**
	 * 在调用之间以固定的周期(以毫秒为单位)执行带注解的方法.
	 * 
	 * @return 作为String值的周期(以毫秒为单位), e.g. 占位符
	 */
	String fixedRateString() default "";

	/**
	 * 在第一次执行{@link #fixedRate()} 或 {@link #fixedDelay()}任务之前延迟的毫秒数.
	 * 
	 * @return 最初的延迟, 以毫秒为单位
	 */
	long initialDelay() default -1;

	/**
	 * 在第一次执行{@link #fixedRate()}或{@link #fixedDelay()}任务之前延迟的毫秒数.
	 * 
	 * @return 作为String值的初始延迟(以毫秒为单位), e.g. 占位符
	 */
	String initialDelayString() default "";

}
