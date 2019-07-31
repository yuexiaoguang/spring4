package org.springframework.remoting.caucho;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * 用于Hessian代理的{@link FactoryBean}. 使用指定的服务接口公开代理服务以用作bean引用.
 *
 * <p>Hessian是一种轻量级的二进制RPC协议.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>
 * <b>Note: 从Spring 4.0开始, 这个代理工厂需要Hessian 4.0或更高版本.</b>
 *
 * <p>服务URL必须是公开Hessian服务的HTTP URL.
 * For details, see the {@link HessianClientInterceptor} javadoc.
 */
public class HessianProxyFactoryBean extends HessianClientInterceptor implements FactoryBean<Object> {

	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(getBeanClassLoader());
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
