package org.springframework.beans.factory.serviceloader;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.springframework.beans.factory.BeanClassLoaderAware;

/**
 * 暴露所配置的服务类的所有服务的{@link org.springframework.beans.factory.FactoryBean}, 以服务对象的List形式表示,
 * 通过JDK 1.6 {@link java.util.ServiceLoader}工具获得.
 */
public class ServiceListFactoryBean extends AbstractServiceLoaderBasedFactoryBean implements BeanClassLoaderAware {

	@Override
	protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
		List<Object> result = new LinkedList<Object>();
		for (Object loaderObject : serviceLoader) {
			result.add(loaderObject);
		}
		return result;
	}

	@Override
	public Class<?> getObjectType() {
		return List.class;
	}

}
