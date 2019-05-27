package org.springframework.web.servlet.mvc.method.annotation;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves response-related method argument values of types:
 * <ul>
 * <li>{@link ServletResponse}
 * <li>{@link OutputStream}
 * <li>{@link Writer}
 * </ul>
 */
public class ServletResponseMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return (ServletResponse.class.isAssignableFrom(paramType) ||
				OutputStream.class.isAssignableFrom(paramType) ||
				Writer.class.isAssignableFrom(paramType));
	}

	/**
	 * Set {@link ModelAndViewContainer#setRequestHandled(boolean)} to
	 * {@code false} to indicate that the method signature provides access
	 * to the response. If subsequently the underlying method returns
	 * {@code null}, the request is considered directly handled.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		if (mavContainer != null) {
			mavContainer.setRequestHandled(true);
		}

		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		Class<?> paramType = parameter.getParameterType();

		if (ServletResponse.class.isAssignableFrom(paramType)) {
			Object nativeResponse = webRequest.getNativeResponse(paramType);
			if (nativeResponse == null) {
				throw new IllegalStateException(
						"Current response is not of type [" + paramType.getName() + "]: " + response);
			}
			return nativeResponse;
		}
		else if (OutputStream.class.isAssignableFrom(paramType)) {
			return response.getOutputStream();
		}
		else if (Writer.class.isAssignableFrom(paramType)) {
			return response.getWriter();
		}
		else {
			// should not happen
			Method method = parameter.getMethod();
			throw new UnsupportedOperationException("Unknown parameter type: " + paramType + " in method: " + method);
		}
	}

}
