package org.springframework.web.bind.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 使用{@link ControllerAdvice @ControllerAdvice}和{@link ResponseBody @ResponseBody}注解作为元注解.
 *
 * <p>带有此注解的类型被视为控制器增强,
 * 其中{@link ExceptionHandler @ExceptionHandler}方法默认采用{@link ResponseBody @ResponseBody}语义.
 *
 * <p><b>NOTE:</b> 如果配置了适当的{@code HandlerMapping}-{@code HandlerAdapter}对,
 * 例如{@code RequestMappingHandlerMapping}-{@code RequestMappingHandlerAdapter}对,
 * 它们是MVC Java配置和MVC命名空间中的默认配置, 则会处理{@code @RestControllerAdvice}.
 * 特别是{@code @RestControllerAdvice}不支持已弃用的
 * {@code DefaultAnnotationHandlerMapping}-{@code AnnotationMethodHandlerAdapter}对.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ControllerAdvice
@ResponseBody
public @interface RestControllerAdvice {

	/**
	 * {@link #basePackages}属性的别名.
	 * <p>允许更简洁的注解声明 e.g.:
	 * {@code @ControllerAdvice("org.my.pkg")}等同于 {@code @ControllerAdvice(basePackages="org.my.pkg")}.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * 基础包的数组.
	 * <p>将包括属于那些基础包或其子包的控制器,
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
	 * <p>可分配给至少一种给定类型的Controller, 将由带{@code @ControllerAdvice}注解的类辅助.
	 */
	Class<?>[] assignableTypes() default {};

	/**
	 * 注解的数组.
	 * <p>使用此注解的Controller将由带{@code @ControllerAdvice}注解的类辅助.
	 * <p>考虑创建一个特殊的注解或使用预定义的注解, 如{@link RestController @RestController}.
	 */
	Class<? extends Annotation>[] annotations() default {};

}
