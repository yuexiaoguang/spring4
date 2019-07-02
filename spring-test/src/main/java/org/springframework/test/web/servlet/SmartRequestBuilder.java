package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * {@link RequestBuilder}的扩展变体, 它将
 * {@link org.springframework.test.web.servlet.request.RequestPostProcessor}作为从{@link #buildRequest}方法分离的步骤.
 */
public interface SmartRequestBuilder extends RequestBuilder {

	/**
	 * 应用请求后处理.
	 * 通常意味着调用一个或多个{@link org.springframework.test.web.servlet.request.RequestPostProcessor}.
	 *
	 * @param request 要初始化的请求
	 * 
	 * @return 使用的请求, 传入的请求或包装的请求
	 */
	MockHttpServletRequest postProcessRequest(MockHttpServletRequest request);

}
