package org.springframework.core.annotation;

import org.springframework.core.NestedRuntimeException;

/**
 * 如果注解配置不正确, 则由{@link AnnotationUtils}和<em>合成注解</em>引发.
 */
@SuppressWarnings("serial")
public class AnnotationConfigurationException extends NestedRuntimeException {

	public AnnotationConfigurationException(String message) {
		super(message);
	}

	public AnnotationConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
