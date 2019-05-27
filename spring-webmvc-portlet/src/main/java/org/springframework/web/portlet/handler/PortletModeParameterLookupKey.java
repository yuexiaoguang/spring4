package org.springframework.web.portlet.handler;

import javax.portlet.PortletMode;

import org.springframework.util.ObjectUtils;

/**
 * Internal class used as lookup key, combining PortletMode and parameter value.
 */
class PortletModeParameterLookupKey {

	private final PortletMode mode;

	private final String parameter;


	public PortletModeParameterLookupKey(PortletMode portletMode, String parameter) {
		this.mode = portletMode;
		this.parameter = parameter;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PortletModeParameterLookupKey)) {
			return false;
		}
		PortletModeParameterLookupKey otherKey = (PortletModeParameterLookupKey) other;
		return (this.mode.equals(otherKey.mode) &&
				ObjectUtils.nullSafeEquals(this.parameter, otherKey.parameter));
	}

	@Override
	public int hashCode() {
		return (this.mode.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.parameter));
	}

	@Override
	public String toString() {
		return "Portlet mode '" + this.mode + "', parameter '" + this.parameter + "'";
	}

}
