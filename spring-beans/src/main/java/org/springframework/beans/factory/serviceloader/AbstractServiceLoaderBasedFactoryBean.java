package org.springframework.beans.factory.serviceloader;

import java.util.ServiceLoader;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * FactoryBeans的抽象基类, 在JDK 1.6 {@link java.util.ServiceLoader}工具上运行.
 */
public abstract class AbstractServiceLoaderBasedFactoryBean extends AbstractFactoryBean<Object>
		implements BeanClassLoaderAware {

	private Class<?> serviceType;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * 指定所需的服务类型 (通常是服务的公共API).
	 */
	public void setServiceType(Class<?> serviceType) {
		this.serviceType = serviceType;
	}

	/**
	 * 返回所需的服务类型.
	 */
	public Class<?> getServiceType() {
		return this.serviceType;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 委托给{@link #getObjectToExpose(java.util.ServiceLoader)}.
	 * 
	 * @return 要公开的对象
	 */
	@Override
	protected Object createInstance() {
		Assert.notNull(getServiceType(), "Property 'serviceType' is required");
		return getObjectToExpose(ServiceLoader.load(getServiceType(), this.beanClassLoader));
	}

	/**
	 * 确定要为给定的ServiceLoader暴露的实际对象.
	 * <p>留给具体的子类.
	 * 
	 * @param serviceLoader 已配置的服务类的ServiceLoader
	 * 
	 * @return 要暴露的对象
	 */
	protected abstract Object getObjectToExpose(ServiceLoader<?> serviceLoader);

}
