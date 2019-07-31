package org.springframework.web.context.request;

/**
 * 使用在异步请求处理期间调用的回调方法扩展{@code WebRequestInterceptor}.
 *
 * <p>当处理器启动异步请求处理时, DispatcherServlet退出时不会像通常那样调用{@code postHandle}和{@code afterCompletion},
 * 正如通常那样, 因为请求处理的结果 (e.g. ModelAndView)在当前线程中不可用并且处理尚未完成.
 * 在这种情况下, 调用{@link #afterConcurrentHandlingStarted(WebRequest)}方法,
 * 允许实现执行诸如清理线程绑定属性之类的任务.
 *
 * <p>异步处理完成后, 将请求分派给容器以进行进一步处理.
 * 在此阶段, DispatcherServlet像往常一样调用{@code preHandle}, {@code postHandle}和{@code afterCompletion}.
 */
public interface AsyncWebRequestInterceptor extends WebRequestInterceptor{

	/**
	 * 当处理器开始同时处理请求时调用, 而不是调用{@code postHandle}和{@code afterCompletion}.
	 *
	 * @param request 当前的请求
	 */
	void afterConcurrentHandlingStarted(WebRequest request);

}
