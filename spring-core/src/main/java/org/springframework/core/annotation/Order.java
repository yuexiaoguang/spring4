package org.springframework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;

/**
 * {@code @Order}定义带注解的组件的排序顺序.
 *
 * <p>{@link #value}是可选的, 表示{@link Ordered}接口中定义的顺序值.
 * 较低的值具有较高的优先级.
 * 默认值为{@code Ordered.LOWEST_PRECEDENCE}, 表示最低优先级.
 *
 * <p><b>NOTE:</b> 从Spring 4.0开始, Spring中的多种组件都支持基于注解的排序, 甚至是集合注入,
 * 其中考虑了目标组件的顺序值 (来自他们的目标类或来自他们的{@code @Bean}方法).
 * 虽然此类顺序值可能影响注入点的优先级, 但请注意它们不会影响单例的启动顺序,
 * 这是由依赖关系和{@code @DependsOn}声明确定的正交关注点 (影响运行时确定的依赖关系图).
 *
 * <p>从Spring 4.1开始, 标准{@link javax.annotation.Priority}注解可用作排序场景中此注解的替代品.
 * 请注意, 当必须选择单个元素时, {@code Priority}可能具有其他语义 (see {@link AnnotationAwareOrderComparator#getPriority}).
 *
 * <p>或者, 也可以通过{@link Ordered}接口在每个实例的基础上确定顺序值, 允许配置确定的实例值, 而不是附加到特定类的硬编码值.
 *
 * <p>有关非有序对象的排序语义的详细信息, 请参阅{@link org.springframework.core.OrderComparator OrderComparator}的javadoc.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {

	/**
	 * 顺序值.
	 * <p>默认 {@link Ordered#LOWEST_PRECEDENCE}.
	 */
	int value() default Ordered.LOWEST_PRECEDENCE;

}
