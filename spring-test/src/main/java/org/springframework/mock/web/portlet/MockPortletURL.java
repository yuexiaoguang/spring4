package org.springframework.mock.web.portlet;

import java.util.Map;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletURL;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Mock implementation of the {@link javax.portlet.PortletURL} interface.
 */
public class MockPortletURL extends MockBaseURL implements PortletURL {

	public static final String URL_TYPE_RENDER = "render";

	public static final String URL_TYPE_ACTION = "action";


	private final PortalContext portalContext;

	private final String urlType;

	private WindowState windowState;

	private PortletMode portletMode;


	/**
	 * Create a new MockPortletURL for the given URL type.
	 * @param portalContext the PortalContext defining the supported
	 * PortletModes and WindowStates
	 * @param urlType the URL type, for example "render" or "action"
	 */
	public MockPortletURL(PortalContext portalContext, String urlType) {
		Assert.notNull(portalContext, "PortalContext is required");
		this.portalContext = portalContext;
		this.urlType = urlType;
	}


	//---------------------------------------------------------------------
	// PortletURL methods
	//---------------------------------------------------------------------

	@Override
	public void setWindowState(WindowState windowState) throws WindowStateException {
		if (!CollectionUtils.contains(this.portalContext.getSupportedWindowStates(), windowState)) {
			throw new WindowStateException("WindowState not supported", windowState);
		}
		this.windowState = windowState;
	}

	@Override
	public WindowState getWindowState() {
		return this.windowState;
	}

	@Override
	public void setPortletMode(PortletMode portletMode) throws PortletModeException {
		if (!CollectionUtils.contains(this.portalContext.getSupportedPortletModes(), portletMode)) {
			throw new PortletModeException("PortletMode not supported", portletMode);
		}
		this.portletMode = portletMode;
	}

	@Override
	public PortletMode getPortletMode() {
		return this.portletMode;
	}

	@Override
	public void removePublicRenderParameter(String name) {
		this.parameters.remove(name);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(encodeParameter("urlType", this.urlType));
		if (this.windowState != null) {
			sb.append(";").append(encodeParameter("windowState", this.windowState.toString()));
		}
		if (this.portletMode != null) {
			sb.append(";").append(encodeParameter("portletMode", this.portletMode.toString()));
		}
		for (Map.Entry<String, String[]> entry : this.parameters.entrySet()) {
			sb.append(";").append(encodeParameter("param_" + entry.getKey(), entry.getValue()));
		}
		return (isSecure() ? "https:" : "http:") +
				"//localhost/mockportlet?" + sb.toString();
	}

}
