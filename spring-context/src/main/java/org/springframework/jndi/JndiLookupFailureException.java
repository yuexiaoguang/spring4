package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.core.NestedRuntimeException;

/**
 * 在JNDI查找失败的情况下抛出RuntimeException, 特别是来自未声明JNDI受检的{@link javax.naming.NamingException}的代码:
 * 例如, 来自Spring的 {@link JndiObjectTargetSource}.
 */
@SuppressWarnings("serial")
public class JndiLookupFailureException extends NestedRuntimeException {

	public JndiLookupFailureException(String msg, NamingException cause) {
		super(msg, cause);
	}

}
