package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * 通过使用{@link HttpMessageConverter}写入响应来
 * 扩展{@link AbstractMessageConverterMethodArgumentResolver}以处理方法返回值.
 */
public abstract class AbstractMessageConverterMethodProcessor extends AbstractMessageConverterMethodArgumentResolver
		implements HandlerMethodReturnValueHandler {

	/* 与内置消息转换器关联的扩展名 */
	private static final Set<String> WHITELISTED_EXTENSIONS = new HashSet<String>(Arrays.asList(
			"txt", "text", "yml", "properties", "csv",
			"json", "xml", "atom", "rss",
			"png", "jpe", "jpeg", "jpg", "gif", "wbmp", "bmp"));

	private static final Set<String> WHITELISTED_MEDIA_BASE_TYPES = new HashSet<String>(
			Arrays.asList("audio", "image", "video"));

	private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

	private static final UrlPathHelper DECODING_URL_PATH_HELPER = new UrlPathHelper();

	private static final UrlPathHelper RAW_URL_PATH_HELPER = new UrlPathHelper();

	static {
		RAW_URL_PATH_HELPER.setRemoveSemicolonContent(false);
		RAW_URL_PATH_HELPER.setUrlDecode(false);
	}


	private final ContentNegotiationManager contentNegotiationManager;

	private final PathExtensionContentNegotiationStrategy pathStrategy;

	private final Set<String> safeExtensions = new HashSet<String>();


	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters) {
		this(converters, null);
	}

	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager contentNegotiationManager) {

		this(converters, contentNegotiationManager, null);
	}

	/**
	 * 请求/响应主体增强实例.
	 */
	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {

		super(converters, requestResponseBodyAdvice);
		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
		this.pathStrategy = initPathStrategy(this.contentNegotiationManager);
		this.safeExtensions.addAll(this.contentNegotiationManager.getAllFileExtensions());
		this.safeExtensions.addAll(WHITELISTED_EXTENSIONS);
	}

	private static PathExtensionContentNegotiationStrategy initPathStrategy(ContentNegotiationManager manager) {
		Class<PathExtensionContentNegotiationStrategy> clazz = PathExtensionContentNegotiationStrategy.class;
		PathExtensionContentNegotiationStrategy strategy = manager.getStrategy(clazz);
		return (strategy != null ? strategy : new PathExtensionContentNegotiationStrategy());
	}


	/**
	 * 从给定的{@link NativeWebRequest}创建一个新的{@link HttpOutputMessage}.
	 * 
	 * @param webRequest 从中创建输出消息的Web请求
	 * 
	 * @return 输出消息
	 */
	protected ServletServerHttpResponse createOutputMessage(NativeWebRequest webRequest) {
		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		return new ServletServerHttpResponse(response);
	}

	/**
	 * 将给定的返回值写入给定的Web请求.
	 * 委托给
	 * {@link #writeWithMessageConverters(Object, MethodParameter, ServletServerHttpRequest, ServletServerHttpResponse)}
	 */
	protected <T> void writeWithMessageConverters(T value, MethodParameter returnType, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
		writeWithMessageConverters(value, returnType, inputMessage, outputMessage);
	}

	/**
	 * 将给定的返回类型写入给定的输出消息.
	 * 
	 * @param value 要写入输出消息的值
	 * @param returnType 值的类型
	 * @param inputMessage 输入消息. 用于检查{@code Accept} header.
	 * @param outputMessage 要写入的输出消息
	 * 
	 * @throws IOException
	 * @throws HttpMediaTypeNotAcceptableException 当消息转换器无法满足请求上的{@code Accept} header指示的条件时抛出
	 */
	@SuppressWarnings("unchecked")
	protected <T> void writeWithMessageConverters(T value, MethodParameter returnType,
			ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		Object outputValue;
		Class<?> valueType;
		Type declaredType;

		if (value instanceof CharSequence) {
			outputValue = value.toString();
			valueType = String.class;
			declaredType = String.class;
		}
		else {
			outputValue = value;
			valueType = getReturnValueType(outputValue, returnType);
			declaredType = getGenericType(returnType);
		}

		HttpServletRequest request = inputMessage.getServletRequest();
		List<MediaType> requestedMediaTypes = getAcceptableMediaTypes(request);
		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request, valueType, declaredType);

		if (outputValue != null && producibleMediaTypes.isEmpty()) {
			throw new IllegalArgumentException("No converter found for return value of type: " + valueType);
		}

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<MediaType>();
		for (MediaType requestedType : requestedMediaTypes) {
			for (MediaType producibleType : producibleMediaTypes) {
				if (requestedType.isCompatibleWith(producibleType)) {
					compatibleMediaTypes.add(getMostSpecificMediaType(requestedType, producibleType));
				}
			}
		}
		if (compatibleMediaTypes.isEmpty()) {
			if (outputValue != null) {
				throw new HttpMediaTypeNotAcceptableException(producibleMediaTypes);
			}
			return;
		}

		List<MediaType> mediaTypes = new ArrayList<MediaType>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(mediaTypes);

		MediaType selectedMediaType = null;
		for (MediaType mediaType : mediaTypes) {
			if (mediaType.isConcrete()) {
				selectedMediaType = mediaType;
				break;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
				selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}

		if (selectedMediaType != null) {
			selectedMediaType = selectedMediaType.removeQualityValue();
			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
				if (messageConverter instanceof GenericHttpMessageConverter) {
					if (((GenericHttpMessageConverter) messageConverter).canWrite(
							declaredType, valueType, selectedMediaType)) {
						outputValue = (T) getAdvice().beforeBodyWrite(outputValue, returnType, selectedMediaType,
								(Class<? extends HttpMessageConverter<?>>) messageConverter.getClass(),
								inputMessage, outputMessage);
						if (outputValue != null) {
							addContentDispositionHeader(inputMessage, outputMessage);
							((GenericHttpMessageConverter) messageConverter).write(
									outputValue, declaredType, selectedMediaType, outputMessage);
							if (logger.isDebugEnabled()) {
								logger.debug("Written [" + outputValue + "] as \"" + selectedMediaType +
										"\" using [" + messageConverter + "]");
							}
						}
						return;
					}
				}
				else if (messageConverter.canWrite(valueType, selectedMediaType)) {
					outputValue = (T) getAdvice().beforeBodyWrite(outputValue, returnType, selectedMediaType,
							(Class<? extends HttpMessageConverter<?>>) messageConverter.getClass(),
							inputMessage, outputMessage);
					if (outputValue != null) {
						addContentDispositionHeader(inputMessage, outputMessage);
						((HttpMessageConverter) messageConverter).write(outputValue, selectedMediaType, outputMessage);
						if (logger.isDebugEnabled()) {
							logger.debug("Written [" + outputValue + "] as \"" + selectedMediaType +
									"\" using [" + messageConverter + "]");
						}
					}
					return;
				}
			}
		}

		if (outputValue != null) {
			throw new HttpMediaTypeNotAcceptableException(this.allSupportedMediaTypes);
		}
	}

	/**
	 * 返回要写入响应的值的类型.
	 * 通常这是通过getClass对值进行的简单检查, 但如果值为null, 则需要检查返回类型, 可能包括泛型类型确定
	 * (e.g. {@code ResponseEntity<T>}).
	 */
	protected Class<?> getReturnValueType(Object value, MethodParameter returnType) {
		return (value != null ? value.getClass() : returnType.getParameterType());
	}

	/**
	 * 返回{@code returnType}的泛型类型 (如果它是{@link HttpEntity}, 则返回嵌套类型的泛型类型).
	 */
	private Type getGenericType(MethodParameter returnType) {
		if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
			return ResolvableType.forType(returnType.getGenericParameterType()).getGeneric(0).getType();
		}
		else {
			return returnType.getGenericParameterType();
		}
	}

	@SuppressWarnings({"unchecked", "unused"})
	protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request, Class<?> valueClass) {
		return getProducibleMediaTypes(request, valueClass, null);
	}

	/**
	 * 返回可以生成的媒体类型:
	 * <ul>
	 * <li>请求映射中指定的可生成媒体类型, 或
	 * <li>配置的转换器的媒体类型, 可以写入特定的返回值, 或
	 * <li>{@link MediaType#ALL}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request, Class<?> valueClass, Type declaredType) {
		Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			return new ArrayList<MediaType>(mediaTypes);
		}
		else if (!this.allSupportedMediaTypes.isEmpty()) {
			List<MediaType> result = new ArrayList<MediaType>();
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				if (converter instanceof GenericHttpMessageConverter && declaredType != null) {
					if (((GenericHttpMessageConverter<?>) converter).canWrite(declaredType, valueClass, null)) {
						result.addAll(converter.getSupportedMediaTypes());
					}
				}
				else if (converter.canWrite(valueClass, null)) {
					result.addAll(converter.getSupportedMediaTypes());
				}
			}
			return result;
		}
		else {
			return Collections.singletonList(MediaType.ALL);
		}
	}

	private List<MediaType> getAcceptableMediaTypes(HttpServletRequest request) throws HttpMediaTypeNotAcceptableException {
		List<MediaType> mediaTypes = this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	/**
	 * 使用前者的q值返回更具体的可接受和可生成的媒体类型.
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
		return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceTypeToUse) <= 0 ? acceptType : produceTypeToUse);
	}

	/**
	 * 检查路径是否具有文件扩展名以及扩展名是{@link #WHITELISTED_EXTENSIONS 白名单}还是显式
	 * {@link ContentNegotiationManager#getAllFileExtensions() 注册}.
	 * 如果没有, 并且状态在2xx范围内, 则添加带有安全附件文件名("f.txt")的'Content-Disposition' header以防止RFD攻击.
	 */
	private void addContentDispositionHeader(ServletServerHttpRequest request, ServletServerHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			return;
		}

		try {
			int status = response.getServletResponse().getStatus();
			if (status < 200 || status > 299) {
				return;
			}
		}
		catch (Throwable ex) {
			// ignore
		}

		HttpServletRequest servletRequest = request.getServletRequest();
		String requestUri = RAW_URL_PATH_HELPER.getOriginatingRequestUri(servletRequest);

		int index = requestUri.lastIndexOf('/') + 1;
		String filename = requestUri.substring(index);
		String pathParams = "";

		index = filename.indexOf(';');
		if (index != -1) {
			pathParams = filename.substring(index);
			filename = filename.substring(0, index);
		}

		filename = DECODING_URL_PATH_HELPER.decodeRequestString(servletRequest, filename);
		String ext = StringUtils.getFilenameExtension(filename);

		pathParams = DECODING_URL_PATH_HELPER.decodeRequestString(servletRequest, pathParams);
		String extInPathParams = StringUtils.getFilenameExtension(pathParams);

		if (!safeExtension(servletRequest, ext) || !safeExtension(servletRequest, extInPathParams)) {
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=f.txt");
		}
	}

	@SuppressWarnings("unchecked")
	private boolean safeExtension(HttpServletRequest request, String extension) {
		if (!StringUtils.hasText(extension)) {
			return true;
		}
		extension = extension.toLowerCase(Locale.ENGLISH);
		if (this.safeExtensions.contains(extension)) {
			return true;
		}
		String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (pattern != null && pattern.endsWith("." + extension)) {
			return true;
		}
		if (extension.equals("html")) {
			String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
			Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(name);
			if (!CollectionUtils.isEmpty(mediaTypes) && mediaTypes.contains(MediaType.TEXT_HTML)) {
				return true;
			}
		}
		return safeMediaTypesForExtension(extension);
	}

	private boolean safeMediaTypesForExtension(String extension) {
		List<MediaType> mediaTypes = null;
		try {
			mediaTypes = this.pathStrategy.resolveMediaTypeKey(null, extension);
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			// Ignore
		}
		if (CollectionUtils.isEmpty(mediaTypes)) {
			return false;
		}
		for (MediaType mediaType : mediaTypes) {
			if (!safeMediaType(mediaType)) {
				return false;
			}
		}
		return true;
	}

	private boolean safeMediaType(MediaType mediaType) {
		return (WHITELISTED_MEDIA_BASE_TYPES.contains(mediaType.getType()) ||
				mediaType.getSubtype().endsWith("+xml"));
	}

}
