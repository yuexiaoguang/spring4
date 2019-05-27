package org.springframework.remoting.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.util.ClassUtils;

/**
 * 用于导出远程服务的类的抽象基类.
 * 提供"service"和"serviceInterface" bean属性.
 *
 * <p>请注意, 正在使用的服务接口将显示一些可远程访问的迹象, 例如它提供的方法调用的粒度.
 * 此外, 它必须具有可序列化的参数等.
 */
public abstract class RemoteExporter extends RemotingSupport {

	private Object service;

	private Class<?> serviceInterface;

	private Boolean registerTraceInterceptor;

	private Object[] interceptors;


	/**
	 * 设置要导出的服务.
	 * 通常通过bean引用填充.
	 */
	public void setService(Object service) {
		this.service = service;
	}

	/**
	 * 返回要导出的服务.
	 */
	public Object getService() {
		return this.service;
	}

	/**
	 * 设置要导出的服务的接口.
	 * 接口必须适合特定的服务和远程处理策略.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回要导出的服务接口.
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * 设置是否为导出的服务注册RemoteInvocationTraceInterceptor.
	 * 仅在子类使用{@code getProxyForService}创建要公开的代理时应用.
	 * <p>默认 "true".
	 * RemoteInvocationTraceInterceptor最重要的值是它记录服务器上的异常堆栈跟踪, 在将异常传播到客户端之前.
	 * 请注意, 如果已指定"interceptors"属性, 则默认情况下不会注册RemoteInvocationTraceInterceptor.
	 */
	public void setRegisterTraceInterceptor(boolean registerTraceInterceptor) {
		this.registerTraceInterceptor = registerTraceInterceptor;
	}

	/**
	 * 设置要在远程端点之前应用的其他拦截器 (或切面), e.g. a PerformanceMonitorInterceptor.
	 * <p>可以指定任何AOP Alliance MethodInterceptor或其他Spring AOP增强, 以及Spring AOP Advisor.
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors = interceptors;
	}


	/**
	 * 检查是否已设置服务引用.
	 */
	protected void checkService() throws IllegalArgumentException {
		if (getService() == null) {
			throw new IllegalArgumentException("Property 'service' is required");
		}
	}

	/**
	 * 检查是否已设置服务引用, 以及它是否与指定的服务匹配.
	 */
	protected void checkServiceInterface() throws IllegalArgumentException {
		Class<?> serviceInterface = getServiceInterface();
		Object service = getService();
		if (serviceInterface == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		if (service instanceof String) {
			throw new IllegalArgumentException("Service [" + service + "] is a String " +
					"rather than an actual service reference: Have you accidentally specified " +
					"the service bean name as value instead of as reference?");
		}
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException("Service interface [" + serviceInterface.getName() +
					"] needs to be implemented by service [" + service + "] of class [" +
					service.getClass().getName() + "]");
		}
	}

	/**
	 * 获取给定服务对象的代理, 实现指定的服务接口.
	 * <p>用于导出不公开任何内部, 但只是用于远程访问的特定接口的代理.
	 * 此外, 将注册{@link RemoteInvocationTraceInterceptor} (默认情况下).
	 * 
	 * @return 代理
	 */
	protected Object getProxyForService() {
		checkService();
		checkServiceInterface();

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.addInterface(getServiceInterface());

		if (this.registerTraceInterceptor != null ? this.registerTraceInterceptor : this.interceptors == null) {
			proxyFactory.addAdvice(new RemoteInvocationTraceInterceptor(getExporterName()));
		}
		if (this.interceptors != null) {
			AdvisorAdapterRegistry adapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
			for (Object interceptor : this.interceptors) {
				proxyFactory.addAdvisor(adapterRegistry.wrap(interceptor));
			}
		}

		proxyFactory.setTarget(getService());
		proxyFactory.setOpaque(true);

		return proxyFactory.getProxy(getBeanClassLoader());
	}

	/**
	 * 返回此导出器的简称.
	 * 用于跟踪远程调用.
	 * <p>默认是非限定类名 (没有包).
	 * 可以在子类中重写.
	 */
	protected String getExporterName() {
		return ClassUtils.getShortName(getClass());
	}

}
