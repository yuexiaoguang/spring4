package org.springframework.remoting.rmi;

import javax.naming.NamingException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

/**
 * 来自JNDI的RMI代理的{@link FactoryBean}.
 *
 * <p>通常用于RMI-IIOP (CORBA), 但也可用于EJB主对象 (例如, a Stateful Session Bean home).
 * 与普通的JNDI查找相比, 此访问器还通过{@link javax.rmi.PortableRemoteObject}执行缩小.
 *
 * <p>对于传统的RMI服务, 此调用器通常与RMI服务接口一起使用.
 * 或者, 此调用器还可以使用匹配的非RMI业务接口代理远程RMI服务,
 * i.e. 一个镜像RMI服务方法但不声明RemoteExceptions的接口.
 * 在后一种情况下, RMI stub引发的RemoteExceptions将自动转换为Spring未受检的RemoteAccessException.
 *
 * <p>JNDI环境可以指定为"jndiEnvironment"属性, 或者在{@code jndi.properties}文件中配置或作为系统属性配置.
 * For example:
 *
 * <pre class="code">&lt;property name="jndiEnvironment"&gt;
 * 	 &lt;props>
 *		 &lt;prop key="java.naming.factory.initial"&gt;com.sun.jndi.cosnaming.CNCtxFactory&lt;/prop&gt;
 *		 &lt;prop key="java.naming.provider.url"&gt;iiop://localhost:1050&lt;/prop&gt;
 *	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 */
public class JndiRmiProxyFactoryBean extends JndiRmiClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware {

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (getServiceInterface() == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(this.beanClassLoader);
	}


	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
