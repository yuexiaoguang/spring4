package org.springframework.web.method.annotation;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;

/**
 * A TypeMismatchException raised while resolving a controller method argument.
 * Provides access to the target {@link org.springframework.core.MethodParameter
 * MethodParameter}.
 */
@SuppressWarnings("serial")
public class MethodArgumentTypeMismatchException extends TypeMismatchException {

	private final String name;

	private final MethodParameter parameter;


	public MethodArgumentTypeMismatchException(Object value, Class<?> requiredType,
			String name, MethodParameter param, Throwable cause) {

		super(value, requiredType, cause);
		this.name = name;
		this.parameter = param;
	}


	/**
	 * Return the name of the method argument.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the target method parameter.
	 */
	public MethodParameter getParameter() {
		return this.parameter;
	}

}
