package org.springframework.web.servlet.handler;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * {@link ServletWebRequest}子类, 知道{@link org.springframework.web.servlet.DispatcherServlet}的请求上下文,
 * 例如由配置的{@link org.springframework.web.servlet.LocaleResolver}确定的Locale.
 */
public class DispatcherServletWebRequest extends ServletWebRequest {

	/**
	 * @param request 当前的HTTP请求
	 */
	public DispatcherServletWebRequest(HttpServletRequest request) {
		super(request);
	}

	/**
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 */
	public DispatcherServletWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	@Override
	public Locale getLocale() {
		return RequestContextUtils.getLocale(getRequest());
	}

}
