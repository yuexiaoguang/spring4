package org.springframework.web.servlet.mvc.support;

import org.springframework.web.servlet.mvc.Controller;

/**
 * 标识控制器类型的内部工具类.
 *
 * @deprecated 从4.3开始, 使用注解驱动的处理器方法
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
