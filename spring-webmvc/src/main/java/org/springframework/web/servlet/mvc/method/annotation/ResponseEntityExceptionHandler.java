package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.WebUtils;

/**
 * {@link ControllerAdvice @ControllerAdvice}类的便捷基类,
 * 希望通过{@code @ExceptionHandler}方法跨所有{@code @RequestMapping}方法提供集中式异常处理.
 *
 * <p>此基类提供了一个{@code @ExceptionHandler}方法来处理内部Spring MVC异常.
 * 此方法返回{@code ResponseEntity}, 用于使用{@link HttpMessageConverter 消息转换器}写入响应,
 * 而{@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 * DefaultHandlerExceptionResolver}则返回
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView}.
 *
 * <p>如果不需要将错误内容写入响应正文, 或者使用视图解析 (e.g., 通过{@code ContentNegotiatingViewResolver}),
 * 那么{@code DefaultHandlerExceptionResolver}就足够了.
 *
 * <p>请注意, 为了检测{@code @ControllerAdvice}子类, 必须配置{@link ExceptionHandlerExceptionResolver}.
 */
public abstract class ResponseEntityExceptionHandler {

	/**
	 * 未找到请求的映射处理器时要使用的日志类别.
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 未找到请求的映射处理器时使用的特定记录器.
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	/**
	 * Common logger for use in subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * 提供标准Spring MVC异常的处理.
	 * 
	 * @param ex 目标异常
	 * @param request 当前的请求
	 */
	@SuppressWarnings("deprecation")
	@ExceptionHandler({
			org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException.class,
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			HttpMediaTypeNotAcceptableException.class,
			MissingPathVariableException.class,
			MissingServletRequestParameterException.class,
			ServletRequestBindingException.class,
			ConversionNotSupportedException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			HttpMessageNotWritableException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestPartException.class,
			BindException.class,
			NoHandlerFoundException.class,
			AsyncRequestTimeoutException.class
		})
	public final ResponseEntity<Object> handleException(Exception ex, WebRequest request) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		if (ex instanceof org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException) {
			HttpStatus status = HttpStatus.NOT_FOUND;
			return handleNoSuchRequestHandlingMethod((org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException) ex, headers, status, request);
		}
		else if (ex instanceof HttpRequestMethodNotSupportedException) {
			HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
			return handleHttpRequestMethodNotSupported((HttpRequestMethodNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotSupportedException) {
			HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
			return handleHttpMediaTypeNotSupported((HttpMediaTypeNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			HttpStatus status = HttpStatus.NOT_ACCEPTABLE;
			return handleHttpMediaTypeNotAcceptable((HttpMediaTypeNotAcceptableException) ex, headers, status, request);
		}
		else if (ex instanceof MissingPathVariableException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleMissingPathVariable((MissingPathVariableException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestParameterException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestParameter((MissingServletRequestParameterException) ex, headers, status, request);
		}
		else if (ex instanceof ServletRequestBindingException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleServletRequestBindingException((ServletRequestBindingException) ex, headers, status, request);
		}
		else if (ex instanceof ConversionNotSupportedException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleConversionNotSupported((ConversionNotSupportedException) ex, headers, status, request);
		}
		else if (ex instanceof TypeMismatchException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleTypeMismatch((TypeMismatchException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotReadableException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, headers, status, request);
		}
		else if (ex instanceof HttpMessageNotWritableException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, headers, status, request);
		}
		else if (ex instanceof MethodArgumentNotValidException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMethodArgumentNotValid((MethodArgumentNotValidException) ex, headers, status, request);
		}
		else if (ex instanceof MissingServletRequestPartException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestPart((MissingServletRequestPartException) ex, headers, status, request);
		}
		else if (ex instanceof BindException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleBindException((BindException) ex, headers, status, request);
		}
		else if (ex instanceof NoHandlerFoundException) {
			HttpStatus status = HttpStatus.NOT_FOUND;
			return handleNoHandlerFoundException((NoHandlerFoundException) ex, headers, status, request);
		}
		else if (ex instanceof AsyncRequestTimeoutException) {
			HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
			return handleAsyncRequestTimeoutException((AsyncRequestTimeoutException) ex, headers, status, request);
		}
		else {
			// 未知异常, 通常是具有常见MVC异常的包装器 (因为 @ExceptionHandler 类型声明也匹配第一级原因):
			// 这里只处理顶级MVC异常, 所以重新抛出给定的异常, 以便通过HandlerExceptionResolver链进一步处理.
			throw ex;
		}
	}

	/**
	 * 自定义NoSuchRequestHandlingMethodException的响应.
	 * <p>此方法记录警告并委托给{@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 * @deprecated as of 4.3, along with {@link org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException}
	 */
	@Deprecated
	protected ResponseEntity<Object> handleNoSuchRequestHandlingMethod(
			org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义HttpRequestMethodNotSupportedException的响应.
	 * <p>此方法记录警告, 设置"Allow" header, 并委托给{@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		pageNotFoundLogger.warn(ex.getMessage());

		Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
		if (!CollectionUtils.isEmpty(supportedMethods)) {
			headers.setAllow(supportedMethods);
		}
		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义HttpMediaTypeNotSupportedException的响应.
	 * <p>此方法设置"Accept" header并委托给{@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			headers.setAccept(mediaTypes);
		}

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义HttpMediaTypeNotAcceptableException的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
			HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingPathVariableException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleMissingPathVariable(
			MissingPathVariableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingServletRequestParameterException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 ServletRequestBindingException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleServletRequestBindingException(
			ServletRequestBindingException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 ConversionNotSupportedException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleConversionNotSupported(
			ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 TypeMismatchException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMessageNotReadableException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMessageNotWritableException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleHttpMessageNotWritable(
			HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MethodArgumentNotValidException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingServletRequestPartException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleMissingServletRequestPart(
			MissingServletRequestPartException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 BindException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleBindException(
			BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 NoHandlerFoundException 的响应.
	 * <p>此方法委托给{@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param request 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleNoHandlerFoundException(
			NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 NoHandlerFoundException 的响应.
	 * <p>此方法委托给 {@link #handleExceptionInternal}.
	 * 
	 * @param ex 异常
	 * @param headers 要写入响应的header
	 * @param status 选定的响应状态
	 * @param webRequest 当前的请求
	 * 
	 * @return {@code ResponseEntity}实例
	 */
	protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
			AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatus status, WebRequest webRequest) {

		if (webRequest instanceof ServletWebRequest) {
			ServletWebRequest servletRequest = (ServletWebRequest) webRequest;
			HttpServletRequest request = servletRequest.getNativeRequest(HttpServletRequest.class);
			HttpServletResponse response = servletRequest.getNativeResponse(HttpServletResponse.class);
			if (response.isCommitted()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Async timeout for " + request.getMethod() + " [" + request.getRequestURI() + "]");
				}
				return null;
			}
		}

		return handleExceptionInternal(ex, null, headers, status, webRequest);
	}

	/**
	 * 用于自定义所有异常类型的响应主体的位置.
	 * <p>默认实现设置{@link WebUtils#ERROR_EXCEPTION_ATTRIBUTE}请求属性,
	 * 并从给定正文, header, 和状态创建{@link ResponseEntity}.
	 * 
	 * @param ex 异常
	 * @param body 响应的主体
	 * @param headers 响应的header
	 * @param status 响应状态
	 * @param request 当前的请求
	 */
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
			request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
		}
		return new ResponseEntity<Object>(body, headers, status);
	}
}
