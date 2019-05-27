package org.springframework.jmx.access;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.util.ClassUtils;

/**
 * 为本地或远程运行的受管资源创建代理.
 * "proxyInterface"属性定义生成的代理应该实现的接口.
 * 此接口应定义与要代理的资源的管理接口中的操作和属性相对应的方法和属性.
 *
 * <p>托管资源不需要实现代理接口, 尽管您可能会觉得这样做很方便.
 * 管理接口中的每个操作和属性都不需要与代理接口中的相应属性或方法匹配.
 *
 * <p>尝试调用或访问代理接口上与管理接口不对应的方法或属性, 将导致{@code InvalidInvocationException}.
 */
public class MBeanProxyFactoryBean extends MBeanClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {

	private Class<?> proxyInterface;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object mbeanProxy;


	/**
	 * 设置生成的代理将实现的接口.
	 * <p>这通常是与目标MBean匹配的管理接口,
	 * 公开MBean属性的bean属性setter和getter, 以及用于MBean操作的Java方法.
	 */
	public void setProxyInterface(Class<?> proxyInterface) {
		this.proxyInterface = proxyInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 检查是否已指定{@code proxyInterface}, 然后为目标MBean生成代理.
	 */
	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException, MBeanInfoRetrievalException {
		super.afterPropertiesSet();

		if (this.proxyInterface == null) {
			this.proxyInterface = getManagementInterface();
			if (this.proxyInterface == null) {
				throw new IllegalArgumentException("Property 'proxyInterface' or 'managementInterface' is required");
			}
		}
		else {
			if (getManagementInterface() == null) {
				setManagementInterface(this.proxyInterface);
			}
		}
		this.mbeanProxy = new ProxyFactory(this.proxyInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	public Object getObject() {
		return this.mbeanProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.proxyInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
