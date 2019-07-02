package org.springframework.mock.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;

/**
 * {@link javax.portlet.ActionRequest}接口的模拟实现.
 */
public class MockActionRequest extends MockClientDataRequest implements ActionRequest {

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 */
	public MockActionRequest() {
		super();
	}

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 * 
	 * @param actionName 要触发的操作的名称
	 */
	public MockActionRequest(String actionName) {
		super();
		setParameter(ActionRequest.ACTION_NAME, actionName);
	}

	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 * 
	 * @param portletMode portlet运行的模式
	 */
	public MockActionRequest(PortletMode portletMode) {
		super();
		setPortletMode(portletMode);
	}

	/**
	 * 使用默认的{@link MockPortalContext}.
	 * 
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockActionRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * @param portalContext 运行请求的PortalContext
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockActionRequest(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
	}


	@Override
	protected String getLifecyclePhase() {
		return ACTION_PHASE;
	}

}
