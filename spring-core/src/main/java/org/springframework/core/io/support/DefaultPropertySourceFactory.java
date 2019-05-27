package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.env.PropertySource;

/**
 * {@link PropertySourceFactory}的默认实现, 包装{@link ResourcePropertySource}中的每个资源.
 */
public class DefaultPropertySourceFactory implements PropertySourceFactory {

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
		return (name != null ? new ResourcePropertySource(name, resource) : new ResourcePropertySource(resource));
	}

}
