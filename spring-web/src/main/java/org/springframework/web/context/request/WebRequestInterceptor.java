package org.springframework.web.context.request;

import org.springframework.ui.ModelMap;

/**
 * 一般Web请求拦截的接口.
 * 通过构建{@link WebRequest}抽象, 允许应用于Servlet请求以及Portlet请求环境.
 *
 * <p>此接口假定MVC样式的请求处理: 处理器执行, 公开一组模型对象, 然后基于该模型呈现视图.
 * 或者, 处理器也可以完全处理请求, 而不会呈现视图.
 *
 * <p>在异步处理场景中, 处理器可以在单独的线程中执行,
 * 主线程退出时不渲染或调用{@code postHandle}和{@code afterCompletion}回调.
 * 并发处理器执行完成后, 将调度该请求以继续渲染模型, 并再次调用此合同的所有方法.
 * 有关更多选项和注释, 请参阅
 * {@code org.springframework.web.context.request.async.AsyncWebRequestInterceptor}
 *
 * <p>该接口是故意简约的, 以保持通用请求拦截器的依赖性尽可能小.
 *
 * <p><b>NOTE:</b> 虽然此拦截器应用于Servlet环境中的整个请求处理,
 * 但它默认仅应用于Portlet环境中的<i>render</i>阶段, 准备和呈现Portlet视图.
 * 要将WebRequestInterceptors应用于<i>action</i>阶段,
 * 将HandlerMapping的"applyWebRequestInterceptorsToRenderPhaseOnly"标志设置为"false".
 * 或者, 考虑使用特定于Portlet的HandlerInterceptor机制来满足此类需求.
 */
public interface WebRequestInterceptor {

	/**
	 * 在调用它<i>之前</i>, 拦截请求处理器的执行.
	 * <p>允许准备上下文资源 (例如Hibernate会话), 并将它们公开为请求属性或线程本地对象.
	 * 
	 * @param request 当前的web请求
	 * 
	 * @throws Exception
	 */
	void preHandle(WebRequest request) throws Exception;

	/**
	 * 在成功调用<i>之后</i>, 在视图渲染之前, 拦截请求处理器的执行.
	 * <p>允许在成功执行处理器后修改上下文资源 (例如, 刷新Hibernate会话).
	 * 
	 * @param request 当前的web请求
	 * @param model 将暴露给视图的模型对象的map (may be {@code null}).
	 * 如果需要, 可用于分析暴露的模型和/或添加其他模型属性.
	 * 
	 * @throws Exception
	 */
	void postHandle(WebRequest request, ModelMap model) throws Exception;

	/**
	 * 完成请求处理后回调, 即渲染视图后回调.
	 * 处理器输出结果后调用, 从而允许适当的资源清理.
	 * <p>Note: 只有在拦截器的{@code preHandle}方法成功完成时才会被调用!
	 * 
	 * @param request 当前的web请求
	 * @param ex 处理器执行时抛出的异常
	 * 
	 * @throws Exception
	 */
	void afterCompletion(WebRequest request, Exception ex) throws Exception;

}
