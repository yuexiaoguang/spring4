package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示带注解的类是"Service", 最初由Domain-Driven Design (Evans, 2003)定义为
 * "作为一个独立于模型中的接口提供的操作, 没有封装状态."
 *
 * <p>也可能表明一个类是"Business Service Facade" (在核心J2EE模式意义上), 或类似的东西.
 * 这个注释是一个通用的构造型, 个别团队可能会缩小其语义并在适当时使用.
 *
 * <p>此注解用作{@link Component @Component}的专业化, 允许通过类路径扫描自动检测实现类.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Service {

	/**
	 * 该值可以指示对逻辑组件名称的建议, 在自动检测组件时将其转换为Spring bean.
	 * 
	 * @return 建议的组件名称 (否则为空字符串)
	 */
	String value() default "";

}
