package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

/**
 * Helps with configuring a list of mapped interceptors.
 */
public class InterceptorRegistry {

	private final List<InterceptorRegistration> registrations = new ArrayList<InterceptorRegistration>();


	/**
	 * Adds the provided {@link HandlerInterceptor}.
	 * @param interceptor the interceptor to add
	 * @return An {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 */
	public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
		InterceptorRegistration registration = new InterceptorRegistration(interceptor);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Adds the provided {@link WebRequestInterceptor}.
	 * @param interceptor the interceptor to add
	 * @return An {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 */
	public InterceptorRegistration addWebRequestInterceptor(WebRequestInterceptor interceptor) {
		WebRequestHandlerInterceptorAdapter adapted = new WebRequestHandlerInterceptorAdapter(interceptor);
		InterceptorRegistration registration = new InterceptorRegistration(adapted);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Return all registered interceptors.
	 */
	protected List<Object> getInterceptors() {
		List<Object> interceptors = new ArrayList<Object>(this.registrations.size());
		for (InterceptorRegistration registration : this.registrations) {
			interceptors.add(registration.getInterceptor());
		}
		return interceptors ;
	}

}
