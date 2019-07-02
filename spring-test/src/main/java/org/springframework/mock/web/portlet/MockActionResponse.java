package org.springframework.mock.web.portlet;

import java.io.IOException;
import java.util.Map;
import javax.portlet.ActionResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;

import org.springframework.util.Assert;

/**
 * {@link javax.portlet.ActionResponse}接口的模拟实现.
 */
public class MockActionResponse extends MockStateAwareResponse implements ActionResponse {

	private boolean redirectAllowed = true;

	private String redirectedUrl;


	/**
	 * 使用默认的{@link MockPortalContext}.
	 */
	public MockActionResponse() {
		super();
	}

	/**
	 * @param portalContext 定义支持的PortletMode和WindowState的PortalContext
	 */
	public MockActionResponse(PortalContext portalContext) {
		super(portalContext);
	}


	@Override
	public void setWindowState(WindowState windowState) throws WindowStateException {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set WindowState after sendRedirect has been called");
		}
		super.setWindowState(windowState);
		this.redirectAllowed = false;
	}

	@Override
	public void setPortletMode(PortletMode portletMode) throws PortletModeException {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set PortletMode after sendRedirect has been called");
		}
		super.setPortletMode(portletMode);
		this.redirectAllowed = false;
	}

	@Override
	public void setRenderParameters(Map<String, String[]> parameters) {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set render parameters after sendRedirect has been called");
		}
		super.setRenderParameters(parameters);
		this.redirectAllowed = false;
	}

	@Override
	public void setRenderParameter(String key, String value) {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set render parameters after sendRedirect has been called");
		}
		super.setRenderParameter(key, value);
		this.redirectAllowed = false;
	}

	@Override
	public void setRenderParameter(String key, String[] values) {
		if (this.redirectedUrl != null) {
			throw new IllegalStateException("Cannot set render parameters after sendRedirect has been called");
		}
		super.setRenderParameter(key, values);
		this.redirectAllowed = false;
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		if (!this.redirectAllowed) {
			throw new IllegalStateException(
					"Cannot call sendRedirect after windowState, portletMode, or renderParameters have been set");
		}
		Assert.notNull(location, "Redirect URL must not be null");
		this.redirectedUrl = location;
	}

	@Override
	public void sendRedirect(String location, String renderUrlParamName) throws IOException {
		sendRedirect(location);
		if (renderUrlParamName != null) {
			setRenderParameter(renderUrlParamName, location);
		}
	}

	public String getRedirectedUrl() {
		return this.redirectedUrl;
	}

}
