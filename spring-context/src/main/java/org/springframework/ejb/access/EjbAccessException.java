package org.springframework.ejb.access;

import org.springframework.core.NestedRuntimeException;

/**
 * 无法正确访问EJBstub时抛出的异常.
 */
@SuppressWarnings("serial")
public class EjbAccessException extends NestedRuntimeException {

	public EjbAccessException(String msg) {
		super(msg);
	}

	public EjbAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
