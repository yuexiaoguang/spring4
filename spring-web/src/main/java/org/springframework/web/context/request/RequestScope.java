package org.springframework.web.context.request;

/**
 * Request-backed {@link org.springframework.beans.factory.config.Scope}
 * implementation.
 *
 * <p>Relies on a thread-bound {@link RequestAttributes} instance, which
 * can be exported through {@link RequestContextListener},
 * {@link org.springframework.web.filter.RequestContextFilter} or
 * {@link org.springframework.web.servlet.DispatcherServlet}.
 *
 * <p>This {@code Scope} will also work for Portlet environments,
 * through an alternate {@code RequestAttributes} implementation
 * (as exposed out-of-the-box by Spring's
 * {@link org.springframework.web.portlet.DispatcherPortlet}.
 */
public class RequestScope extends AbstractRequestAttributesScope {

	@Override
	protected int getScope() {
		return RequestAttributes.SCOPE_REQUEST;
	}

	/**
	 * There is no conversation id concept for a request, so this method
	 * returns {@code null}.
	 */
	@Override
	public String getConversationId() {
		return null;
	}

}
