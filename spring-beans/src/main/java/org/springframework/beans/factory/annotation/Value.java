package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段或方法/构造函数参数级别的注解, 指示受影响参数的默认值表达式.
 *
 * <p>通常用于表达式驱动的依赖注入. 还支持处理器方法参数的动态解析, e.g. in Spring MVC.
 *
 * <p>一个常见的用例是使用 "#{systemProperties.myProp}" 样式表达式分配默认字段值.
 *
 * <p>请注意{@code @Value}注解的实际处理由
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor}执行,
 * 意味着不能在
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 或
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}类型中使用{@code @Value}.
 * Please consult the javadoc for the {@link AutowiredAnnotationBeanPostProcessor} class 
 * (默认情况下, 它会检查是否存在此注解).
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {

	/**
	 * 实际值表达式: e.g. "#{systemProperties.myProp}".
	 */
	String value();

}
