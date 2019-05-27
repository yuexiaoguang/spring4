package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.util.Assert;

/**
 * 可以找到任意数量的JNDI对象的类的超类.
 * 派生自JndiAccessor以继承 "jndiTemplate"和"jndiEnvironment" bean属性.
 *
 * <p>JNDI名称可能包含也可能不包含J2EE应用程序在访问本地映射(ENC - Environmental Naming Context)资源时所需的 "java:comp/env/"前缀.
 * 如果没有, 如果"resourceRef"属性为true(默认为<strong>false</strong>)并且没有其他方案(e.g. "java:"),
 * 则前缀为"java:comp/env/"前缀.
 */
public abstract class JndiLocatorSupport extends JndiAccessor {

	/** JNDI prefix used in a J2EE container */
	public static final String CONTAINER_PREFIX = "java:comp/env/";


	private boolean resourceRef = false;


	/**
	 * 设置查找是否发生在J2EE容器中, i.e. 如果JNDI名称尚未包含前缀"java:comp/env/", 则需要添加前缀"java:comp/env/".
	 * 默认"false".
	 * <p>Note: 只有在没有给出其他方案 (e.g. "java:")时才会应用.
	 */
	public void setResourceRef(boolean resourceRef) {
		this.resourceRef = resourceRef;
	}

	/**
	 * 返回查询是否发生在J2EE容器中.
	 */
	public boolean isResourceRef() {
		return this.resourceRef;
	}


	/**
	 * 通过JndiTemplate对给定名称执行实际的JNDI查找.
     * <p>如果名称不以"java:comp/env/"开头, 如果"resourceRef"设置为"true", 则添加此前缀.
	 * 
	 * @param jndiName 要查找的JNDI名称
	 * 
	 * @return 获取到的对象
	 * @throws NamingException 如果JNDI查找失败
	 */
	protected Object lookup(String jndiName) throws NamingException {
		return lookup(jndiName, null);
	}

	/**
	 * 通过JndiTemplate对给定名称执行实际的JNDI查找.
	 * <p>如果名称不以"java:comp/env/"开头, 如果"resourceRef"设置为"true", 则添加此前缀.
	 * 
	 * @param jndiName 要查找的JNDI名称
	 * @param requiredType 所需的对象类型
	 * 
	 * @return 获取到的对象
	 * @throws NamingException 如果JNDI查找失败
	 */
	protected <T> T lookup(String jndiName, Class<T> requiredType) throws NamingException {
		Assert.notNull(jndiName, "'jndiName' must not be null");
		String convertedName = convertJndiName(jndiName);
		T jndiObject;
		try {
			jndiObject = getJndiTemplate().lookup(convertedName, requiredType);
		}
		catch (NamingException ex) {
			if (!convertedName.equals(jndiName)) {
				// 尝试回退到最初指定的名称...
				if (logger.isDebugEnabled()) {
					logger.debug("Converted JNDI name [" + convertedName +
							"] not found - trying original name [" + jndiName + "]. " + ex);
				}
				jndiObject = getJndiTemplate().lookup(jndiName, requiredType);
			}
			else {
				throw ex;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Located object with JNDI name [" + convertedName + "]");
		}
		return jndiObject;
	}

	/**
	 * 将给定的JNDI名称转换为要使用的实际JNDI名称.
	 * <p>如果"resourceRef"为"true", 并且没有给出其他方案(e.g. "java:"), 则默认实现应用"java:comp/env/"前缀.
	 * 
	 * @param jndiName 原始的JNDI名称
	 * 
	 * @return 要使用的JNDI名称
	 */
	protected String convertJndiName(String jndiName) {
		// Prepend container prefix if not already specified and no other scheme given.
		if (isResourceRef() && !jndiName.startsWith(CONTAINER_PREFIX) && jndiName.indexOf(':') == -1) {
			jndiName = CONTAINER_PREFIX + jndiName;
		}
		return jndiName;
	}

}
