package org.springframework.test.web.client;

/**
 * 建立请求预期和定义响应的约定.
 * 可以通过{@link MockRestServiceServer#expect(RequestMatcher)}获取实现.
 */
public interface ResponseActions {

	/**
	 * 添加请求预期.
	 * 
	 * @return 预期
	 */
	ResponseActions andExpect(RequestMatcher requestMatcher);

	/**
	 * 定义响应.
	 * 
	 * @param responseCreator 响应的创建者
	 */
	void andRespond(ResponseCreator responseCreator);

}
