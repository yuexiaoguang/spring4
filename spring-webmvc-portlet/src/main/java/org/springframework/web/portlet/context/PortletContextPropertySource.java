package org.springframework.web.portlet.context;

import javax.portlet.PortletContext;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * 从{@link PortletContext}对象读取init参数的{@link PropertySource}.
 */
public class PortletContextPropertySource extends EnumerablePropertySource<PortletContext> {

	public PortletContextPropertySource(String name, PortletContext portletContext) {
		super(name, portletContext);
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
