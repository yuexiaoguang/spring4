package org.springframework.web.servlet.mvc.support;

import org.springframework.web.servlet.mvc.Controller;

/**
 * Internal helper class that identifies controller types.
 *
 * @deprecated as of 4.3, in favor of annotation-driven handler methods
 */
@Deprecated
class ControllerTypePredicate {

	public boolean isControllerType(Class<?> beanClass) {
		return Controller.class.isAssignableFrom(beanClass);
	}

	@SuppressWarnings("deprecation")
	public boolean isMultiActionControllerType(Class<?> beanClass) {
		return org.springframework.web.servlet.mvc.multiaction.MultiActionController.class.isAssignableFrom(beanClass);
	}

}
