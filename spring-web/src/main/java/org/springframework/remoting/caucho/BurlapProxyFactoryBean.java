package org.springframework.remoting.caucho;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean}用于Burlap代理. 使用指定的服务接口公开代理服务以用作bean引用.
 *
 * <p>Burlap是一种基于XML的轻量级RPC协议.
 * For information on Burlap, see the
 * <a href="http://www.caucho.com/burlap">Burlap website</a>
 *
 * <p>服务URL必须是公开Burlap服务的HTTP URL.
 * For details, see the {@link BurlapClientInterceptor} javadoc.
 *
 * @deprecated 从Spring 4.0开始, 由于Burlap几年没有进展 (与其兄弟Hessian形成鲜明对比)
 */
@Deprecated
public class BurlapProxyFactoryBean extends BurlapClientInterceptor implements FactoryBean<Object> {

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
