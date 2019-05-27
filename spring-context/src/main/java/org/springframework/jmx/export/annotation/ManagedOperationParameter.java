package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级注解, 用于提供有关操作参数的元数据, 对应于{@code ManagedOperationParameter}属性.
 * 用作{@link ManagedOperationParameters}注解的一部分.
 *
 * <p>从Spring Framework 4.2.4开始, 此注解被声明为可重复的.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ManagedOperationParameters.class)
public @interface ManagedOperationParameter {

	String name();

	String description();

}
