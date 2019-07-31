package org.springframework.web.context.request;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Servlet监听器, 将请求公开给当前线程,
 * 通过{@link org.springframework.context.i18n.LocaleContextHolder}和{@link RequestContextHolder}.
 * 要在{@code web.xml}中注册为监听器.
 *
 * <p>或者, Spring的{@link org.springframework.web.filter.RequestContextFilter}
 * 和Spring的{@link org.springframework.web.servlet.DispatcherServlet}也将相同的请求上下文暴露给当前线程.
 * 与此监听器相比, 那里提供了高级选项 (e.g. "threadContextInheritable").
 *
 * <p>此监听器主要用于第三方servlet, e.g. the JSF FacesServlet.
 * 在Spring自己的Web支持中, DispatcherServlet的处理就足够了.
 */
public class RequestContextListener implements ServletRequestListener {

	private static final String REQUEST_ATTRIBUTES_ATTRIBUTE =
			RequestContextListener.class.getName() + ".REQUEST_ATTRIBUTES";


	@Override
	public void requestInitialized(ServletRequestEvent requestEvent) {
		if (!(requestEvent.getServletRequest() instanceof HttpServletRequest)) {
			throw new IllegalArgumentException(
					"Request is not an HttpServletRequest: " + requestEvent.getServletRequest());
		}
		HttpServletRequest request = (HttpServletRequest) requestEvent.getServletRequest();
		ServletRequestAttributes attributes = new ServletRequestAttributes(request);
		request.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE, attributes);
		LocaleContextHolder.setLocale(request.getLocale());
		RequestContextHolder.setRequestAttributes(attributes);
	}

	@Override
	public void requestDestroyed(ServletRequestEvent requestEvent) {
		ServletRequestAttributes attributes = null;
		Object reqAttr = requestEvent.getServletRequest().getAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE);
		if (reqAttr instanceof ServletRequestAttributes) {
			attributes = (ServletRequestAttributes) reqAttr;
		}
		RequestAttributes threadAttributes = RequestContextHolder.getRequestAttributes();
		if (threadAttributes != null) {
			// 完全可以在原始请求线程中...
			LocaleContextHolder.resetLocaleContext();
			RequestContextHolder.resetRequestAttributes();
			if (attributes == null && threadAttributes instanceof ServletRequestAttributes) {
				attributes = (ServletRequestAttributes) threadAttributes;
			}
		}
		if (attributes != null) {
			attributes.requestCompleted();
		}
	}
}
