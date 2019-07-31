package org.springframework.web.context.support;

import org.springframework.context.ApplicationEvent;

/**
 * 在ApplicationContext中处理请求时引发的事件.
 *
 * <p>由Spring自己的FrameworkServlet支持 (通过特定的ServletRequestHandledEvent子类), 但也可以由任何其他Web组件引发.
 * 例如, 由Spring的开箱即用的PerformanceMonitorListener使用.
 */
@SuppressWarnings("serial")
public class RequestHandledEvent extends ApplicationEvent {

	/** 应用于请求的会话ID */
	private String sessionId;

	/** 通常是UserPrincipal */
	private String userName;

	/** 请求处理时间 */
	private final long processingTimeMillis;

	/** 失败的原因 */
	private Throwable failureCause;


	/**
	 * @param source 发布事件的组件
	 * @param sessionId HTTP会话的ID
	 * @param userName 与请求关联的用户的名称 (通常是 UserPrincipal)
	 * @param processingTimeMillis 请求的处理时间, 以毫秒为单位
	 */
	public RequestHandledEvent(Object source, String sessionId, String userName, long processingTimeMillis) {
		super(source);
		this.sessionId = sessionId;
		this.userName = userName;
		this.processingTimeMillis = processingTimeMillis;
	}

	/**
	 * @param source 发布事件的组件
	 * @param sessionId HTTP会话的ID
	 * @param userName 与请求关联的用户的名称 (通常是 UserPrincipal)
	 * @param processingTimeMillis 请求的处理时间, 以毫秒为单位
	 * @param failureCause 失败的原因
	 */
	public RequestHandledEvent(
			Object source, String sessionId, String userName, long processingTimeMillis, Throwable failureCause) {

		this(source, sessionId, userName, processingTimeMillis);
		this.failureCause = failureCause;
	}


	/**
	 * 返回请求的处理时间, 以毫秒为单位.
	 */
	public long getProcessingTimeMillis() {
		return this.processingTimeMillis;
	}

	/**
	 * 返回HTTP会话的ID.
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * 返回与请求关联的用户的名称 (通常是UserPrincipal).
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * 返回请求是否失败.
	 */
	public boolean wasFailure() {
		return (this.failureCause != null);
	}

	/**
	 * 返回失败的原因.
	 */
	public Throwable getFailureCause() {
		return this.failureCause;
	}


	/**
	 * 返回此事件的简短描述, 仅涉及最重要的上下文数据.
	 */
	public String getShortDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("session=[").append(this.sessionId).append("]; ");
		sb.append("user=[").append(this.userName).append("]; ");
		return sb.toString();
	}

	/**
	 * 返回此事件的完整描述, 涉及所有可用的上下文数据.
	 */
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("session=[").append(this.sessionId).append("]; ");
		sb.append("user=[").append(this.userName).append("]; ");
		sb.append("time=[").append(this.processingTimeMillis).append("ms]; ");
		sb.append("status=[");
		if (!wasFailure()) {
			sb.append("OK");
		}
		else {
			sb.append("failed: ").append(this.failureCause);
		}
		sb.append(']');
		return sb.toString();
	}

	@Override
	public String toString() {
		return ("RequestHandledEvent: " + getDescription());
	}

}
