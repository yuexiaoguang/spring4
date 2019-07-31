package org.springframework.web.context.request;

/**
 * 请求支持的{@link org.springframework.beans.factory.config.Scope}实现.
 *
 * <p>依赖于线程绑定的{@link RequestAttributes}实例,
 * 可以通过{@link RequestContextListener},
 * {@link org.springframework.web.filter.RequestContextFilter}
 * 或{@link org.springframework.web.servlet.DispatcherServlet}导出.
 *
 * <p>此{@code Scope}也适用于Portlet环境, 通过备用{@code RequestAttributes}实现
 * (通过Spring的{@link org.springframework.web.portlet.DispatcherPortlet}开箱即用).
 */
public class RequestScope extends AbstractRequestAttributesScope {

	@Override
	protected int getScope() {
		return RequestAttributes.SCOPE_REQUEST;
	}

	/**
	 * 请求没有会话ID概念, 因此此方法返回{@code null}.
	 */
	@Override
	public String getConversationId() {
		return null;
	}
}
