package org.springframework.beans.factory.support;

import java.security.AccessControlContext;
import java.security.AccessController;

/**
 * 简单的{@link SecurityContextProvider}实现.
 */
public class SimpleSecurityContextProvider implements SecurityContextProvider {

	private final AccessControlContext acc;


	/**
	 * <p>将在当前线程的每次调用时检索安全上下文.
	 */
	public SimpleSecurityContextProvider() {
		this(null);
	}

	/**
	 * <p>如果给定的控制上下文为null, 则将在当前线程的每次调用时检索安全上下文.
	 * 
	 * @param acc 访问控制上下文 (can be {@code null})
	 */
	public SimpleSecurityContextProvider(AccessControlContext acc) {
		this.acc = acc;
	}


	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.acc != null ? acc : AccessController.getContext());
	}

}
