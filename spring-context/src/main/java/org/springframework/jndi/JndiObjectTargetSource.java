package org.springframework.jndi;

import javax.naming.NamingException;

import org.springframework.aop.TargetSource;

/**
 * AOP {@link org.springframework.aop.TargetSource}, 为{@code getTarget()}调用提供可配置的JNDI查找.
 *
 * <p>可以用作{@link JndiObjectFactoryBean}的替代, 以允许延迟地重定位JNDI对象或每个操作
 * (see "lookupOnStartup" and "cache" properties).
 * 这在开发期间特别有用, 因为它允许热重启JNDI服务器 (例如, 远程JMS服务器).
 *
 * <p>Example:
 *
 * <pre class="code">
 * &lt;bean id="queueConnectionFactoryTarget" class="org.springframework.jndi.JndiObjectTargetSource"&gt;
 *   &lt;property name="jndiName" value="JmsQueueConnectionFactory"/&gt;
 *   &lt;property name="lookupOnStartup" value="false"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="queueConnectionFactory" class="org.springframework.aop.framework.ProxyFactoryBean"&gt;
 *   &lt;property name="proxyInterfaces" value="javax.jms.QueueConnectionFactory"/&gt;
 *   &lt;property name="targetSource" ref="queueConnectionFactoryTarget"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 对"queueConnectionFactory"代理进行{@code createQueueConnection}调用将导致对"JmsQueueConnectionFactory"的延迟JNDI查找,
 * 以及对检索到的QueueConnectionFactory的{@code createQueueConnection}的后续委托调用.
 *
 * <p><b>或者, 使用带有"proxyInterface"的{@link JndiObjectFactoryBean}.</b>
 * 然后可以在JndiObjectFactoryBean上指定"lookupOnStartup"和"cache", 在下面创建一个JndiObjectTargetSource
 * (而不是定义单独的ProxyFactoryBean 和 JndiObjectTargetSource bean).
 */
public class JndiObjectTargetSource extends JndiObjectLocator implements TargetSource {

	private boolean lookupOnStartup = true;

	private boolean cache = true;

	private Object cachedObject;

	private Class<?> targetClass;


	/**
	 * 设置是否在启动时查找JNDI对象. 默认"true".
	 * <p>可以关闭以允许JNDI对象的延迟可用性.
	 * 在这种情况下, 将在首次访问时获取JNDI对象.
	 */
	public void setLookupOnStartup(boolean lookupOnStartup) {
		this.lookupOnStartup = lookupOnStartup;
	}

	/**
	 * 设置是否在找到JNDI对象后对其进行缓存.
	 * 默认"true".
	 * <p>可以关闭以允许热重新部署JNDI对象.
	 * 在这种情况下,将为每次调用获取JNDI对象.
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.lookupOnStartup) {
			Object object = lookup();
			if (this.cache) {
				this.cachedObject = object;
			}
			else {
				this.targetClass = object.getClass();
			}
		}
	}


	@Override
	public Class<?> getTargetClass() {
		if (this.cachedObject != null) {
			return this.cachedObject.getClass();
		}
		else if (this.targetClass != null) {
			return this.targetClass;
		}
		else {
			return getExpectedType();
		}
	}

	@Override
	public boolean isStatic() {
		return (this.cachedObject != null);
	}

	@Override
	public Object getTarget() {
		try {
			if (this.lookupOnStartup || !this.cache) {
				return (this.cachedObject != null ? this.cachedObject : lookup());
			}
			else {
				synchronized (this) {
					if (this.cachedObject == null) {
						this.cachedObject = lookup();
					}
					return this.cachedObject;
				}
			}
		}
		catch (NamingException ex) {
			throw new JndiLookupFailureException("JndiObjectTargetSource failed to obtain new target object", ex);
		}
	}

	@Override
	public void releaseTarget(Object target) {
	}

}
