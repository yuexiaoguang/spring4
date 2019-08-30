package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * 从{@link PortletConfig}对象读取init参数的{@link PropertySource}.
 */
public class PortletConfigPropertySource extends EnumerablePropertySource<PortletConfig> {

	public PortletConfigPropertySource(String name, PortletConfig portletConfig) {
		super(name, portletConfig);
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
