package org.springframework.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JSR-303的{@link javax.validation.Valid}的变体, 支持验证组的规范.
 * 用于方便使用Spring的JSR-303支持, 但不特定于JSR-303.
 *
 * <p>可以使用 e.g. 使用Spring MVC处理器方法参数.
 * 通过{@link org.springframework.validation.SmartValidator}的验证提示概念支持, 验证组类充当提示对象.
 *
 * <p>也可以与方法级别验证一起使用, 指示应该在方法级别验证特定类 (充当相应验证拦截器的切点),
 * 但也可以选择在带注解的类中指定方法级验证的验证组.
 * 在方法级别应用此注解允许覆盖特定方法的验证组, 但不用作切点;
 * 但是, 必须使用类级别注解来触发特定bean的方法验证.
 * 也可以用作自定义构造型注解或自定义特定组验证注解的元注解.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Validated {

	/**
	 * 指定一个或多个验证组, 以应用于此注解启动的验证步骤.
	 * <p>JSR-303将验证组定义为自定义注解, 应用程序声明这些注解的唯一目的是将它们用作类型安全的组参数,
	 * 如{@link org.springframework.validation.beanvalidation.SpringValidatorAdapter}中所实现的那样.
	 * <p>其他{@link org.springframework.validation.SmartValidator}实现也可以通过其他方式支持类参数.
	 */
	Class<?>[] value() default {};

}
