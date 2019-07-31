package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标识初始化{@link org.springframework.web.bind.WebDataBinder}的方法.
 * 该方法将用于填充带注解的处理器方法的命令和表单对象参数.
 *
 * <p>这样的init-binder方法支持{@link RequestMapping}支持的所有参数, 除了命令/表单对象和相应的验证结果对象.
 * Init-binder方法不能有返回值; 它们通常被声明为{@code void}.
 *
 * <p>典型的参数是{@link org.springframework.web.bind.WebDataBinder}
 * 与{@link org.springframework.web.context.request.WebRequest}
 * 或{@link java.util.Locale}的组合, 允许注册特定于上下文的编辑器.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InitBinder {

	/**
	 * 应该将此init-binder方法应用于的命令/表单属性和/或请求参数的名称.
	 * <p>默认是应用于所有命令/表单属性, 以及由带注解的处理器类处理的所有请求参数.
	 * 在此处指定模型属性名称或请求参数名称将init-binder方法限制为那些特定属性/参数,
	 * 使用不同的init-binder方法通常应用于不同的属性或参数组.
	 */
	String[] value() default {};

}
