package org.springframework.web.servlet.mvc.method.annotation;

import javax.servlet.ServletException;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;

/**
 * 解析带 @{@link SessionAttribute}注解的方法参数.
 */
public class SessionAttributeMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(SessionAttribute.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		SessionAttribute ann = parameter.getParameterAnnotation(SessionAttribute.class);
		return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
	}

	@Override
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request){
		return request.getAttribute(name, RequestAttributes.SCOPE_SESSION);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new ServletRequestBindingException("Missing session attribute '" + name +
				"' of type " +  parameter.getNestedParameterType().getSimpleName());
	}

}
