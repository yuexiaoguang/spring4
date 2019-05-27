package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示带注解的类是“组件”.
 * 当使用基于注解的配置和类路径扫描时, 这些类被视为自动检测的候选者.
 *
 * <p>其他类级注解也可以被视为标识组件, 通常是特殊类型的组件:
 * e.g. {@link Repository @Repository}注解, 或AspectJ的 {@link org.aspectj.lang.annotation.Aspect @Aspect}注解.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {

	/**
	 * 该值可以指示对逻辑组件名称的建议, 在自动检测组件时, 将其转换为Spring bean.
	 * 
	 * @return 建议的组件名称 (否则为空字符串)
	 */
	String value() default "";

}
