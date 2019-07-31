package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.util.WebUtils;

/**
 * 过滤器基类, 旨在保证在任何servlet容器上每次请求调度执行一次.
 * 它提供了一个带有HttpServletRequest和HttpServletResponse参数的{@link #doFilterInternal}方法.
 *
 * <p>从Servlet 3.0开始, 可以调用过滤器作为在单独线程中发生的
 * {@link javax.servlet.DispatcherType#REQUEST REQUEST}
 * 或{@link javax.servlet.DispatcherType#ASYNC ASYNC}调度的一部分.
 * 可以在{@code web.xml}中配置过滤器是否应该参与异步调度.
 * 但是, 在某些情况下, servlet容器采用不同的默认配置.
 * 因此, 子类可以覆盖方法{@link #shouldNotFilterAsyncDispatch()},
 * 以便在两种类型的调度期间静态地声明它们是否应该被调用<em>一次</em>,
 * 以便提供线程初始化, 日志记录, 安全性.
 * 此机制补充并不替代在{@code web.xml}中使用调度程序类型配置过滤器的需要.
 *
 * <p>子类可以使用{@link #isAsyncDispatch(HttpServletRequest)}来确定何时调用过滤器作为异步调度的一部分,
 * 并使用{@link #isAsyncStarted(HttpServletRequest)}来确定请求何时被置于异步模式, 因此当前的调度将不是给定请求的最后一个.
 *
 * <p>另一个也出现在自己的线程中的调度类型是{@link javax.servlet.DispatcherType#ERROR ERROR}.
 * 子类可以覆盖{@link #shouldNotFilterErrorDispatch()}, 如果他们希望静态声明是否应该在错误发送期间调用<em>一次<em>.
 *
 * <p>{@link #getAlreadyFilteredAttributeName}方法确定如何识别请求已被过滤.
 * 默认实现基于具体过滤器实例的配置名称.
 */
public abstract class OncePerRequestFilter extends GenericFilterBean {

	/**
	 * 附加到"已过滤"请求属性的过滤器名称的后缀.
	 */
	public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";


	/**
	 * 这个{@code doFilter}实现存储了"已过滤"的请求属性, 如果该属性已经存在, 则不再进行过滤.
	 */
	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("OncePerRequestFilter just supports HTTP requests");
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
		boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;

		if (hasAlreadyFilteredAttribute || skipDispatch(httpRequest) || shouldNotFilter(httpRequest)) {

			// 继续操作而不调用此过滤器...
			filterChain.doFilter(request, response);
		}
		else {
			// 调用此过滤器...
			request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
			try {
				doFilterInternal(httpRequest, httpResponse, filterChain);
			}
			finally {
				// 删除此请求的"已过滤"请求属性.
				request.removeAttribute(alreadyFilteredAttributeName);
			}
		}
	}


	private boolean skipDispatch(HttpServletRequest request) {
		if (isAsyncDispatch(request) && shouldNotFilterAsyncDispatch()) {
			return true;
		}
		if (request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null && shouldNotFilterErrorDispatch()) {
			return true;
		}
		return false;
	}

	/**
	 * Servlet 3.0中引入的调度程序类型{@code javax.servlet.DispatcherType.ASYNC},
	 * 意味着可以在单个请求的过程中在多个线程中调用过滤器.
	 * 如果过滤器当前正在异步调度中执行, 则此方法返回{@code true}.
	 * 
	 * @param request 当前的请求
	 */
	protected boolean isAsyncDispatch(HttpServletRequest request) {
		return WebAsyncUtils.getAsyncManager(request).hasConcurrentResult();
	}

	/**
	 * 请求处理是否处于异步模式, 这意味着在退出当前线程后不会提交响应.
	 * 
	 * @param request 当前的请求
	 */
	protected boolean isAsyncStarted(HttpServletRequest request) {
		return WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted();
	}

	/**
	 * 返回标识已过滤请求的请求属性的名称.
	 * <p>默认实现采用具体过滤器实例的配置名称并附加".FILTERED".
	 * 如果过滤器未完全初始化, 则会回退到其类名.
	 */
	protected String getAlreadyFilteredAttributeName() {
		String name = getFilterName();
		if (name == null) {
			name = getClass().getName();
		}
		return name + ALREADY_FILTERED_SUFFIX;
	}

	/**
	 * 可以在子类中重写以进行自定义过滤控制, 返回{@code true}以避免过滤给定的请求.
	 * <p>默认实现始终返回{@code false}.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 是否应该<i>不</i>过滤给定的请求
	 * @throws ServletException
	 */
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return false;
	}

	/**
	 * Servlet 3.0中引入的调度程序类型{@code javax.servlet.DispatcherType.ASYNC},
	 * 意味着可以在单个请求的过程中在多个线程中调用过滤器.
	 * 有些过滤器只需要过滤初始线程 (e.g. 请求包装), 而其他过滤器可能需要在每个附加线程中至少调用一次,
	 * 例如用于设置线程本地或在最后执行最终处理.
	 * <p>请注意，尽管可以映射过滤器以通过{@code web.xml}或Java中的{@code ServletContext}处理特定的调度器类型,
	 * 但servlet容器可能会针对调度器类型强制使用不同的默认值.
	 * 此标志强制过滤器的设计意图.
	 * <p>默认返回值为"true", 这意味着在后续异步调度期间不会调用过滤器.
	 * 如果为"false", 则在异步调度期间, 保证在单个线程中的请求期间仅调用一次相同的过滤器.
	 */
	protected boolean shouldNotFilterAsyncDispatch() {
		return true;
	}

	/**
	 * 是否过滤错误调度, 例如servlet容器处理和{@code web.xml}中映射的错误.
	 * 默认返回值为"true", 这意味着在发生错误分发时不会调用过滤器.
	 */
	protected boolean shouldNotFilterErrorDispatch() {
		return true;
	}


	/**
	 * 与{@code doFilter}相同的约定, 但保证在单个请求线程中, 每个请求只调用一次.
	 * See {@link #shouldNotFilterAsyncDispatch()} for details.
	 * <p>提供HttpServletRequest和HttpServletResponse参数, 而不是默认的ServletRequest和ServletResponse参数.
	 */
	protected abstract void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException;

}
