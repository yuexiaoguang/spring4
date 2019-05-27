package org.springframework.mock.web.portlet;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;

/**
 * Mock implementation of the {@link javax.portlet.EventRequest} interface.
 */
public class MockEventRequest extends MockPortletRequest implements EventRequest {

	private final Event event;

	private String method;


	/**
	 * Create a new MockEventRequest with a default {@link MockPortalContext}
	 * and a default {@link MockPortletContext}.
	 * @param event the event that this request wraps
	 */
	public MockEventRequest(Event event) {
		super();
		this.event = event;
	}

	/**
	 * Create a new MockEventRequest with a default {@link MockPortalContext}.
	 * @param event the event that this request wraps
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockEventRequest(Event event, PortletContext portletContext) {
		super(portletContext);
		this.event = event;
	}

	/**
	 * Create a new MockEventRequest.
	 * @param event the event that this request wraps
	 * @param portalContext the PortletContext that the request runs in
	 * @param portletContext the PortletContext that the request runs in
	 */
	public MockEventRequest(Event event, PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
		this.event = event;
	}


	@Override
	protected String getLifecyclePhase() {
		return EVENT_PHASE;
	}

	@Override
	public Event getEvent() {
		return this.event;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	@Override
	public String getMethod() {
		return this.method;
	}

}
