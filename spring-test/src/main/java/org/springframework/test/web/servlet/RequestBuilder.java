package org.springframework.test.web.servlet;

import javax.servlet.ServletContext;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 构建一个{@link MockHttpServletRequest}.
 *
 * <p>请参阅
 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders MockMvcRequestBuilders}中的静态工厂方法.
 */
public interface RequestBuilder {

	/**
	 * 构建请求.
	 * 
	 * @param servletContext 用于创建请求的{@link ServletContext}
	 * 
	 * @return 请求
	 */
	MockHttpServletRequest buildRequest(ServletContext servletContext);

}
