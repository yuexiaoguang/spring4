package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

/**
 * 帮助配置映射的拦截器列表.
 */
public class InterceptorRegistry {

	private final List<InterceptorRegistration> registrations = new ArrayList<InterceptorRegistration>();


	/**
	 * 添加提供的{@link HandlerInterceptor}.
	 * 
	 * @param interceptor 要添加的拦截器
	 * 
	 * @return 一个{@link InterceptorRegistration}, 允许选择性地配置已注册的拦截器,
	 * 例如添加它应该应用的URL模式.
	 */
	public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
		InterceptorRegistration registration = new InterceptorRegistration(interceptor);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 添加提供的{@link WebRequestInterceptor}.
	 * 
	 * @param interceptor 要添加的拦截器
	 * 
	 * @return 一个{@link InterceptorRegistration}, 允许选择性地配置已注册的拦截器,
	 * 例如添加它应该应用的URL模式.
	 */
	public InterceptorRegistration addWebRequestInterceptor(WebRequestInterceptor interceptor) {
		WebRequestHandlerInterceptorAdapter adapted = new WebRequestHandlerInterceptorAdapter(interceptor);
		InterceptorRegistration registration = new InterceptorRegistration(adapted);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * 返回所有已注册的拦截器.
	 */
	protected List<Object> getInterceptors() {
		List<Object> interceptors = new ArrayList<Object>(this.registrations.size());
		for (InterceptorRegistration registration : this.registrations) {
			interceptors.add(registration.getInterceptor());
		}
		return interceptors ;
	}
}
