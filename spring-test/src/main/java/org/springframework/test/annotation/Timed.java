package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 特定于测试的注解, 指示测试方法必须在{@linkplain #millis() 指定的时间段内}完成执行.
 *
 * <p>如果文本执行时间超过指定的时间段, 则认为测试失败.
 *
 * <p>请注意, 时间段包括测试方法本身的执行, 测试的任何{@linkplain Repeat 重复},
 * 以及测试环境的任何<em>设置</em>或<em>拆卸</em>.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {

	/**
	 * 测试执行可以花费的最长时间 (以毫秒为单位), 不会由于耗时太长而被标记为失败.
	 */
	long millis();

}
