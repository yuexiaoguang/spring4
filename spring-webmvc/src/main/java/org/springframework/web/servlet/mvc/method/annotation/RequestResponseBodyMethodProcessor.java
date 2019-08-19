package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * 解析带{@code @RequestBody}注解的方法参数, 并通过使用{@link HttpMessageConverter}
 * 读取和写入请求或响应的主体来处理使用{@code @ResponseBody}注解的方法的返回值.
 *
 * <p>如果{@code @RequestBody}方法参数使用{@code @javax.validation.Valid}注解, 也会对其进行验证.
 * 如果验证失败, 则会引发{@link MethodArgumentNotValidException},
 * 如果配置了{@link DefaultHandlerExceptionResolver}, 则会生成HTTP 400响应状态码.
 */
public class RequestResponseBodyMethodProcessor extends AbstractMessageConverterMethodProcessor {

	/**
	 * 适合解析{@code @RequestBody}.
	 * 要处理{@code @ResponseBody}, 考虑提供{@code ContentNegotiationManager}.
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters) {
		super(converters);
	}

	/**
	 * 适用于解析{@code @RequestBody}, 并处理没有{@code Request~}或{@code ResponseBodyAdvice}的{@code @ResponseBody}.
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager) {

		super(converters, manager);
	}

	/**
	 * 用于解析{@code @RequestBody}方法参数的完整构造函数.
	 * 要处理{@code @ResponseBody}, 考虑提供{@code ContentNegotiationManager}.
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
			List<Object> requestResponseBodyAdvice) {

		super(converters, null, requestResponseBodyAdvice);
	}

	/**
	 * 完整的构造函数, 用于解析{@code @RequestBody}并处理{@code @ResponseBody}.
	 */
	public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {

		super(converters, manager, requestResponseBodyAdvice);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||
				returnType.hasMethodAnnotation(ResponseBody.class));
	}

	/**
	 * 如果验证失败, 则抛出MethodArgumentNotValidException.
	 * 
	 * @throws HttpMessageNotReadableException 如果{@link RequestBody#required()}是{@code true},
	 * 并且没有正文内容, 或者没有合适的转换器来读取内容.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		parameter = parameter.nestedIfOptional();
		Object arg = readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());
		String name = Conventions.getVariableNameForParameter(parameter);

		WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
		if (arg != null) {
			validateIfApplicable(binder, parameter);
			if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
				throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
			}
		}
		mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());

		return adaptArgumentIfNecessary(arg, parameter);
	}

	@Override
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
			Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(servletRequest);

		Object arg = readWithMessageConverters(inputMessage, parameter, paramType);
		if (arg == null) {
			if (checkRequired(parameter)) {
				throw new HttpMessageNotReadableException("Required request body is missing: " +
						parameter.getMethod().toGenericString());
			}
		}
		return arg;
	}

	protected boolean checkRequired(MethodParameter parameter) {
		return (parameter.getParameterAnnotation(RequestBody.class).required() && !parameter.isOptional());
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		mavContainer.setRequestHandled(true);
		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

		// 尝试使用null返回值. ResponseBodyAdvice可以参与其中.
		writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
	}

}
