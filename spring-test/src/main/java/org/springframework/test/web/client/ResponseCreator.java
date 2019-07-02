package org.springframework.test.web.client;

import java.io.IOException;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.response.MockRestResponseCreators;

/**
 * 创建{@link ClientHttpResponse}的约定.
 * 可以通过{@link MockRestResponseCreators}获得实现.
 */
public interface ResponseCreator {

	/**
	 * 为给定的请求创建响应.
	 * 
	 * @param request 请求
	 */
	ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException;

}
