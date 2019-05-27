package org.springframework.remoting.caucho;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} for Hessian proxies. Exposes the proxied service
 * for use as a bean reference, using the specified service interface.
 *
 * <p>Hessian is a slim, binary RPC protocol.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>
 * <b>Note: As of Spring 4.0, this proxy factory requires Hessian 4.0 or above.</b>
 *
 * <p>The service URL must be an HTTP URL exposing a Hessian service.
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
