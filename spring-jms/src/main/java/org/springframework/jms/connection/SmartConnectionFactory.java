package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

/**
 * {@code javax.jms.ConnectionFactory}接口的扩展, 指示如何释放从中获取的Connection.
 */
public interface SmartConnectionFactory extends ConnectionFactory {

	/**
	 * 应该停止从此ConnectionFactory获取的Connection?
	 * 
	 * @param con 要检查的Connection
	 * 
	 * @return 是否需要停止调用
	 */
	boolean shouldStop(Connection con);

}
