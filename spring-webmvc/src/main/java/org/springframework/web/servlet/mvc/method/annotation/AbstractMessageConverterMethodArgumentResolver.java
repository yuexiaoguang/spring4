package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * 通过使用{@link HttpMessageConverter}读取请求正文来解析方法参数值的基类.
 */
public abstract class AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

	private static final Object NO_VALUE = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	protected final List<HttpMessageConverter<?>> messageConverters;

	protected final List<MediaType> allSupportedMediaTypes;

	private final RequestResponseBodyAdviceChain advice;


	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
		this(converters, null);
	}

	/**
	 * 转换器和{@code Request~}和{@code ResponseBodyAdvice}.
	 */
	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters,
			List<Object> requestResponseBodyAdvice) {

		Assert.notEmpty(converters, "'messageConverters' must not be empty");
		this.messageConverters = converters;
		this.allSupportedMediaTypes = getAllSupportedMediaTypes(converters);
		this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
	}


	/**
	 * 返回通过{@link MediaType#sortBySpecificity(List)}特定排序的所有提供的消息转换器支持的媒体类型.
	 */
	private static List<MediaType> getAllSupportedMediaTypes(List<HttpMessageConverter<?>> messageConverters) {
		Set<MediaType> allSupportedMediaTypes = new LinkedHashSet<MediaType>();
		for (HttpMessageConverter<?> messageConverter : messageConverters) {
			allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
		}
		List<MediaType> result = new ArrayList<MediaType>(allSupportedMediaTypes);
		MediaType.sortBySpecificity(result);
		return Collections.unmodifiableList(result);
	}


	/**
	 * 返回配置的{@link RequestBodyAdvice}和{@link RequestBodyAdvice}, 其中每个实例都可以包装为
	 * {@link org.springframework.web.method.ControllerAdviceBean ControllerAdviceBean}.
	 */
	protected RequestResponseBodyAdviceChain getAdvice() {
		return this.advice;
	}

	/**
	 * 通过读取给定的请求来创建预期参数类型的方法参数值.
	 * 
	 * @param <T> 要创建的参数值的预期类型
	 * @param webRequest 当前的请求
	 * @param parameter 方法参数描述符 (may be {@code null})
	 * @param paramType 要创建的参数值的类型
	 * 
	 * @return 创建的方法参数值
	 * @throws IOException 如果读取请求失败
	 * @throws HttpMediaTypeNotSupportedException 如果找不到合适的消息转换器
	 */
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
			Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		HttpInputMessage inputMessage = createInputMessage(webRequest);
		return readWithMessageConverters(inputMessage, parameter, paramType);
	}

	/**
	 * 通过读取给定的HttpInputMessage来创建预期参数类型的方法参数值.
	 * 
	 * @param <T> 要创建的参数值的预期类型
	 * @param inputMessage 表示当前请求的HTTP输入消息
	 * @param parameter 方法参数描述符 (may be {@code null})
	 * @param targetType 目标类型, 不一定与方法参数类型相同, e.g. 用于{@code HttpEntity<String>}.
	 * 
	 * @return 创建的方法参数值
	 * @throws IOException 如果读取请求失败
	 * @throws HttpMediaTypeNotSupportedException 如果找不到合适的消息转换器
	 */
	@SuppressWarnings("unchecked")
	protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		MediaType contentType;
		boolean noContentType = false;
		try {
			contentType = inputMessage.getHeaders().getContentType();
		}
		catch (InvalidMediaTypeException ex) {
			throw new HttpMediaTypeNotSupportedException(ex.getMessage());
		}
		if (contentType == null) {
			noContentType = true;
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}

		Class<?> contextClass = (parameter != null ? parameter.getContainingClass() : null);
		Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);
		if (targetClass == null) {
			ResolvableType resolvableType = (parameter != null ?
					ResolvableType.forMethodParameter(parameter) : ResolvableType.forType(targetType));
			targetClass = (Class<T>) resolvableType.resolve();
		}

		HttpMethod httpMethod = ((HttpRequest) inputMessage).getMethod();
		Object body = NO_VALUE;

		try {
			inputMessage = new EmptyBodyCheckingHttpInputMessage(inputMessage);

			for (HttpMessageConverter<?> converter : this.messageConverters) {
				Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();
				if (converter instanceof GenericHttpMessageConverter) {
					GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
					if (genericConverter.canRead(targetType, contextClass, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Read [" + targetType + "] as \"" + contentType + "\" with [" + converter + "]");
						}
						if (inputMessage.getBody() != null) {
							inputMessage = getAdvice().beforeBodyRead(inputMessage, parameter, targetType, converterType);
							body = genericConverter.read(targetType, contextClass, inputMessage);
							body = getAdvice().afterBodyRead(body, inputMessage, parameter, targetType, converterType);
						}
						else {
							body = getAdvice().handleEmptyBody(null, inputMessage, parameter, targetType, converterType);
						}
						break;
					}
				}
				else if (targetClass != null) {
					if (converter.canRead(targetClass, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Read [" + targetType + "] as \"" + contentType + "\" with [" + converter + "]");
						}
						if (inputMessage.getBody() != null) {
							inputMessage = getAdvice().beforeBodyRead(inputMessage, parameter, targetType, converterType);
							body = ((HttpMessageConverter<T>) converter).read(targetClass, inputMessage);
							body = getAdvice().afterBodyRead(body, inputMessage, parameter, targetType, converterType);
						}
						else {
							body = getAdvice().handleEmptyBody(null, inputMessage, parameter, targetType, converterType);
						}
						break;
					}
				}
			}
		}
		catch (IOException ex) {
			throw new HttpMessageNotReadableException("I/O error while reading input message", ex);
		}

		if (body == NO_VALUE) {
			if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) ||
					(noContentType && inputMessage.getBody() == null)) {
				return null;
			}
			throw new HttpMediaTypeNotSupportedException(contentType, this.allSupportedMediaTypes);
		}

		return body;
	}

	/**
	 * 从给定的{@link NativeWebRequest}创建一个新的{@link HttpInputMessage}.
	 * 
	 * @param webRequest 从中创建输入消息的Web请求
	 * 
	 * @return 输入消息
	 */
	protected ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		return new ServletServerHttpRequest(servletRequest);
	}

	/**
	 * 如果适用, 验证绑定目标.
	 * <p>默认实现检查{@code @javax.validation.Valid},
	 * Spring的{@link org.springframework.validation.annotation.Validated}, 以及名称以"Valid"开头的自定义注解.
	 * 
	 * @param binder 要使用的DataBinder
	 * @param parameter 方法参数描述符
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				binder.validate(validationHints);
				break;
			}
		}
	}

	/**
	 * 是否在验证错误时引发致命的绑定异常.
	 * 
	 * @param binder 用于执行数据绑定的数据绑定器
	 * @param parameter 方法参数描述符
	 * 
	 * @return {@code true} 如果下一个方法参数不是{@link Errors}类型
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getMethod().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
	}

	/**
	 * 如有必要, 根据方法参数适配给定参数.
	 * 
	 * @param arg 解析的参数
	 * @param parameter 方法参数描述符
	 * 
	 * @return 适配的参数, 或解析的原始参数
	 */
	protected Object adaptArgumentIfNecessary(Object arg, MethodParameter parameter) {
		return (parameter.isOptional() ? OptionalResolver.resolveValue(arg) : arg);
	}


	private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {

		private final HttpHeaders headers;

		private final InputStream body;

		private final HttpMethod method;


		public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
			this.headers = inputMessage.getHeaders();
			InputStream inputStream = inputMessage.getBody();
			if (inputStream == null) {
				this.body = null;
			}
			else if (inputStream.markSupported()) {
				inputStream.mark(1);
				this.body = (inputStream.read() != -1 ? inputStream : null);
				inputStream.reset();
			}
			else {
				PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
				int b = pushbackInputStream.read();
				if (b == -1) {
					this.body = null;
				}
				else {
					this.body = pushbackInputStream;
					pushbackInputStream.unread(b);
				}
			}
			this.method = ((HttpRequest) inputMessage).getMethod();
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			return this.body;
		}

		public HttpMethod getMethod() {
			return this.method;
		}
	}


	/**
	 * 内部类, 以避免对Java 8的Optional类型的硬编码依赖性...
	 */
	@UsesJava8
	private static class OptionalResolver {

		public static Object resolveValue(Object value) {
			if (value == null || (value instanceof Collection && ((Collection) value).isEmpty()) ||
					(value instanceof Object[] && ((Object[]) value).length == 0)) {
				return Optional.empty();
			}
			return Optional.of(value);
		}
	}

}
