package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides access to the result of an executed request.
 */
public interface MvcResult {

	/**
	 * Return the performed request.
	 * @return the request, never {@code null}
	 */
	MockHttpServletRequest getRequest();

	/**
	 * Return the resulting response.
	 * @return the response, never {@code null}
	 */
	MockHttpServletResponse getResponse();

	/**
	 * Return the executed handler.
	 * @return the handler, possibly {@code null} if none were executed
	 */
	Object getHandler();

	/**
	 * Return interceptors around the handler.
	 * @return interceptors, or {@code null} if none were selected
	 */
	HandlerInterceptor[] getInterceptors();

	/**
	 * Return the {@code ModelAndView} prepared by the handler.
	 * @return a {@code ModelAndView}, or {@code null}
	 */
	ModelAndView getModelAndView();

	/**
	 * Return any exception raised by a handler and successfully resolved
	 * through a {@link HandlerExceptionResolver}.
	 * @return an exception, possibly {@code null}
	 */
 	Exception getResolvedException();

	/**
	 * Return the "output" flash attributes saved during request processing.
	 * @return the {@code FlashMap}, possibly empty
	 */
	FlashMap getFlashMap();

	/**
	 * Get the result of async execution. This method will wait for the async result
	 * to be set for up to the amount of time configured on the async request,
	 * i.e. {@link org.springframework.mock.web.MockAsyncContext#getTimeout()}.
	 * @throws IllegalStateException if the async result was not set
	 */
	Object getAsyncResult();

	/**
	 * Get the result of async execution. This method will wait for the async result
	 * to be set for up to the specified amount of time.
	 * @param timeToWait how long to wait for the async result to be set, in
	 * 	milliseconds; if -1, then the async request timeout value is used,
	 *  i.e.{@link org.springframework.mock.web.MockAsyncContext#getTimeout()}.
	 * @throws IllegalStateException if the async result was not set
	 */
	Object getAsyncResult(long timeToWait);

}
