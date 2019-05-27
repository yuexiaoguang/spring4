package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示带注解的类是"Controller" (e.g. a web controller).
 *
 * <p>此注解用作{@link Component @Component}的专业化, 允许通过类路径扫描自动检测实现类.
 * 它通常与带{@link org.springframework.web.bind.annotation.RequestMapping}注解的处理器方法结合使用.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {

	/**
	 * 该值可以指示对逻辑组件名称的建议, 在自动检测组件时将其转换为Spring bean.
	 * 
	 * @return 建议的组件名称(否则为空字符串)
	 */
	String value() default "";

}
