package org.springframework.web.context.request.async;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * 与处理异步Web请求相关的实用方法.
 */
public abstract class WebAsyncUtils {

	public static final String WEB_ASYNC_MANAGER_ATTRIBUTE = WebAsyncManager.class.getName() + ".WEB_ASYNC_MANAGER";

	// 确定Servlet 3.0的ServletRequest.startAsync方法是否可用
	private static final boolean startAsyncAvailable = ClassUtils.hasMethod(ServletRequest.class, "startAsync");


	/**
	 * 获取当前请求的{@link WebAsyncManager}, 如果未找到, 则创建并将其与请求关联.
	 */
	public static WebAsyncManager getAsyncManager(ServletRequest servletRequest) {
		WebAsyncManager asyncManager = null;
		Object asyncManagerAttr = servletRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
		if (asyncManagerAttr instanceof WebAsyncManager) {
			asyncManager = (WebAsyncManager) asyncManagerAttr;
		}
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			servletRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager);
		}
		return asyncManager;
	}

	/**
	 * 获取当前请求的{@link WebAsyncManager}, 如果未找到, 则创建并将其与请求关联.
	 */
	public static WebAsyncManager getAsyncManager(WebRequest webRequest) {
		int scope = RequestAttributes.SCOPE_REQUEST;
		WebAsyncManager asyncManager = null;
		Object asyncManagerAttr = webRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, scope);
		if (asyncManagerAttr instanceof WebAsyncManager) {
			asyncManager = (WebAsyncManager) asyncManagerAttr;
		}
		if (asyncManager == null) {
			asyncManager = new WebAsyncManager();
			webRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager, scope);
		}
		return asyncManager;
	}

	/**
	 * 创建AsyncWebRequest实例.
	 * 默认情况下, 在Servlet 3.0 (或更高版本)环境中运行时, 
	 * 会创建{@link StandardServletAsyncWebRequest}的实例 - 作为后备, 将返回{@link NoSupportAsyncWebRequest}的实例.
	 * 
	 * @param request 当前请求
	 * @param response 当前响应
	 * 
	 * @return AsyncWebRequest实例 (never {@code null})
	 */
	public static AsyncWebRequest createAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		return (startAsyncAvailable ? AsyncWebRequestFactory.createStandardAsyncWebRequest(request, response) :
				new NoSupportAsyncWebRequest(request, response));
	}


	/**
	 * 内部类, 以避免硬件依赖于Servlet 3.0 API.
	 */
	private static class AsyncWebRequestFactory {

		public static AsyncWebRequest createStandardAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
			return new StandardServletAsyncWebRequest(request, response);
		}
	}

}
