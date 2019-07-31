package org.springframework.remoting.jaxws;

import javax.xml.ws.Service;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 用于本地定义的JAX-WS服务引用的{@link org.springframework.beans.factory.FactoryBean}.
 * 使用下面的{@link LocalJaxWsServiceFactory}设施.
 *
 * <p>或者, 可以在J2EE容器的JNDI环境中查找JAX-WS服务引用.
 */
public class LocalJaxWsServiceFactoryBean extends LocalJaxWsServiceFactory
		implements FactoryBean<Service>, InitializingBean {

	private Service service;


	@Override
	public void afterPropertiesSet() {
		this.service = createJaxWsService();
	}

	@Override
	public Service getObject() {
		return this.service;
	}

	@Override
	public Class<? extends Service> getObjectType() {
		return (this.service != null ? this.service.getClass() : Service.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
