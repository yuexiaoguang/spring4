package org.springframework.web.context.support;

import javax.servlet.ServletConfig;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource}, 从{@link ServletConfig}对象读取init参数.
 */
public class ServletConfigPropertySource extends EnumerablePropertySource<ServletConfig> {

	public ServletConfigPropertySource(String name, ServletConfig servletConfig) {
		super(name, servletConfig);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.getInitParameterNames());
	}

	@Override
	public String getProperty(String name) {
		return this.source.getInitParameter(name);
	}
}
