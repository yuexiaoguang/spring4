package org.springframework.web.portlet;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * Interface to be implemented by objects than can resolve exceptions thrown
 * during handler mapping or execution, in the typical case to error views.
 * Implementors are typically registered as beans in the application context.
 *
 * <p>Error views are analogous to the error page JSPs, but can be used with
 * any kind of exception including any checked exception, with potentially
 * fine-granular mappings for specific handlers.
 */
public interface HandlerExceptionResolver {

	/**
	 * Try to resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler the executed handler, or null if none chosen at the time of
	 * the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to,
	 * or {@code null} for default processing
	 */
	ModelAndView resolveException(
			RenderRequest request, RenderResponse response, Object handler, Exception ex);

	/**
	 * Try to resolve the given exception that got thrown during on handler execution,
	 * returning a ModelAndView that represents a specific error page if appropriate.
	 * @param request current portlet request
	 * @param response current portlet response
	 * @param handler the executed handler, or null if none chosen at the time of
	 * the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to,
	 * or {@code null} for default processing
	 */
	ModelAndView resolveException(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex);

}
