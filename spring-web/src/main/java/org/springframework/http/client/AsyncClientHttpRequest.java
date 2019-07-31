package org.springframework.http.client;

import java.io.IOException;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRequest;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 表示客户端异步HTTP请求.
 * 通过{@link AsyncClientHttpRequestFactory}的实现创建.
 *
 * <p>{@code AsyncHttpRequest}可以{@linkplain #executeAsync() 执行}, 获得异步{@link ClientHttpResponse}.
 */
public interface AsyncClientHttpRequest extends HttpRequest, HttpOutputMessage {

	/**
	 * 异步执行此请求, 从而生成Future句柄.
	 * 可以读取的{@link ClientHttpResponse}.
	 * 
	 * @return 执行的异步响应结果
	 * @throws java.io.IOException
	 */
	ListenableFuture<ClientHttpResponse> executeAsync() throws IOException;

}
