package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示当所有{@linkplain #value 指定条件}匹配时, 组件才符合注册条件.
 *
 * <p><em>条件</em>是可以在bean定义注册之前以编程方式确定的任何状态 (see {@link Condition} for details).
 *
 * <p>{@code @Conditional}注解可以通过以下任何方式使用:
 * <ul>
 * <li>作为使用{@code @Component}直接或间接注解的类的类型级注解, 包括{@link Configuration @Configuration}类</li>
 * <li>作为元注解, 用于组成自定义构造型注解</li>
 * <li>作为{@link Bean @Bean}方法的方法级注解</li>
 * </ul>
 *
 * <p>如果{@code @Configuration}类标有{@code @Conditional},
 * 所有{@code @Bean}方法, {@link Import @Import}注解, 以及与该类关联的{@link ComponentScan @ComponentScan}注解都将受条件限制.
 *
 * <p><strong>NOTE</strong>: 不支持继承{@code @Conditional}注解;
 * 来自超类或来自重写方法的任何条件都不会被考虑.
 * 为了强制执行这些语义, {@code @Conditional}本身未声明为 {@link java.lang.annotation.Inherited @Inherited};
 * 此外, 任何带{@code @Conditional}注解的元注解的自定义<em>组合注解</em>都不得声明为{@code @Inherited}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * 所有{@link Condition}必须按顺序{@linkplain Condition#matches 匹配}, 才能注册组件.
	 */
	Class<? extends Condition>[] value();

}
