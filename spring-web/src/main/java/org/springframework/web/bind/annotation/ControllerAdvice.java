package org.springframework.web.bind.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * {@link Component @Component}专门用于声明{@link ExceptionHandler @ExceptionHandler},
 * {@link InitBinder @InitBinder}, 或{@link ModelAttribute @ModelAttribute}方法的类,
 * 以便在多个{@code @Controller}类之间共享.
 *
 * <p>具有{@code @ControllerAdvice}的类可以显式声明为Spring bean或通过类路径扫描自动检测.
 * 所有这些bean都是通过
 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator AnnotationAwareOrderComparator}进行排序的,
 * i.e. 基于{@link org.springframework.core.annotation.Order @Order}和{@link org.springframework.core.Ordered Ordered},
 * 并在运行时以该顺序应用.
 * 为了处理异常, 将使用匹配的异常处理器方法在第一个增强上选择{@code @ExceptionHandler}.
 * 对于model属性和{@code InitBinder}初始化, {@code @ModelAttribute}和{@code @InitBinder}方法
 * 也将遵循{@code @ControllerAdvice}顺序.
 *
 * <p>Note: 对于{@code @ExceptionHandler}方法, 在特定增强bean的处理器方法中, 匹配的根异常将优先于匹配的当前异常的原因.
 * 但是, 优先级较高的增强的原因匹配仍然优先于优先级较低的增强bean上的任何匹配 (无论是根异常还是原因级别).
 * 因此, 请在具有相应顺序的优先级增强bean上声明主根异常映射!
 *
 * <p>默认情况下, {@code @ControllerAdvice}中的方法全局应用于所有Controller.
 * 使用选择器{@link #annotations()}, {@link #basePackageClasses()},
 * 和{@link #basePackages()} (或其别名{@link #value()})来定义更窄的目标Controller子集.
 * 如果声明了多个选择器, 则应用OR逻辑, 这意味着所选的Controller应匹配至少一个选择器.
 * 请注意, 选择器检查是在运行时执行的, 因此添加许多选择器可能会对性能产生负面影响并增加复杂性.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ControllerAdvice {

	/**
	 * {@link #basePackages}属性的别名.
	 * <p>允许更简洁的注解声明 e.g.:
	 * {@code @ControllerAdvice("org.my.pkg")}等同于{@code @ControllerAdvice(basePackages="org.my.pkg")}.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * 基础包的数组.
	 * <p>将包括属于那些基础包或其子包的Controller,
	 * e.g.: {@code @ControllerAdvice(basePackages="org.my.pkg")}
	 * 或{@code @ControllerAdvice(basePackages={"org.my.pkg", "org.my.other.pkg"})}.
	 * <p>{@link #value}是此属性的别名, 只是允许更简洁地使用注解.
	 * <p>还可以考虑使用{@link #basePackageClasses()}作为基于字符串的包名称的类型安全替代方法.
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * {@link #value()}的类型安全替代方法, 用于指定包,
	 * 以选择由带{@code @ControllerAdvice}注解的类辅助的Controller.
	 * <p>考虑在每个包中创建一个特殊的无操作标记类或接口, 除了被该属性引用之外没有其它用途.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * 类的数组.
	 * <p>可分配给至少一种给定类型的Controller将由带{@code @ControllerAdvice}注解的类辅助.
	 */
	Class<?>[] assignableTypes() default {};

	/**
	 * 注解的数组.
	 * <p>使用此注解/其中一个注解的Controller将由带{@code @ControllerAdvice}注解的类辅助.
	 * <p>考虑创建一个特殊的注解或使用预定义的注解, 如{@link RestController @RestController}.
	 */
	Class<? extends Annotation>[] annotations() default {};

}
