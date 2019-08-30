package org.springframework.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * 允许自定义处理器执行链的工作流接口.
 * 应用程序可以为某些处理器组注册任意数量的现有或自定义拦截器, 以添加常见的预处理行为, 而无需修改每个处理器实现.
 *
 * <p>在适当的{@link HandlerAdapter}触发处理器本身的执行之前调用{@code HandlerInterceptor}.
 * 该机制可用于大范围的预处理切面, e.g. 用于授权检查, 或常见的处理器行为, 如区域设置或主题更改.
 * 其主要目的是允许分解其他重复的处理器代码.
 *
 * <p>通常, 每个{@link HandlerMapping} bean定义一个拦截器链, 共享其粒度.
 * 为了能够将某个拦截器链应用于一组处理器, 需要通过一个{@code HandlerMapping} bean映射所需的处理器.
 * 拦截器本身在应用程序上下文中定义为bean, 由映射bean定义通过其
 * {@link org.springframework.web.portlet.handler.AbstractHandlerMapping#setInterceptors "interceptors"}
 * 属性引用(在XML中: &lt;ref&gt; 元素的 &lt;list&gt;).
 *
 * <p>{@code HandlerInterceptor}基本上类似于Servlet {@link javax.servlet.Filter},
 * 但与后者相反, 它允许自定义预处理, 使用选项禁止执行处理器本身, 并自定义后处理.
 * {@code Filters}功能更强大; 例如, 它们允许交换传递链的请求和响应对象.
 * 请注意, 过滤器在{@code web.xml}中配置, {@code HandlerInterceptor}在应用程序上下文中配置.
 *
 * <p>作为基本准则, 与细粒度处理器相关的预处理任务是{@code HandlerInterceptor}实现的候选者,
 * 尤其是分解出来的常见处理器代码和授权检查.
 * 另一方面, {@code Filter}非常适合请求内容和视图内容处理, 如multipart表单和GZIP压缩.
 * 这通常表示何时需要将过滤器映射到某些内容类型 (e.g. images), 或所有请求.
 *
 * <p>请注意, 过滤器不能应用于portlet请求 (它们仅对servlet请求进行操作), 因此对于portlet请求, 拦截器是必不可少的.
 *
 * <p>如果假设一个"sunny day"请求周期 (i.e. 即一个没有出错且一切都很好的请求),
 * {@code HandlerInterceptor}的工作流程将如下:
 *
 * <p><b>操作请求:</b><p>
 * <ol>
 * <li>{@code DispatcherPortlet}将操作请求映射到特定的处理器, 并组装一个处理器执行链,
 * 该处理器执行链包含要调用的处理器和适用于请求的所有{@code HandlerInterceptor}实例.</li>
 * <li>调用{@link HandlerInterceptor#preHandleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object) preHandleAction(..)};
 * 如果此方法的调用返回{@code true}, 则此工作流继续.</li>
 * <li>目标处理器处理操作请求 (通过
 * {@link HandlerAdapter#handleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object) HandlerAdapter.handleAction(..)}).</li>
 * <li>调用{@link HandlerInterceptor#afterActionCompletion(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object, Exception)
 * afterActionCompletion(..)}.</li>
 * </ol>
 *
 * <p><b>渲染请求:</b><p>
 * <ol>
 * <li>{@code DispatcherPortlet}将渲染请求映射到特定处理器, 并组装处理器执行链,
 * 该处理器执行链包含要调用的处理器和适用于请求的所有{@code HandlerInterceptor}实例.</li>
 * <li>调用{@link HandlerInterceptor#preHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object) preHandleRender(..)};
 * 如果此方法的调用返回{@code true}, 则此工作流继续.</li>
 * <li>目标处理器处理渲染请求 (通过
 * {@link HandlerAdapter#handleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object) HandlerAdapter.handleRender(..)}).</li>
 * <li>调用{@link HandlerInterceptor#postHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object, ModelAndView)
 * postHandleRender(..)}.</li>
 * <li>如果{@code HandlerAdapter}返回{@code ModelAndView}, 则{@code DispatcherPortlet}会相应地渲染视图.
 * <li>调用{@link HandlerInterceptor#afterRenderCompletion(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object, Exception)
 * afterRenderCompletion(..)}.</li>
 * </ol>
 */
public interface HandlerInterceptor {

	/**
	 * 在操作阶段拦截处理器的执行.
	 * <p>在HandlerMapping确定适当的处理{@link ActionRequest}的处理器对象之后, 但在所述HandlerAdapter实际调用处理器之前调用.
	 * <p>{@link DispatcherPortlet}处理执行链中的处理器, 由任意数量的拦截器组成, 处理器本身在最后.
	 * 使用此方法, 每个拦截器都可以决定中止执行链, 通常会抛出异常或写入自定义响应.
	 * 
	 * @param request 当前的portlet操作请求
	 * @param response 当前的portlet操作响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例评估
	 * 
	 * @return {@code true} 如果执行链应该继续下一个拦截器或处理器本身.
	 * 否则, {@code DispatcherPortlet}假设这个拦截器已经处理了响应本身
	 * @throws Exception
	 */
	boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler)
			throws Exception;

	/**
	 * 在操作阶段完成请求处理后, 即渲染视图后回调.
	 * 将在处理器执行之后调用, 从而允许适当的资源清理.
	 * <p>Note: 只有在此拦截器的
	 * {@link #preHandleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object)}
	 * 方法成功完成并返回{@code true}时才会被调用!
	 * 
	 * @param request 当前的portlet操作请求
	 * @param response 当前的portlet操作响应
	 * @param handler 选择的要执行的处理器, 用于类型和/或实例检查
	 * @param ex 处理器执行时抛出的异常 (仅作为处理器抛出异常的情况的附加上下文信息包含在内;
	 * 即使此参数为{@code null}, 请求执行也可能失败)
	 * 
	 * @throws Exception
	 */
	void afterActionCompletion(ActionRequest request, ActionResponse response, Object handler, Exception ex)
			throws Exception;

	/**
	 * 在渲染阶段拦截处理器的执行.
	 * <p>在HandlerMapping确定适当的处理{@link RenderRequest}的处理器对象之后, 但在所述HandlerAdapter实际调用处理器之前调用.
	 * <p>{@link DispatcherPortlet}处理执行链中的处理器, 由任意数量的拦截器组成, 处理器本身在最后.
	 * 使用此方法, 每个拦截器都可以决定中止执行链, 通常会抛出异常或写入自定义响应.
	 * 
	 * @param request 当前portlet渲染请求
	 * @param response 当前portlet渲染响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例评估
	 * 
	 * @return {@code true} 如果执行链应该继续下一个拦截器或处理器本身.
	 * 否则, {@code DispatcherPortlet}假设这个拦截器已经处理了响应本身
	 * @throws Exception
	 */
	boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler)
			throws Exception;

	/**
	 * 在渲染阶段拦截处理器的执行.
	 * <p>在{@link HandlerAdapter}实际调用处理器之后, 但在{@code DispatcherPortlet}渲染视图之前调用.
	 * 因此可以通过给定的{@link ModelAndView}将其他模型对象暴露给视图.
	 * <p>{@code DispatcherPortlet}处理执行链中的处理器, 包含任意数量的拦截器, 最后处理器本身.
	 * 使用此方法, 每个拦截器都可以对执行进行后处理, 并按执行链的逆序进行应用.
	 * 
	 * @param request 当前portlet的渲染请求
	 * @param response 当前portlet的渲染响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例检查
	 * @param modelAndView 处理器返回的{@code ModelAndView} (也可以是{@code null})
	 * 
	 * @throws Exception
	 */
	void postHandleRender(RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView)
			throws Exception;

	/**
	 * 完成请求处理后回调, 即渲染视图后回调.
	 * 将在处理器执行后调用, 从而允许适当的资源清理.
	 * <p>Note: 只有在此拦截器的
	 * {@link #preHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object)}
	 * 方法成功完成并返回{@code true}时才会被调用!
	 * 
	 * @param request 当前portlet的渲染请求
	 * @param response 当前portlet的渲染响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例检查
	 * @param ex 处理器执行时抛出的异常
	 * 
	 * @throws Exception
	 */
	void afterRenderCompletion(RenderRequest request, RenderResponse response, Object handler, Exception ex)
			throws Exception;

	/**
	 * 在渲染阶段拦截处理器的执行.
	 * <p>在HandlerMapping确定适当的处理{@link RenderRequest}的处理器对象之后, 但在所述HandlerAdapter实际调用处理器之前调用.
	 * <p>{@link DispatcherPortlet}处理执行链中的处理器, 由任意数量的拦截器组成, 处理器本身在最后.
	 * 使用此方法, 每个拦截器都可以决定中止执行链, 通常会抛出异常或写入自定义响应.
	 * 
	 * @param request 当前portlet的渲染请求
	 * @param response 当前portlet的渲染响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例评估
	 * 
	 * @return {@code true} 如果执行链应该继续下一个拦截器或处理器本身.
	 * 否则, {@code DispatcherPortlet}假设这个拦截器已经处理了响应本身
	 * @throws Exception
	 */
	boolean preHandleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception;

	/**
	 * 在渲染阶段拦截处理器的执行.
	 * <p>在{@link HandlerAdapter}实际调用处理器之后, 但在{@code DispatcherPortlet}呈现视图之前调用.
	 * 因此可以通过给定的{@link ModelAndView}将其他模型对象暴露给视图.
	 * <p>{@code DispatcherPortlet}处理执行链中的处理器, 包含任意数量的拦截器, 最后处理器本身.
	 * 使用此方法, 每个拦截器都可以进行后处理, 并按执行链的逆序进行应用.
	 * 
	 * @param request 当前portlet的渲染请求
	 * @param response 当前portlet的渲染响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例检查
	 * @param modelAndView 处理器返回的{@code ModelAndView} (也可以是{@code null})
	 * 
	 * @throws Exception
	 */
	void postHandleResource(ResourceRequest request, ResourceResponse response, Object handler, ModelAndView modelAndView)
			throws Exception;

	/**
	 * 完成请求处理后回调, 即渲染视图后回调.
	 * 将在处理器执行后调用, 从而允许适当的资源清理.
	 * <p>Note: 只有在此拦截器的
	 * {@link #preHandleRender(javax.portlet.RenderRequest, javax.portlet.RenderResponse, Object)}
	 * 方法成功完成并返回{@code true}时才会被调用!
	 * 
	 * @param request 当前portlet的渲染请求
	 * @param response 当前portlet的渲染响应
	 * @param handler 选择的要执行的处理器, 用于类型和/或实例检查
	 * @param ex 处理器执行时抛出的异常
	 * 
	 * @throws Exception
	 */
	void afterResourceCompletion(ResourceRequest request, ResourceResponse response, Object handler, Exception ex)
			throws Exception;

	/**
	 * 在操作阶段拦截处理器的执行.
	 * <p>在HandlerMapping确定适当的处理{@link ActionRequest}的处理器对象之后, 但在所述HandlerAdapter实际调用处理器之前调用.
	 * <p>{@link DispatcherPortlet}处理执行链中的处理器, 由任意数量的拦截器组成, 处理器本身在最后.
	 * 使用此方法, 每个拦截器都可以决定中止执行链, 通常会抛出异常或写入自定义响应.
	 * 
	 * @param request 当前的portlet操作请求
	 * @param response 当前的portlet操作响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例评估
	 * 
	 * @return {@code true} 如果执行链应该继续下一个拦截器或处理器本身.
	 * 否则, {@code DispatcherPortlet}假设这个拦截器已经处理了响应本身
	 * @throws Exception
	 */
	boolean preHandleEvent(EventRequest request, EventResponse response, Object handler)
			throws Exception;

	/**
	 * 在操作阶段完成请求处理后, 即渲染视图后回调.
	 * 将在处理器执行之后调用, 从而允许适当的资源清理.
	 * <p>Note: 只有在此拦截器的
	 * {@link #preHandleAction(javax.portlet.ActionRequest, javax.portlet.ActionResponse, Object)}
	 * 方法成功完成并返回{@code true}时才会被调用!
	 * 
	 * @param request 当前的portlet操作请求
	 * @param response 当前的portlet操作响应
	 * @param handler 选择要执行的处理器, 用于类型和/或实例检查
	 * @param ex 处理器执行时抛出的异常 (仅作为处理器引发异常的情况的附加上下文信息包含在内;
	 * 即使此参数为{@code null}, 请求执行也可能失败)
	 * 
	 * @throws Exception
	 */
	void afterEventCompletion(EventRequest request, EventResponse response, Object handler, Exception ex)
			throws Exception;

}
