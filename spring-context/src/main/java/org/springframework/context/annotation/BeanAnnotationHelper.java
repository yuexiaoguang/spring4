package org.springframework.context.annotation;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 用于处理带{@link Bean}注解的方法的实用程序.
 */
class BeanAnnotationHelper {

	public static boolean isBeanAnnotated(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, Bean.class);
	}

	public static String determineBeanNameFor(Method beanMethod) {
		// 默认情况下, bean名称是带 @Bean注解的方法的名称
		String beanName = beanMethod.getName();
		// 检查用户是否已显式设置自定义bean名称...
		Bean bean = AnnotatedElementUtils.findMergedAnnotation(beanMethod, Bean.class);
		if (bean != null) {
			String[] names = bean.name();
			if (names.length > 0) {
				beanName = names[0];
			}
		}
		return beanName;
	}

}
