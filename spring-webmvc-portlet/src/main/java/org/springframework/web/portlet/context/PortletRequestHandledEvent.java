package org.springframework.web.portlet.context;

import org.springframework.web.context.support.RequestHandledEvent;

/**
 * Portlet-specific subclass of RequestHandledEvent,
 * adding portlet-specific context information.
 */
@SuppressWarnings("serial")
public class PortletRequestHandledEvent extends RequestHandledEvent {

	/** Name of the portlet that handled the request */
	private final String portletName;

	/** PortletMode of the request */
	private final String portletMode;

	/** Type of Portlet Request */
	private final String requestType;


	/**
	 * Create a new PortletRequestHandledEvent.
	 * @param source the component that published the event
	 * @param portletName the name of the portlet that handled the request
	 * @param portletMode the PortletMode of the request (usually 'view', 'edit', or 'help')
	 * @param requestType the type of Portlet request ('action' or 'render')
	 * @param sessionId the id of the HTTP session, if any
	 * @param userName the name of the user that was associated with the
	 * request, if any (usually the UserPrincipal)
	 * @param processingTimeMillis the processing time of the request in milliseconds
	 */
	public PortletRequestHandledEvent(Object source, String portletName,
			String portletMode, String requestType, String sessionId,
			String userName, long processingTimeMillis) {

		super(source, sessionId, userName, processingTimeMillis);
		this.portletName = portletName;
		this.portletMode = portletMode;
		this.requestType = requestType;
	}

	/**
	 * Create a new PortletRequestHandledEvent.
	 * @param source the component that published the event
	 * @param portletName the name of the portlet that handled the request
	 * @param portletMode the PortletMode of the request (usually 'view', 'edit', or 'help')
	 * @param requestType the type of Portlet request ('action' or 'render')
	 * @param sessionId the id of the HTTP session, if any
	 * @param userName the name of the user that was associated with the
	 * request, if any (usually the UserPrincipal)
	 * @param processingTimeMillis the processing time of the request in milliseconds
	 * @param failureCause the cause of failure, if any
	 */
	public PortletRequestHandledEvent(Object source, String portletName,
			String portletMode, String requestType, String sessionId,
			String userName, long processingTimeMillis, Throwable failureCause) {

		super(source, sessionId, userName, processingTimeMillis, failureCause);
		this.portletName = portletName;
		this.portletMode = portletMode;
		this.requestType = requestType;
	}


	/**
	 * Return the name of the portlet that handled the request.
	 */
	public String getPortletName() {
		return this.portletName;
	}

	/**
	 * Return the mode of the portlet request (usually 'view', 'edit', or 'help').
	 */
	public String getPortletMode() {
		return this.portletMode;
	}

	/**
	 * Return the type of Portlet Request ('action' or 'render').
	 */
	public String getRequestType() {
		return this.requestType;
	}


	@Override
	public String getShortDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("portlet=[").append(this.portletName).append("]; ");
		sb.append(super.getShortDescription());
		return sb.toString();
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("portlet=[").append(this.portletName).append("]; ");
		sb.append("mode=[").append(this.portletMode).append("]; ");
		sb.append("type=[").append(this.requestType).append("]; ");
		sb.append(super.getDescription());
		return sb.toString();
	}

	@Override
	public String toString() {
		return "PortletRequestHandledEvent: " + getDescription();
	}

}
