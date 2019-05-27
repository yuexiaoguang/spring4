package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} that reads init parameters from a {@link PortletConfig} object.
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
