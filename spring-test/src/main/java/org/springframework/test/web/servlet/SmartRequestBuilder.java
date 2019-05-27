package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Extended variant of a {@link RequestBuilder} that applies its
 * {@link org.springframework.test.web.servlet.request.RequestPostProcessor}s
 * as a separate step from the {@link #buildRequest} method.
 */
public interface SmartRequestBuilder extends RequestBuilder {

	/**
	 * Apply request post processing. Typically that means invoking one or more
	 * {@link org.springframework.test.web.servlet.request.RequestPostProcessor}s.
	 *
	 * @param request the request to initialize
	 * @return the request to use, either the one passed in or a wrapped one
	 */
	MockHttpServletRequest postProcessRequest(MockHttpServletRequest request);

}
