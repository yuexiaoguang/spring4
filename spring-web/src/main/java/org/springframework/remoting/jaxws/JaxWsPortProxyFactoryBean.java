package org.springframework.remoting.jaxws;

import javax.xml.ws.BindingProvider;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * 用于JAX-WS服务的特定端口的{@link org.springframework.beans.factory.FactoryBean}.
 * 公开端口的代理, 用于bean引用.
 * 从{@link JaxWsPortClientInterceptor}继承配置属性.
 */
public class JaxWsPortProxyFactoryBean extends JaxWsPortClientInterceptor
		implements FactoryBean<Object> {

	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// 构建一个也公开JAX-WS BindingProvider接口的代理.
		ProxyFactory pf = new ProxyFactory();
		pf.addInterface(getServiceInterface());
		pf.addInterface(BindingProvider.class);
		pf.addAdvice(this);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
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
