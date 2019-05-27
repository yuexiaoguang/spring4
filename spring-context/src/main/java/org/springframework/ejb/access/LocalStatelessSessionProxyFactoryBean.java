package org.springframework.ejb.access;

import javax.naming.NamingException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

/**
 * 用于本地无状态会话Bean (SLSB) 代理的{@link FactoryBean}.
 * 专为EJB 2.x设计, 但也适用于EJB 3会话Bean.
 *
 * <p>有关如何指定目标EJB的JNDI位置的信息, 请参阅{@link org.springframework.jndi.JndiObjectLocator}.
 *
 * <p>如果想控制拦截器链, 使用AOP ProxyFactoryBean和LocalSlsbInvokerInterceptor, 而不是依赖这个类.
 *
 * <p>在bean容器中, 此类通常最适合用作单例.
 * 但是, 如果该bean容器预先实例化单例 (就像 XML ApplicationContext变体一样),
 * 如果在EJB容器加载目标EJB之前加载bean容器, 则可能会出现问题.
 * 这是因为默认情况下, JNDI查找将在此类的init方法中执行并缓存, 但EJB尚未绑定到目标位置.
 * 最好的解决方案是将lookupHomeOnStartup属性设置为false, 在这种情况下, 首次访问EJB时将获取home.
 * (出于向后兼容性原因, 此标志仅在默认情况下为true).
 */
public class LocalStatelessSessionProxyFactoryBean extends LocalSlsbInvokerInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware {

	/** 代理的EJB的业务接口 */
	private Class<?> businessInterface;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** EJBLocalObject */
	private Object proxy;


	/**
	 * 设置代理的EJB的业务接口.
	 * 这通常是EJB本地组件接口的超级接口.
	 * 在实现EJB时, 使用业务方法接口是最佳实践.
	 * 
	 * @param businessInterface 设置EJB的业务接口
	 */
	public void setBusinessInterface(Class<?> businessInterface) {
		this.businessInterface = businessInterface;
	}

	/**
	 * 返回代理的EJB的业务接口.
	 */
	public Class<?> getBusinessInterface() {
		return this.businessInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.businessInterface == null) {
			throw new IllegalArgumentException("businessInterface is required");
		}
		this.proxy = new ProxyFactory(this.businessInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	public Object getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.businessInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
