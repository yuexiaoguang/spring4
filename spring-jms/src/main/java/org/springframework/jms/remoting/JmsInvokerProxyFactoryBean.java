package org.springframework.jms.remoting;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

/**
 * JMS调用器代理的FactoryBean.
 * 使用指定的服务接口公开代理服务以用作bean引用.
 *
 * <p>序列化远程调用对象, 并反序列化远程调用结果对象.
 * 像RMI一样使用Java序列化, 但使用JMS提供者作为通信基础结构.
 *
 * <p>要配置{@link javax.jms.QueueConnectionFactory}和目标队列 (作为{@link javax.jms.Queue}引用或作为队列名称).
 */
public class JmsInvokerProxyFactoryBean extends JmsInvokerClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware {

	private Class<?> serviceInterface;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object serviceProxy;


	/**
	 * 设置代理必须实现的接口.
	 * 
	 * @param serviceInterface 代理必须实现的接口
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code serviceInterface} 是 {@code null}, 或者提供的{@code serviceInterface}不是接口类型
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface == null || !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (this.serviceInterface == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		this.serviceProxy = new ProxyFactory(this.serviceInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
