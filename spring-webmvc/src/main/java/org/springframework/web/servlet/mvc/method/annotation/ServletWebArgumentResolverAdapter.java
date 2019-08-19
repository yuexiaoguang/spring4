package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter;

/**
 * 特定于Servlet的{@link org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter}
 * 它从{@link ServletRequestAttributes}创建{@link NativeWebRequest}.
 *
 * <p><strong>Note:</strong> 提供此类是为了向后兼容.
 * 但是建议重写{@code WebArgumentResolver}为{@code HandlerMethodArgumentResolver}.
 * For more details see javadoc of
 * {@link org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter}.
 */
public class ServletWebArgumentResolverAdapter extends AbstractWebArgumentResolverAdapter {

	public ServletWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		super(adaptee);
	}

	@Override
	protected NativeWebRequest getWebRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
			return new ServletWebRequest(servletRequestAttributes.getRequest());
		}
		return null;
	}
}
