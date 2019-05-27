package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

/**
 * 基于JNDI的服务定位器的便捷超类, 提供特定JNDI资源的可配置查找.
 *
 * <p>公开{@link #setJndiName "jndiName"}属性.
 * 这可能包括也可能不包括J2EE应用程序在访问本地映射(Environmental Naming Context)资源时所需的"java:comp/env/"前缀.
 * 如果没有, 如果"resourceRef"属性为true(默认为<strong>false</strong>), 并且没有其他方案(e.g. "java:"), 则前缀为"java:comp/env/".
 *
 * <p>子类可以在适当的时候调用{@link #lookup()}方法.
 * 有些类可能会在初始化时执行此操作, 而其他类可能会按需执行此操作.
 * 后一种策略更灵活, 因为它允许在JNDI对象可用之前初始化定位器.
 */
public abstract class JndiObjectLocator extends JndiLocatorSupport implements InitializingBean {

	private String jndiName;

	private Class<?> expectedType;


	/**
	 * 指定要查找的JNDI名称.
	 * 如果它不以"java:comp/env/"开头, 如果"resourceRef"设置为"true", 则会自动添加此前缀.
	 * 
	 * @param jndiName 要查找的JNDI名称
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	/**
	 * 返回要查找的JNDI名称.
	 */
	public String getJndiName() {
		return this.jndiName;
	}

	/**
	 * 指定所定位的JNDI对象应该可分配的类型.
	 */
	public void setExpectedType(Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	/**
	 * 返回所定位的JNDI对象应该可分配的类型.
	 */
	public Class<?> getExpectedType() {
		return this.expectedType;
	}

	@Override
	public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
		if (!StringUtils.hasLength(getJndiName())) {
			throw new IllegalArgumentException("Property 'jndiName' is required");
		}
	}


	/**
	 * 对此定位器的目标资源执行实际的JNDI查找.
	 * 
	 * @return 找到的目标对象
	 * @throws NamingException 如果JNDI查找失败, 或者找到的JNDI对象不能分配给期望的类型
	 */
	protected Object lookup() throws NamingException {
		return lookup(getJndiName(), getExpectedType());
	}
}
