package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequest}对象的工厂.
 * 由{@link #createRequest(URI, HttpMethod)}方法创建请求.
 */
public interface ClientHttpRequestFactory {

	/**
	 * 为指定的URI和HTTP方法创建一个新的{@link ClientHttpRequest}.
	 * <p>返回的请求可以被写入, 然后通过调用{@link ClientHttpRequest#execute()}来执行.
	 * 
	 * @param uri 用于创建请求的URI
	 * @param httpMethod 要执行的HTTP方法
	 * 
	 * @return 创建的请求
	 * @throws IOException
	 */
	ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
