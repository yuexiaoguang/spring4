package org.springframework.web.servlet.mvc.method.annotation;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * An {@link org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver}
 * that resolves cookie values from an {@link HttpServletRequest}.
 */
public class ServletCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	public ServletCookieValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}


	@Override
	protected Object resolveName(String cookieName, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Cookie cookieValue = WebUtils.getCookie(servletRequest, cookieName);
		if (Cookie.class.isAssignableFrom(parameter.getNestedParameterType())) {
			return cookieValue;
		}
		else if (cookieValue != null) {
			return this.urlPathHelper.decodeRequestString(servletRequest, cookieValue.getValue());
		}
		else {
			return null;
		}
	}

}