package org.springframework.mock.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;

/**
 * Mock implementation of the {@link javax.portlet.ActionRequest} interface.
 */
public class MockActionRequest extends MockClientDataRequest implements ActionRequest {

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 */
	public MockActionRequest() {
		super();
	}

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param actionName the name of the action to trigger
	 */
	public MockActionRequest(String actionName) {
		super();
		setParameter(ActionRequest.ACTION_NAME, actionName);
	}

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param portletMode the mode that the portlet runs in
	 */
	public MockActionRequest(PortletMode portletMode) {
		super();
		setPortletMode(portletMode);
	}

	/**
	 * Create a new MockActionRequest with a default {@link MockPortalContext}.
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockActionRequest(PortletContext portletContext) {
		super(portletContext);
	}

	/**
	 * Create a new MockActionRequest.
	 * @param portalContext the PortalContext that the request runs in
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockActionRequest(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
	}


	@Override
	protected String getLifecyclePhase() {
		return ACTION_PHASE;
	}

}
