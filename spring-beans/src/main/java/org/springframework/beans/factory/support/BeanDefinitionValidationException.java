package org.springframework.beans.factory.support;

import org.springframework.beans.FatalBeanException;

/**
 * 当bean定义的验证失败时抛出.
 */
@SuppressWarnings("serial")
public class BeanDefinitionValidationException extends FatalBeanException {

	public BeanDefinitionValidationException(String msg) {
		super(msg);
	}

	public BeanDefinitionValidationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
