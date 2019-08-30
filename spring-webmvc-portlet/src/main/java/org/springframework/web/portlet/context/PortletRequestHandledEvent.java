package org.springframework.web.portlet.context;

import org.springframework.web.context.support.RequestHandledEvent;

/**
 * RequestHandledEvent的特定于Portlet的子类, 添加特定于Portlet的上下文信息.
 */
@SuppressWarnings("serial")
public class PortletRequestHandledEvent extends RequestHandledEvent {

	/** 处理请求的portlet的名称 */
	private final String portletName;

	/** 请求的PortletMode */
	private final String portletMode;

	/** Portlet请求的类型 */
	private final String requestType;


	/**
	 * @param source 发布事件的组件
	 * @param portletName 处理请求的portlet的名称
	 * @param portletMode 请求的PortletMode (通常是'view', 'edit', 或'help')
	 * @param requestType Portlet请求的类型 ('action'或'render')
	 * @param sessionId HTTP会话的ID
	 * @param userName 与请求关联的用户的名称 (通常是 UserPrincipal)
	 * @param processingTimeMillis 请求的处理时间(以毫秒为单位)
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
	 * @param source 发布事件的组件
	 * @param portletName 处理请求的portlet的名称
	 * @param portletMode 请求的PortletMode (通常是'view', 'edit', 或'help')
	 * @param requestType Portlet请求的类型 ('action'或'render')
	 * @param sessionId HTTP会话的ID
	 * @param userName 与请求关联的用户的名称 (通常是 UserPrincipal)
	 * @param processingTimeMillis 请求的处理时间(以毫秒为单位)
	 * @param failureCause 失败的原因
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
	 * 返回处理请求的portlet的名称.
	 */
	public String getPortletName() {
		return this.portletName;
	}

	/**
	 * 返回portlet请求的模式 (通常是'view', 'edit', 或'help').
	 */
	public String getPortletMode() {
		return this.portletMode;
	}

	/**
	 * 返回Portlet请求的类型 ('action'或'render').
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
