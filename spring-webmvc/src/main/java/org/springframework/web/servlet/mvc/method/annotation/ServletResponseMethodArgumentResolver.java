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
 * 解决类型的响应相关的方法参数值:
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
	 * 将{@link ModelAndViewContainer#setRequestHandled(boolean)}设置为{@code false},
	 * 以指示方法签名提供对响应的访问.
	 * 如果随后底层方法返回{@code null}, 则认为该请求是直接处理的.
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
