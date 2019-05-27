package org.springframework.beans.factory.serviceloader;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.springframework.beans.factory.BeanClassLoaderAware;

/**
 * 暴露已配置的服务类的“主要”服务的{@link org.springframework.beans.factory.FactoryBean},
 * 通过JDK 1.6 {@link java.util.ServiceLoader}工具获得.
 */
public class ServiceFactoryBean extends AbstractServiceLoaderBasedFactoryBean implements BeanClassLoaderAware {

	@Override
	protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
		Iterator<?> it = serviceLoader.iterator();
		if (!it.hasNext()) {
			throw new IllegalStateException(
					"ServiceLoader could not find service for type [" + getServiceType() + "]");
		}
		return it.next();
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceType();
	}

}
