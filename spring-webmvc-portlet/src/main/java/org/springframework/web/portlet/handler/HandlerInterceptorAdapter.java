package org.springframework.web.portlet.handler;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.web.portlet.HandlerInterceptor;
import org.springframework.web.portlet.ModelAndView;

/**
 * {@link HandlerInterceptor}接口的抽象适配器类, 用于简化pre-only/post-only拦截器的实现.
 */
public abstract class HandlerInterceptorAdapter implements HandlerInterceptor {

	/**
	 * 此实现委托给{@link #preHandle}.
	 */
	@Override
	public boolean preHandleAction(ActionRequest request, ActionResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * 此实现委托给{@link #afterCompletion}.
	 */
	@Override
	public void afterActionCompletion(
			ActionRequest request, ActionResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * 此实现委托给{@link #preHandle}.
	 */
	@Override
	public boolean preHandleRender(RenderRequest request, RenderResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * 此实现为空.
	 */
	@Override
	public void postHandleRender(
			RenderRequest request, RenderResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {
	}

	/**
	 * 此实现委托给{@link #afterCompletion}.
	 */
	@Override
	public void afterRenderCompletion(
			RenderRequest request, RenderResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * 此实现委托给{@link #preHandle}.
	 */
	@Override
	public boolean preHandleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * 此实现为空.
	 */
	@Override
	public void postHandleResource(
			ResourceRequest request, ResourceResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {
	}

	/**
	 * 此实现委托给{@link #afterCompletion}.
	 */
	@Override
	public void afterResourceCompletion(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * 此实现委托给{@link #preHandle}.
	 */
	@Override
	public boolean preHandleEvent(EventRequest request, EventResponse response, Object handler)
			throws Exception {

		return preHandle(request, response, handler);
	}

	/**
	 * 此实现委托给{@link #afterCompletion}.
	 */
	@Override
	public void afterEventCompletion(
			EventRequest request, EventResponse response, Object handler, Exception ex)
			throws Exception {

		afterCompletion(request, response, handler, ex);
	}


	/**
	 * 所有"pre*"方法委托的默认回调.
	 * <p>此实现始终返回{@code true}.
	 */
	protected boolean preHandle(PortletRequest request, PortletResponse response, Object handler)
			throws Exception {

		return true;
	}

	/**
	 * 所有"after*"方法委托的默认回调.
	 * <p>此实现为空.
	 */
	protected void afterCompletion(
			PortletRequest request, PortletResponse response, Object handler, Exception ex)
			throws Exception {

	}
}
