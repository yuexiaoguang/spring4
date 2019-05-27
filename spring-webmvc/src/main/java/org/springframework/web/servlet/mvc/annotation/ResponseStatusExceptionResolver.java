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
 * A {@link org.springframework.web.servlet.HandlerExceptionResolver
 * HandlerExceptionResolver} that uses the {@link ResponseStatus @ResponseStatus}
 * annotation to map exceptions to HTTP status codes.
 *
 * <p>This exception resolver is enabled by default in the
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
 * and the MVC Java config and the MVC namespace.
 *
 * <p>As of 4.2 this resolver also looks recursively for {@code @ResponseStatus}
 * present on cause exceptions, and as of 4.2.2 this resolver supports
 * attribute overrides for {@code @ResponseStatus} in custom composed annotations.
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
	 * Template method that handles the {@link ResponseStatus @ResponseStatus} annotation.
	 * <p>The default implementation sends a response error using
	 * {@link HttpServletResponse#sendError(int)} or
	 * {@link HttpServletResponse#sendError(int, String)} if the annotation has a
	 * {@linkplain ResponseStatus#reason() reason} and then returns an empty ModelAndView.
	 * @param responseStatus the annotation
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the
	 * time of the exception, e.g. if multipart resolution failed
	 * @param ex the exception
	 * @return an empty ModelAndView, i.e. exception resolved
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
