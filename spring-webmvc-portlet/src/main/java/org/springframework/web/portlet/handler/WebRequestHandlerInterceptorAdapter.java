package org.springframework.web.portlet.handler;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.portlet.HandlerInterceptor;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletWebRequest;

/**
 * 现Portlet HandlerInterceptor接口并包装底层WebRequestInterceptor的适配器.
 *
 * <p><b>NOTE:</b> 默认情况下, WebRequestInterceptor仅应用于Portlet <b>渲染</b>阶段, 该阶段准备和渲染Portlet视图.
 * 如果{@code renderPhaseOnly}标志显式设置为{@code false}, 则只会使用WebRequestInterceptor调用拦截Portlet操作阶段.
 * 通常, 建议使用特定于Portlet的HandlerInterceptor机制来区分操作和渲染拦截.
 */
public class WebRequestHandlerInterceptorAdapter implements HandlerInterceptor {

	private final WebRequestInterceptor requestInterceptor;

	private final boolean renderPhaseOnly;


	/**
	 * 仅适用于渲染阶段.
	 * 
	 * @param requestInterceptor 要包装的WebRequestInterceptor
	 */
	public WebRequestHandlerInterceptorAdapter(WebRequestInterceptor requestInterceptor) {
		this(requestInterceptor, true);
	}

	/**
	 * @param requestInterceptor 要包装的WebRequestInterceptor
	 * @param renderPhaseOnly 是否仅应用于渲染阶段 ({@code true}) 或操作阶段 ({@code false})
	 */
	public WebRequestHandlerInterceptorAdapter(WebRequestInterceptor requestInterceptor, boolean renderPhaseOnly) {
		Assert.notNull(requestInterceptor, "WebRequestInterceptor must not be null");
		this.requestInterceptor = requestInterceptor;
		this.renderPhaseOnly = renderPhaseOnly;
	}


	@Override
	public boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception {
		if (!this.renderPhaseOnly) {
			this.requestInterceptor.preHandle(new PortletWebRequest(request));
		}
		return true;
	}

	@Override
	public void afterActionCompletion(
			ActionRequest request, ActionResponse response, Object handler, Exception ex) throws Exception {

		if (!this.renderPhaseOnly) {
			this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
		}
	}

	@Override
	public boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception {
		this.requestInterceptor.preHandle(new PortletWebRequest(request));
		return true;
	}

	@Override
	public void postHandleRender(
			RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView) throws Exception {

		this.requestInterceptor.postHandle(new PortletWebRequest(request),
				(modelAndView != null && !modelAndView.wasCleared() ? modelAndView.getModelMap() : null));
	}

	@Override
	public void afterRenderCompletion(
			RenderRequest request, RenderResponse response, Object handler, Exception ex) throws Exception {

		this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
	}

	@Override
	public boolean preHandleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception {

		this.requestInterceptor.preHandle(new PortletWebRequest(request));
		return true;
	}

	@Override
	public void postHandleResource(ResourceRequest request, ResourceResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {

		this.requestInterceptor.postHandle(new PortletWebRequest(request),
				(modelAndView != null ? modelAndView.getModelMap() : null));
	}

	@Override
	public void afterResourceCompletion(ResourceRequest request, ResourceResponse response, Object handler,
			Exception ex) throws Exception {

		this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
	}

	@Override
	public boolean preHandleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
		this.requestInterceptor.preHandle(new PortletWebRequest(request));
		return true;
	}

	@Override
	public void afterEventCompletion(EventRequest request, EventResponse response, Object handler, Exception ex)
			throws Exception {

		this.requestInterceptor.afterCompletion(new PortletWebRequest(request), ex);
	}

}
