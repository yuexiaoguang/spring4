package org.springframework.web.context.support;

/**
 * RequestHandledEvent的特定于Servlet的子类, 添加特定于servlet的上下文信息.
 */
@SuppressWarnings("serial")
public class ServletRequestHandledEvent extends RequestHandledEvent {

	/** 触发请求的URL */
	private final String requestUrl;

	/** 请求来自的IP地址 */
	private final String clientAddress;

	/** 通常是GET 或 POST */
	private final String method;

	/** 处理请求的servlet的名称 */
	private final String servletName;

	/** 响应的HTTP状态码 */
	private final int statusCode;


	/**
	 * @param source 发布事件的组件
	 * @param requestUrl 请求的URL
	 * @param clientAddress 请求来自的IP地址
	 * @param method 请求的HTTP方法 (通常是GET或POST)
	 * @param servletName 处理请求的servlet的名称
	 * @param sessionId HTTP会话的ID
	 * @param userName 与请求关联的用户的名称 (通常是UserPrincipal)
	 * @param processingTimeMillis 请求的处理时间(以毫秒为单位)
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
			String clientAddress, String method, String servletName,
			String sessionId, String userName, long processingTimeMillis) {

		super(source, sessionId, userName, processingTimeMillis);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = -1;
	}

	/**
	 * @param source 发布事件的组件
	 * @param requestUrl 请求的URL
	 * @param clientAddress 请求来自的IP地址
	 * @param method 请求的HTTP方法 (通常是GET或POST)
	 * @param servletName 处理请求的servlet的名称
	 * @param sessionId HTTP会话的ID
	 * @param userName 与请求关联的用户的名称 (通常是UserPrincipal)
	 * @param processingTimeMillis 请求的处理时间(以毫秒为单位)
	 * @param failureCause 失败的原因
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
			String clientAddress, String method, String servletName, String sessionId,
			String userName, long processingTimeMillis, Throwable failureCause) {

		super(source, sessionId, userName, processingTimeMillis, failureCause);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = -1;
	}

	/**
	 * @param source 发布事件的组件
	 * @param requestUrl 请求的URL
	 * @param clientAddress 请求来自的IP地址
	 * @param method 请求的HTTP方法 (通常是GET或POST)
	 * @param servletName 处理请求的servlet的名称
	 * @param sessionId HTTP会话的ID
	 * @param userName 与请求关联的用户的名称 (通常是UserPrincipal)
	 * @param processingTimeMillis 请求的处理时间(以毫秒为单位)
	 * @param failureCause 失败的原因
	 * @param statusCode 响应的HTTP状态码
	 */
	public ServletRequestHandledEvent(Object source, String requestUrl,
			String clientAddress, String method, String servletName, String sessionId,
			String userName, long processingTimeMillis, Throwable failureCause, int statusCode) {

		super(source, sessionId, userName, processingTimeMillis, failureCause);
		this.requestUrl = requestUrl;
		this.clientAddress = clientAddress;
		this.method = method;
		this.servletName = servletName;
		this.statusCode = statusCode;
	}


	/**
	 * 返回请求的URL.
	 */
	public String getRequestUrl() {
		return this.requestUrl;
	}

	/**
	 * 返回请求来自的IP地址.
	 */
	public String getClientAddress() {
		return this.clientAddress;
	}

	/**
	 * 返回请求的HTTP方法 (通常是GET或POST).
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * 返回处理请求的servlet的名称.
	 */
	public String getServletName() {
		return this.servletName;
	}

	/**
	 * 返回响应的HTTP状态码, 或-1 如果状态代码不可用.
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public String getShortDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("url=[").append(getRequestUrl()).append("]; ");
		sb.append("client=[").append(getClientAddress()).append("]; ");
		sb.append(super.getShortDescription());
		return sb.toString();
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("url=[").append(getRequestUrl()).append("]; ");
		sb.append("client=[").append(getClientAddress()).append("]; ");
		sb.append("method=[").append(getMethod()).append("]; ");
		sb.append("servlet=[").append(getServletName()).append("]; ");
		sb.append(super.getDescription());
		return sb.toString();
	}

	@Override
	public String toString() {
		return "ServletRequestHandledEvent: " + getDescription();
	}

}
