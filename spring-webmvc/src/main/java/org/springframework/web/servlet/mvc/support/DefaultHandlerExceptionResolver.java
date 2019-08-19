package org.springframework.web.servlet.mvc.support;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver}接口的默认实现,
 * 解析标准的Spring MVC异常并将它们转换为相应的HTTP状态码.
 *
 * <p>默认情况下, 在常见的Spring {@link org.springframework.web.servlet.DispatcherServlet}中启用此异常解析器.
 */
public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 未找到请求的映射处理器时要使用的日志类别.
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 未找到请求的映射处理器时使用的其他记录器.
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);


	/**
	 * 将{@linkplain #setOrder(int) 顺序}设置为{@link #LOWEST_PRECEDENCE}.
	 */
	public DefaultHandlerExceptionResolver() {
		setOrder(Ordered.LOWEST_PRECEDENCE);
	}


	@Override
	@SuppressWarnings("deprecation")
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

		try {
			if (ex instanceof org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException) {
				return handleNoSuchRequestHandlingMethod(
						(org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException) ex,
						request, response, handler);
			}
			else if (ex instanceof HttpRequestMethodNotSupportedException) {
				return handleHttpRequestMethodNotSupported(
						(HttpRequestMethodNotSupportedException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMediaTypeNotSupportedException) {
				return handleHttpMediaTypeNotSupported(
						(HttpMediaTypeNotSupportedException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMediaTypeNotAcceptableException) {
				return handleHttpMediaTypeNotAcceptable(
						(HttpMediaTypeNotAcceptableException) ex, request, response, handler);
			}
			else if (ex instanceof MissingPathVariableException) {
				return handleMissingPathVariable(
						(MissingPathVariableException) ex, request, response, handler);
			}
			else if (ex instanceof MissingServletRequestParameterException) {
				return handleMissingServletRequestParameter(
						(MissingServletRequestParameterException) ex, request, response, handler);
			}
			else if (ex instanceof ServletRequestBindingException) {
				return handleServletRequestBindingException(
						(ServletRequestBindingException) ex, request, response, handler);
			}
			else if (ex instanceof ConversionNotSupportedException) {
				return handleConversionNotSupported(
						(ConversionNotSupportedException) ex, request, response, handler);
			}
			else if (ex instanceof TypeMismatchException) {
				return handleTypeMismatch(
						(TypeMismatchException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMessageNotReadableException) {
				return handleHttpMessageNotReadable(
						(HttpMessageNotReadableException) ex, request, response, handler);
			}
			else if (ex instanceof HttpMessageNotWritableException) {
				return handleHttpMessageNotWritable(
						(HttpMessageNotWritableException) ex, request, response, handler);
			}
			else if (ex instanceof MethodArgumentNotValidException) {
				return handleMethodArgumentNotValidException(
						(MethodArgumentNotValidException) ex, request, response, handler);
			}
			else if (ex instanceof MissingServletRequestPartException) {
				return handleMissingServletRequestPartException(
						(MissingServletRequestPartException) ex, request, response, handler);
			}
			else if (ex instanceof BindException) {
				return handleBindException((BindException) ex, request, response, handler);
			}
			else if (ex instanceof NoHandlerFoundException) {
				return handleNoHandlerFoundException(
						(NoHandlerFoundException) ex, request, response, handler);
			}
			else if (ex instanceof AsyncRequestTimeoutException) {
				return handleAsyncRequestTimeoutException(
						(AsyncRequestTimeoutException) ex, request, response, handler);
			}
		}
		catch (Exception handlerException) {
			if (logger.isWarnEnabled()) {
				logger.warn("Handling of [" + ex.getClass().getName() + "] resulted in exception", handlerException);
			}
		}
		return null;
	}

	/**
	 * 处理未找到请求处理器方法的情况.
	 * <p>默认实现记录警告, 发送HTTP 404错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出NoSuchRequestHandlingMethodException.
	 * 
	 * @param ex 要处理的NoSuchRequestHandlingMethodException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 * @deprecated as of 4.3, along with {@link org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException}
	 */
	@Deprecated
	protected ModelAndView handleNoSuchRequestHandlingMethod(org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		pageNotFoundLogger.warn(ex.getMessage());
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		return new ModelAndView();
	}

	/**
	 * 处理未找到特定HTTP请求方法的请求处理器方法的情况.
	 * <p>默认实现记录警告, 发送HTTP 405错误, 设置"Allow" header, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出HttpRequestMethodNotSupportedException.
	 * 
	 * @param ex 要处理的HttpRequestMethodNotSupportedException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		pageNotFoundLogger.warn(ex.getMessage());
		String[] supportedMethods = ex.getSupportedMethods();
		if (supportedMethods != null) {
			response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
		}
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理未找到PUT或POST内容的{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}的情况.
	 * <p>默认实现发送HTTP 415错误, 设置"Accept" header, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新引发HttpMediaTypeNotSupportedException.
	 * 
	 * @param ex 要处理的HttpMediaTypeNotSupportedException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			response.setHeader("Accept", MediaType.toString(mediaTypes));
		}
		return new ModelAndView();
	}

	/**
	 * 处理没有找到客户可接受的{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}的情况 (通过{@code Accept} header表示).
	 * <p>默认实现发送HTTP 406错误并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出HttpMediaTypeNotAcceptableException.
	 * 
	 * @param ex 要处理的HttpMediaTypeNotAcceptableException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
		return new ModelAndView();
	}

	/**
	 * 处理当声明的路径变量与任何提取的URI变量不匹配时的情况.
	 * <p>默认实现发送HTTP 500错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出MissingPathVariableException.
	 * 
	 * @param ex 要处理的MissingPathVariableException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleMissingPathVariable(MissingPathVariableException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理缺少必需参数时的情况.
	 * <p>默认实现发送HTTP 400错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出 MissingServletRequestParameterException.
	 * 
	 * @param ex 要处理的MissingServletRequestParameterException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理不可恢复的绑定异常时的情况 - e.g. 必需的header, 必需的cookie.
	 * <p>默认实现发送HTTP 400错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出异常.
	 * 
	 * @param ex 要处理的异常
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleServletRequestBindingException(ServletRequestBindingException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理不能发生{@link org.springframework.web.bind.WebDataBinder}转换时的情况.
	 * <p>默认实现发送HTTP 500错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出TypeMismatchException.
	 * 
	 * @param ex 要处理的ConversionNotSupportedException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleConversionNotSupported(ConversionNotSupportedException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		if (logger.isWarnEnabled()) {
			logger.warn("Failed to convert request element: " + ex);
		}
		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * 处理{@link org.springframework.web.bind.WebDataBinder}转换错误时的情况.
	 * <p>默认实现发送HTTP 400错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出TypeMismatchException.
	 * 
	 * @param ex 要处理的TypeMismatchException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		if (logger.isWarnEnabled()) {
			logger.warn("Failed to bind request element: " + ex);
		}
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}无法从HTTP请求中读取的情况.
	 * <p>默认实现发送HTTP 400错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出HttpMediaTypeNotSupportedException.
	 * 
	 * @param ex 要处理的HttpMessageNotReadableException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		if (logger.isWarnEnabled()) {
			logger.warn("Failed to read HTTP message: " + ex);
		}
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}无法写入HTTP请求的情况.
	 * <p>默认实现发送HTTP 500错误, 并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出 HttpMediaTypeNotSupportedException.
	 * 
	 * @param ex 要处理的HttpMessageNotWritableException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		if (logger.isWarnEnabled()) {
			logger.warn("Failed to write HTTP message: " + ex);
		}
		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * 处理使用{@code @Valid}注解的参数(例如{@link RequestBody} 或 {@link RequestPart}参数)无法验证的情况.
	 * HTTP 400错误将发送回客户端.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

 		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理缺少必需的{@linkplain RequestPart @RequestPart}, {@link MultipartFile}, 或{@code javax.servlet.http.Part}参数的情况.
	 * HTTP 400错误将发送回客户端.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleMissingServletRequestPartException(MissingServletRequestPartException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理{@linkplain ModelAttribute @ModelAttribute}方法参数具有绑定或验证错误,
	 * 并且未跟随{@link BindingResult}类型的另一个方法参数的情况.
	 * 默认情况下, 会将HTTP 400错误发送回客户端.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleBindException(BindException ex, HttpServletRequest request,
			HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理在分派期间未找到处理器的情况.
	 * <p>默认实现发送HTTP 404错误并返回空{@code ModelAndView}.
	 * 或者, 可以选择回退视图, 或者可以按原样重新抛出NoHandlerFoundException.
	 * 
	 * @param ex 要处理的NoHandlerFoundException
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		return new ModelAndView();
	}

	/**
	 * 处理异步请求超时的情况.
	 * <p>默认实现发送HTTP 503错误.
	 * 
	 * @param ex 要处理的{@link AsyncRequestTimeoutException }
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * 
	 * @return 一个空的ModelAndView, 指示处理了异常
	 * @throws IOException 可能从 response.sendError() 抛出
	 */
	protected ModelAndView handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
			HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

		if (!response.isCommitted()) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Async timeout for " + request.getMethod() + " [" + request.getRequestURI() + "]");
		}
		return new ModelAndView();
	}

	/**
	 * 调用发送服务器错误.
	 * 将状态设置为500, 并将请求属性"javax.servlet.error.exception"设置为Exception.
	 */
	protected void sendServerError(Exception ex, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		request.setAttribute("javax.servlet.error.exception", ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
}
