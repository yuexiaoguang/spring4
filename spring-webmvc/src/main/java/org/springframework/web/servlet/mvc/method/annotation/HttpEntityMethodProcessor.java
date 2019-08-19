package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析{@link HttpEntity}和{@link RequestEntity}方法参数值,
 * 并处理{@link HttpEntity}和{@link ResponseEntity}返回值.
 *
 * <p>{@link HttpEntity}返回类型具有特定目的.
 * 因此, 应该在处理器之前配置此处理器, 该处理器支持使用{@code @ModelAttribute}
 * 或{@code @ResponseBody}注解的返回值类型, 以确保它们不会接管.
 */
public class HttpEntityMethodProcessor extends AbstractMessageConverterMethodProcessor {

	private static final Set<HttpMethod> SAFE_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

	/**
	 * 适合解析{@code HttpEntity}.
	 * 要处理{@code ResponseEntity}, 考虑提供{@code ContentNegotiationManager}.
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters) {
		super(converters);
	}

	/**
	 * 适用于解析{@code HttpEntity},
	 * 并在没有{@code Request~}或{@code ResponseBodyAdvice}的情况下处理{@code ResponseEntity}.
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager) {

		super(converters, manager);
	}

	/**
	 * 用于解析{@code HttpEntity}方法参数的完整构造函数.
	 * 要处理{@code ResponseEntity}, 考虑提供{@code ContentNegotiationManager}.
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
			List<Object> requestResponseBodyAdvice) {

		super(converters, null, requestResponseBodyAdvice);
	}

	/**
	 * 用于解析{@code HttpEntity}和处理{@code ResponseEntity}的完整构造函数.
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {

		super(converters, manager, requestResponseBodyAdvice);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (HttpEntity.class == parameter.getParameterType() ||
				RequestEntity.class == parameter.getParameterType());
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (HttpEntity.class.isAssignableFrom(returnType.getParameterType()) &&
				!RequestEntity.class.isAssignableFrom(returnType.getParameterType()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
			throws IOException, HttpMediaTypeNotSupportedException {

		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		Type paramType = getHttpEntityType(parameter);
		if (paramType == null) {
			throw new IllegalArgumentException("HttpEntity parameter '" + parameter.getParameterName() +
					"' in method " + parameter.getMethod() + " is not parameterized");
		}

		Object body = readWithMessageConverters(webRequest, parameter, paramType);
		if (RequestEntity.class == parameter.getParameterType()) {
			return new RequestEntity<Object>(body, inputMessage.getHeaders(),
					inputMessage.getMethod(), inputMessage.getURI());
		}
		else {
			return new HttpEntity<Object>(body, inputMessage.getHeaders());
		}
	}

	private Type getHttpEntityType(MethodParameter parameter) {
		Assert.isAssignable(HttpEntity.class, parameter.getParameterType());
		Type parameterType = parameter.getGenericParameterType();
		if (parameterType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) parameterType;
			if (type.getActualTypeArguments().length != 1) {
				throw new IllegalArgumentException("Expected single generic parameter on '" +
						parameter.getParameterName() + "' in method " + parameter.getMethod());
			}
			return type.getActualTypeArguments()[0];
		}
		else if (parameterType instanceof Class) {
			return Object.class;
		}
		else {
			return null;
		}
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		mavContainer.setRequestHandled(true);
		if (returnValue == null) {
			return;
		}

		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

		Assert.isInstanceOf(HttpEntity.class, returnValue);
		HttpEntity<?> responseEntity = (HttpEntity<?>) returnValue;

		HttpHeaders outputHeaders = outputMessage.getHeaders();
		HttpHeaders entityHeaders = responseEntity.getHeaders();
		if (!entityHeaders.isEmpty()) {
			for (Map.Entry<String, List<String>> entry : entityHeaders.entrySet()) {
				if (HttpHeaders.VARY.equals(entry.getKey()) && outputHeaders.containsKey(HttpHeaders.VARY)) {
					List<String> values = getVaryRequestHeadersToAdd(outputHeaders, entityHeaders);
					if (!values.isEmpty()) {
						outputHeaders.setVary(values);
					}
				}
				else {
					outputHeaders.put(entry.getKey(), entry.getValue());
				}
			}
		}

		if (responseEntity instanceof ResponseEntity) {
			int returnStatus = ((ResponseEntity<?>) responseEntity).getStatusCodeValue();
			outputMessage.getServletResponse().setStatus(returnStatus);
			if (returnStatus == 200) {
				if (SAFE_METHODS.contains(inputMessage.getMethod())
						&& isResourceNotModified(inputMessage, outputMessage)) {
					// 确保刷新header, 不应写入正文.
					outputMessage.flush();
					// 跳过对转换器的调用, 因为它们可能会更新正文.
					return;
				}
			}
		}

		// 尝试使用null 正文. ResponseBodyAdvice 可以参与其中.
		writeWithMessageConverters(responseEntity.getBody(), returnType, inputMessage, outputMessage);

		// 即使没有写入正文, 也要确保刷新header.
		outputMessage.flush();
	}

	private List<String> getVaryRequestHeadersToAdd(HttpHeaders responseHeaders, HttpHeaders entityHeaders) {
		List<String> entityHeadersVary = entityHeaders.getVary();
		List<String> vary = responseHeaders.get(HttpHeaders.VARY);
		if (vary != null) {
			List<String> result = new ArrayList<String>(entityHeadersVary);
			for (String header : vary) {
				for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
					if ("*".equals(existing)) {
						return Collections.emptyList();
					}
					for (String value : entityHeadersVary) {
						if (value.equalsIgnoreCase(existing)) {
							result.remove(value);
						}
					}
				}
			}
			return result;
		}
		return entityHeadersVary;
	}

	private boolean isResourceNotModified(ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage) {
		ServletWebRequest servletWebRequest =
				new ServletWebRequest(inputMessage.getServletRequest(), outputMessage.getServletResponse());
		HttpHeaders responseHeaders = outputMessage.getHeaders();
		String etag = responseHeaders.getETag();
		long lastModifiedTimestamp = responseHeaders.getLastModified();
		if (inputMessage.getMethod() == HttpMethod.GET || inputMessage.getMethod() == HttpMethod.HEAD) {
			responseHeaders.remove(HttpHeaders.ETAG);
			responseHeaders.remove(HttpHeaders.LAST_MODIFIED);
		}

		return servletWebRequest.checkNotModified(etag, lastModifiedTimestamp);
	}

	@Override
	protected Class<?> getReturnValueType(Object returnValue, MethodParameter returnType) {
		if (returnValue != null) {
			return returnValue.getClass();
		}
		else {
			Type type = getHttpEntityType(returnType);
			type = (type != null ? type : Object.class);
			return ResolvableType.forMethodParameter(returnType, type).resolve(Object.class);
		}
	}
}
