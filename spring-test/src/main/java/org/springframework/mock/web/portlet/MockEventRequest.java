package org.springframework.mock.web.portlet;

import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletContext;

/**
 * {@link javax.portlet.EventRequest}接口的模拟实现.
 */
public class MockEventRequest extends MockPortletRequest implements EventRequest {

	private final Event event;

	private String method;


	/**
	 * 使用默认的{@link MockPortalContext}和默认的{@link MockPortletContext}.
	 * 
	 * @param event 此请求包含的事件
	 */
	public MockEventRequest(Event event) {
		super();
		this.event = event;
	}

	/**
	 * 使用默认的{@link MockPortalContext}.
	 * 
	 * @param event 此请求包含的事件
	 * @param portletContext 运行请求的PortletContext
	 */
	public MockEventRequest(Event event, PortletContext portletContext) {
		super(portletContext);
		this.event = event;
	}

	/**
	 * @param event 此请求包含的事件
	 * @param portalContext 运行请求的PortletContext
	 * @param portletContext 运行请求的PortletContext
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
