package org.springframework.mock.web.portlet;

import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.RenderRequest;
import javax.portlet.WindowState;

/**
 * Mock implementation of the {@link javax.portlet.RenderRequest} interface.
 */
public class MockRenderRequest extends MockPortletRequest implements RenderRequest {

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 */
	public MockRenderRequest() {
		super();
	}

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param portletMode the mode that the portlet runs in
	 */
	public MockRenderRequest(PortletMode portletMode) {
		super();
		setPortletMode(portletMode);
	}

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param portletMode the mode that the portlet runs in
	 * @param windowState the window state to run the portlet in
	 */
	public MockRenderRequest(PortletMode portletMode, WindowState windowState) {
		super();
		setPortletMode(portletMode);
		setWindowState(windowState);
	}

	/**
	 * Create a new MockRenderRequest with a default {@link MockPortalContext}.
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockRenderRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * Create a new MockRenderRequest.
	 * @param portalContext the PortletContext that the request runs in
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockRenderRequest(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
	}


	@Override
	protected String getLifecyclePhase() {
		return RENDER_PHASE;
	}

	@Override
	public String getETag() {
		return getProperty(RenderRequest.ETAG);
	}

}
