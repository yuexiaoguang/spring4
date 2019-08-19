package org.springframework.web.servlet.mvc.support;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;

/**
 * {@link ControllerTypePredicate}的扩展, 它也检测带注解的{@code @Controller} bean.
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
 */
@Deprecated
class AnnotationControllerTypePredicate extends ControllerTypePredicate {

	@Override
	public boolean isControllerType(Class<?> beanClass) {
		return (super.isControllerType(beanClass) ||
				AnnotationUtils.findAnnotation(beanClass, Controller.class) != null);
	}

	@Override
	public boolean isMultiActionControllerType(Class<?> beanClass) {
		return (super.isMultiActionControllerType(beanClass) ||
				AnnotationUtils.findAnnotation(beanClass, Controller.class) != null);
	}
}
