package org.springframework.test.annotation;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 用于处理Spring核心测试注解的方法.
 */
public class TestAnnotationUtils {

	/**
	 * 通过提供的{@code method}上的{@link Timed @Timed}注解配置{@code timeout}.
	 * <p>负值将转换为{@code 0}.
	 * 
	 * @return 配置的超时, 如果方法未使用{@code @Timed}注解, 则为{@code 0}
	 */
	public static long getTimeout(Method method) {
		Timed timed = AnnotatedElementUtils.findMergedAnnotation(method, Timed.class);
		if (timed == null) {
			return 0;
		}
		return Math.max(0, timed.millis());
	}

	/**
	 * 获取通过提供的{@code method}上的{@link Repeat @Repeat}注解配置的重复计数.
	 * <p>负值将转换为{@code 1}.
	 * 
	 * @return 配置的重复计数, 如果方法未使用{@code @Repeat}注解, 则为{@code 1}
	 */
	public static int getRepeatCount(Method method) {
		Repeat repeat = AnnotatedElementUtils.findMergedAnnotation(method, Repeat.class);
		if (repeat == null) {
			return 1;
		}
		return Math.max(1, repeat.value());
	}

}
