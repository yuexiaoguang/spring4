package org.springframework.web.method.annotation;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;

/**
 * 解析控制器方法参数时引发的TypeMismatchException.
 * 可以访问目标{@link org.springframework.core.MethodParameter MethodParameter}.
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
	 * 返回方法参数的名称.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回目标方法参数.
	 */
	public MethodParameter getParameter() {
		return this.parameter;
	}

}
