package org.springframework.beans.factory.support;

import java.security.AccessControlContext;

/**
 * 在bean工厂内运行的代码的安全上下文的提供者.
 */
public interface SecurityContextProvider {

	/**
	 * 提供与Bean工厂相关的安全访问控制上下文.
	 * 
	 * @return bean工厂安全控制上下文
	 */
	AccessControlContext getAccessControlContext();

}
