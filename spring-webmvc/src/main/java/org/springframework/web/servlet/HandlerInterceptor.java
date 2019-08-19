package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;

/**
 * 允许自定义处理器执行链的工作流接口.
 * 应用程序可以为某些处理器组注册任意数量的现有或自定义拦截器, 以添加常见的预处理行为, 而无需修改每个处理器实现.
 *
 * <p>在适当的HandlerAdapter触发处理器本身的执行之前调用HandlerInterceptor.
 * 该机制可用于大范围的预处理切面, e.g. 用于授权检查, 或常见的处理器行为, 如区域设置或主题更改.
 * 其主要目的是允许分解重复的处理器代码.
 *
 * <p>在异步处理场景中, 处理器可以在单独的线程中执行, 主线程退出而不渲染或调用{@code postHandle}和{@code afterCompletion}回调.
 * 并发处理器执行完成后, 将调度该请求以继续呈现模型, 并再次调用此约定的所有方法.
 * 有关更多选项和详细信息, 请参阅{@code org.springframework.web.servlet.AsyncHandlerInterceptor}
 *
 * <p>通常, 每个HandlerMapping bean定义一个拦截器链, 共享其粒度.
 * 为了能够将某个拦截器链应用于一组处理器, 需要通过一个HandlerMapping bean映射所需的处理器.
 * 拦截器本身在应用程序上下文中定义为bean, 由映射bean定义通过其"interceptors"属性引用 (在XML中: a &lt;list&gt; of &lt;ref&gt;).
 *
 * <p>HandlerInterceptor基本上类似于Servlet过滤器, 但与后者相反, 它只允许自定义预处理, 禁止执行处理程序本身, 以及允许自定义后处理.
 * 过滤器功能更强大, 例如, 它们允许交换传递链中的请求和响应对象.
 * 请注意, 过滤器在web.xml中配置, 应用程序上下文中的HandlerInterceptor.
 *
 * <p>作为基本准则, 细粒度处理器相关的预处理任务是HandlerInterceptor实现的候选者, 尤其是分解出来的常见处理器代码和授权检查.
 * 另一方面, 过滤器非常适合请求内容和视图内容处理, 如multipart表单和GZIP压缩.
 * 这通常表示何时需要将过滤器映射到某些内容类型 (e.g. 图像), 或所有请求.
 */
public interface HandlerInterceptor {

	/**
	 * 拦截处理器的执行.
	 * 在HandlerMapping确定适当的处理器对象之后调用, 但在HandlerAdapter调用处理器之前.
	 * <p>DispatcherServlet处理执行链中的处理器, 包含任意数量的拦截器, 最后是处理器本身.
	 * 使用此方法, 每个拦截器都可以决定中止执行链, 通常发送HTTP错误或写入自定义响应.
	 * <p><strong>Note:</strong> 特殊注意事项适用于异步请求处理.
	 * 有关更多详细信息, 请参阅{@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例评估
	 * 
	 * @return {@code true} 如果执行链应该继续下一个拦截器或处理器本身.
	 * 否则, DispatcherServlet假定这个拦截器已经处理了响应本身.
	 * @throws Exception
	 */
	boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception;

	/**
	 * 拦截处理器的执行.
	 * 在HandlerAdapter实际调用处理器之后, 但在DispatcherServlet呈现视图之前调用.
	 * 可以通过给定的ModelAndView将其他模型对象暴露给视图.
	 * <p>DispatcherServlet处理执行链中的处理器, 包含任意数量的拦截器, 最后是处理器本身.
	 * 使用此方法, 每个拦截器都可以对执行进行后处理, 并按执行链的逆序进行应用.
	 * <p><strong>Note:</strong> 特殊注意事项适用于异步请求处理.
	 * 有关更多详细信息, 请参阅{@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 启动异步执行的处理器 (或{@link HandlerMethod}), 用于类型和/或实例检查
	 * @param modelAndView 处理器返回的{@code ModelAndView} (也可以是{@code null})
	 * 
	 * @throws Exception
	 */
	void postHandle(
			HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
			throws Exception;

	/**
	 * 完成请求处理后回调, 即渲染视图后回调.
	 * 处理器返回执行的结果后调用, 从而允许适当的资源清理.
	 * <p>Note: 只有在此拦截器的{@code preHandle}方法成功完成并返回{@code true}时才会被调用!
	 * <p>与{@code postHandle}方法一样, 该方法将以相反的顺序在链中的每个拦截器上调用, 因此第一个拦截器将被最后一个调用.
	 * <p><strong>Note:</strong> 特殊注意事项适用于异步请求处理.
	 * 有关更多详细信息, 请参阅{@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 启动异步执行的处理器 (或{@link HandlerMethod}), 用于类型和/或实例检查
	 * @param ex 处理器执行时抛出的异常
	 * 
	 * @throws Exception
	 */
	void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception;

}
