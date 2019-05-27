package org.springframework.web.servlet.mvc.support;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;

/**
 * Extension of {@link ControllerTypePredicate} that detects
 * annotated {@code @Controller} beans as well.
 *
 * @deprecated as of 4.3, in favor of annotation-driven handler methods
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
