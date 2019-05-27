package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;

/**
 * 表示表达式评估失败的异常.
 */
@SuppressWarnings("serial")
public class BeanExpressionException extends FatalBeanException {

	/**
	 * @param msg the detail message
	 */
	public BeanExpressionException(String msg) {
		super(msg);
	}

	/**
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanExpressionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
