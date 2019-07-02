package org.springframework.test.web.servlet.request;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 应用程序或第三方库的扩展点, 希望在{@link MockHttpServletRequestBuilder}
 * 或其子类{@link MockMultipartHttpServletRequestBuilder}构建之后,
 * 进一步初始化{@link MockHttpServletRequest}实例.
 *
 * <p>在即将构建请求时, 可以向{@link MockHttpServletRequestBuilder#with(RequestPostProcessor)}提供此接口的实现.
 */
public interface RequestPostProcessor {

	/**
	 * 在通过{@code MockHttpServletRequestBuilder}创建和初始化之后,
	 * 对给定的{@code MockHttpServletRequest}进行后处理.
	 * 
	 * @param request 要初始化的请求
	 * 
	 * @return 使用的请求, 传入的请求或包装的请求
	 */
	MockHttpServletRequest postProcessRequest(MockHttpServletRequest request);

}
