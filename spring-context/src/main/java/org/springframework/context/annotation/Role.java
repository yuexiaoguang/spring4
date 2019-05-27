package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * 表示给定bean的“角色”提示.
 *
 * <p>可以在直接或间接使用{@link org.springframework.stereotype.Component}注解的类或使用{@link Bean}注解的方法上使用.
 *
 * <p>如果组件或Bean定义中不存在此注解, 则将应用{@link BeanDefinition#ROLE_APPLICATION}的默认值.
 *
 * <p>如果{@link Configuration @Configuration}类中存在Role, 则表示配置类bean定义的角色,
 * 并且不会级联到在其中定义的所有 @{@code Bean}方法.
 * 例如, 此行为与 @{@link Lazy}注解的行为不同.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Role {

	/**
	 * 设置关联bean的角色提示.
	 */
	int value();
}
