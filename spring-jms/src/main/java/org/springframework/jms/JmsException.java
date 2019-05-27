package org.springframework.jms;

import javax.jms.JMSException;

import org.springframework.core.NestedRuntimeException;

/**
 * 每当遇到与JMS相关的问题时, 框架抛出异常的基类.
 */
@SuppressWarnings("serial")
public abstract class JmsException extends NestedRuntimeException {

	public JmsException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 异常的原因.
	 * 通常期望此参数是{@link javax.jms.JMSException}的正确子类, 但也可以是JNDI NamingException等.
	 */
	public JmsException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param cause 异常的原因.
	 * 通常期望此参数是{@link javax.jms.JMSException}的正确子类.
	 */
	public JmsException(Throwable cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}


	/**
	 * 如果根本原因是JMSException的实例, 获取供应商特定错误代码.
	 * 
	 * @return 如果根本原因是JMSException的实例, 则为特定于供应商的错误代码; 或{@code null}
	 */
	public String getErrorCode() {
		Throwable cause = getCause();
		if (cause instanceof JMSException) {
			return ((JMSException) cause).getErrorCode();
		}
		return null;
	}

	/**
	 * 返回详细消息, 包括来自链接的异常的消息.
	 */
	@Override
	public String getMessage() {
		String message = super.getMessage();
		Throwable cause = getCause();
		if (cause instanceof JMSException) {
			Exception linkedEx = ((JMSException) cause).getLinkedException();
			if (linkedEx != null) {
				String linkedMessage = linkedEx.getMessage();
				String causeMessage = cause.getMessage();
				if (linkedMessage != null && (causeMessage == null || !causeMessage.contains(linkedMessage))) {
					message = message + "; nested exception is " + linkedEx;
				}
			}
		}
		return message;
	}
}
