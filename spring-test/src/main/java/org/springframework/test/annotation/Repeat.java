package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 测试注解, 以指示应该重复调用测试方法.
 *
 * <p>请注意, 要重复执行的范围包括测试方法本身的执行, 以及测试环境的任何<em>设置</em>或<em>拆卸</em>.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Repeat {

	/**
	 * 带注解的测试方法应重复的次数.
	 */
	int value() default 1;

}
