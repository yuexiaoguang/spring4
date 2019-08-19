package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;

/**
 * 扩展{@code HandlerInterceptor}, 使用在异步请求处理开始后调用的回调方法.
 *
 * <p>当处理器启动异步请求时, {@link DispatcherServlet}退出时不会像通常对同步请求那样
 * 调用{@code postHandle}和{@code afterCompletion},
 * 因为请求处理的结果 (e.g. ModelAndView)还没有准备就绪, 将从另一个线程并发生成.
 * 在这种情况下, 调用{@link #afterConcurrentHandlingStarted}, 允许实现执行任务,
 * 例如在将线程释放到Servlet容器之前清理线程绑定属性.
 *
 * <p>异步处理完成后, 将请求分派给容器以进行进一步处理.
 * 在此阶段, {@code DispatcherServlet}调用{@code preHandle}, {@code postHandle}, 和{@code afterCompletion}.
 * 为了区分异步处理完成后的初始请求和后续调度, 拦截器可以检查{@link javax.servlet.ServletRequest}的
 * {@code javax.servlet.DispatcherType}是{@code "REQUEST"}还是{@code "ASYNC"}.
 *
 * <p>请注意, 当异步请求超时或因网络错误而完成时, {@code HandlerInterceptor}实现可能需要工作.
 * 对于这种情况, Servlet容器不会调度, 因此不会调用{@code postHandle}和{@code afterCompletion}方法.
 * 相反, 拦截器可以通过{@link org.springframework.web.context.request.async.WebAsyncManager WebAsyncManager}
 * 上的{@code registerCallbackInterceptor}和{@code registerDeferredResultInterceptor}方法注册以跟踪异步请求.
 * 无论异步请求处理是否开始, 这都可以在{@code preHandle}的每个请求上主动完成.
 */
public interface AsyncHandlerInterceptor extends HandlerInterceptor {

	/**
	 * 当一个处理程序并发执行时调用, 而不是{@code postHandle}和{@code afterCompletion}.
	 * <p>实现可以使用提供的请求和响应, 但应避免以与处理器的并发执行冲突的方式修改它们.
	 * 此方法的典型用法是清理线程局部变量.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param handler 启动异步执行的处理器 (或{@link HandlerMethod}), 用于类型和/或实例检查
	 * 
	 * @throws Exception
	 */
	void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception;

}
