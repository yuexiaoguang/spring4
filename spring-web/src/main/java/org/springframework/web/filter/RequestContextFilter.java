package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet过滤器, 将请求公开给当前线程, 通过
 * {@link org.springframework.context.i18n.LocaleContextHolder}和{@link RequestContextHolder}.
 * 要在{@code web.xml}中注册为过滤器.
 *
 * <p>或者, Spring的{@link org.springframework.web.context.request.RequestContextListener}
 * 和Spring的{@link org.springframework.web.servlet.DispatcherServlet}也将相同的请求上下文暴露给当前线程.
 *
 * <p>此过滤器主要用于第三方servlet, e.g. JSF FacesServlet.
 * 在Spring自己的Web支持中, DispatcherServlet的处理就足够了.
 */
public class RequestContextFilter extends OncePerRequestFilter {

	private boolean threadContextInheritable = false;


	/**
	 * 设置是否将LocaleContext 和 RequestAttributes公开为子线程可继承 (使用{@link java.lang.InheritableThreadLocal}).
	 * <p>默认"false", 以避免对衍生的后台线程产生副作用.
	 * 将其切换为"true"以启用在请求处理期间生成并仅用于此请求的自定义子线程的继承
	 * (也就是说, 在他们的初始任务结束后, 不重用线程).
	 * <p><b>WARNING:</b> 如果要访问配置为可能按需添加新线程的线程池 (e.g. JDK {@link java.util.concurrent.ThreadPoolExecutor}),
	 * 请不要对子线程使用继承, 因为这会将继承的上下文暴露给此类.
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}


	/**
	 * 返回"false", 以便过滤器可以在每个异步调度的线程中设置请求上下文.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回"false", 以便过滤器可以在错误调度中设置请求上下文.
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		ServletRequestAttributes attributes = new ServletRequestAttributes(request, response);
		initContextHolders(request, attributes);

		try {
			filterChain.doFilter(request, response);
		}
		finally {
			resetContextHolders();
			if (logger.isDebugEnabled()) {
				logger.debug("Cleared thread-bound request context: " + request);
			}
			attributes.requestCompleted();
		}
	}

	private void initContextHolders(HttpServletRequest request, ServletRequestAttributes requestAttributes) {
		LocaleContextHolder.setLocale(request.getLocale(), this.threadContextInheritable);
		RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		if (logger.isDebugEnabled()) {
			logger.debug("Bound request context to thread: " + request);
		}
	}

	private void resetContextHolders() {
		LocaleContextHolder.resetLocaleContext();
		RequestContextHolder.resetRequestAttributes();
	}

}
