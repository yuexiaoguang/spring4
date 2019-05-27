package org.springframework.jms.connection;

import javax.jms.Session;

/**
 * {@link javax.jms.Session}的子接口, 由Session代理实现. 允许访问底层目标Session.
 */
public interface SessionProxy extends Session {

	/**
	 * 返回此代理的目标会话.
	 * <p>这通常是本机提供者Session或会话池中的包装器.
	 * 
	 * @return 底层Session (never {@code null})
	 */
	Session getTargetSession();

}
