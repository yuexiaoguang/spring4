package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;

/**
 * 将请求与预期匹配的约定.
 *
 * <p>有关静态工厂方法, 请参阅{@link org.springframework.test.web.client.match.MockRestRequestMatchers MockRestRequestMatchers}.
 */
public interface RequestMatcher {

	/**
	 * 将给定的请求与特定预期相匹配.
	 * 
	 * @param request 要断言的请求
	 * 
	 * @throws IOException 发生I/O错误
	 * @throws AssertionError 如果没有达到预期
	 */
	void match(ClientHttpRequest request) throws IOException, AssertionError;

}
