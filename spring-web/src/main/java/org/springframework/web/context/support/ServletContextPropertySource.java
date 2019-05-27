package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} that reads init parameters from a {@link ServletContext} object.
 */
public class ServletContextPropertySource extends EnumerablePropertySource<ServletContext> {

	public ServletContextPropertySource(String name, ServletContext servletContext) {
		super(name, servletContext);
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
