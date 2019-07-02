package org.springframework.mock.web.portlet;

import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.RenderRequest;
import javax.portlet.WindowState;

/**
 * {@link javax.portlet.RenderRequest}接口的模拟实现.
 */
public class MockRenderRequest extends MockPortletRequest implements RenderRequest {

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 */
	public MockRenderRequest() {
		super();
	}

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 * 
	 * @param portletMode portlet运行的模式
	 */
	public MockRenderRequest(PortletMode portletMode) {
		super();
		setPortletMode(portletMode);
	}

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 * 
	 * @param portletMode portlet运行的模式
	 * @param windowState 运行portlet的窗口状态
	 */
	public MockRenderRequest(PortletMode portletMode, WindowState windowState) {
		super();
		setPortletMode(portletMode);
		setWindowState(windowState);
	}

	/**
	 * 使用默认的{@link MockPortalContext}.
	 * 
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockRenderRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * @param portalContext 运行请求的PortletContext
	 * @param portletContext 运行请求的PortletContext
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
