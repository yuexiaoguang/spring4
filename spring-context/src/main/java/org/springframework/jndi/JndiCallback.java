package org.springframework.jndi;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * 由需要在JNDI上下文中执行操作 (例如查找)的类实现的回调接口.
 * 这种回调方法在简化错误处理方面很有价值, 错误处理是由JndiTemplate类执行的.
 * 这与JdbcTemplate的方法类似.
 *
 * <p>请注意, 几乎不需要实现此回调接口, 因为JndiTemplate通过便捷方法提供所有常见的JNDI操作.
 */
public interface JndiCallback<T> {

	/**
	 * 使用给定的JNDI上下文执行某些操作.
	 * <p>实现不需要担心错误处理或清理, 因为JndiTemplate类将处理此问题.
	 * 
	 * @param ctx 当前的JNDI上下文
	 * 
	 * @throws NamingException 如果由JNDI方法抛出
	 * @return 结果对象, 或{@code null}
	 */
	T doInContext(Context ctx) throws NamingException;

}

