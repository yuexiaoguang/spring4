package org.springframework.web.servlet.mvc.annotation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver},
 * 使用{@link ResponseStatus @ResponseStatus}注解将异常映射到HTTP状态码.
 *
 * <p>默认情况下, 在{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * 和MVC Java配置以及MVC命名空间中启用此异常解析器.
 *
 * <p>从4.2开始, 这个解析器也会以{@code @ResponseStatus}递归查看原因异常,
 * 从4.2.2开始, 此解析器支持自定义组合注解中{@code @ResponseStatus}的属性覆盖.
 */
public class ResponseStatusExceptionResolver extends AbstractHandlerExceptionResolver implements MessageSourceAware {

	private MessageSource messageSource;


	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}


	@Override
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

		ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
		if (status != null) {
			try {
				return resolveResponseStatus(status, request, response, handler, ex);
			}
			catch (Exception resolveEx) {
				logger.warn("ResponseStatus handling resulted in exception", resolveEx);
			}
		}
		else if (ex.getCause() instanceof Exception) {
			ex = (Exception) ex.getCause();
			return doResolveException(request, response, handler, ex);
		}
		return null;
	}

	/**
	 * 处理{@link ResponseStatus @ResponseStatus} 注解的模板方法.
	 * <p>如果注解具有{@linkplain ResponseStatus#reason() reason},
	 * 则默认实现使用{@link HttpServletResponse#sendError(int)} or
	 * 或{@link HttpServletResponse#sendError(int, String)}发送响应错误, 然后返回空的ModelAndView.
	 * 
	 * @param responseStatus 注解
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null}, 如果在异常时没有选择, e.g. 如果multipart解析失败
	 * @param ex 异常
	 * 
	 * @return 空的ModelAndView, i.e. 解析的异常
	 */
	protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) throws Exception {

		int statusCode = responseStatus.code().value();
		String reason = responseStatus.reason();
		if (!StringUtils.hasLength(reason)) {
			response.sendError(statusCode);
		}
		else {
			String resolvedReason = (this.messageSource != null ?
					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
					reason);
			response.sendError(statusCode, resolvedReason);
		}
		return new ModelAndView();
	}

}
