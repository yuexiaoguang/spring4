package org.springframework.test.web.servlet;

import javax.servlet.ServletContext;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Builds a {@link MockHttpServletRequest}.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders MockMvcRequestBuilders}.
 */
public interface RequestBuilder {

	/**
	 * Build the request.
	 * @param servletContext the {@link ServletContext} to use to create the request
	 * @return the request
	 */
	MockHttpServletRequest buildRequest(ServletContext servletContext);

}
