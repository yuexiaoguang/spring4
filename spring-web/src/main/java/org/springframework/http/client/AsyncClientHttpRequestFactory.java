package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * {@link AsyncClientHttpRequest}对象的工厂.
 * 由{@link #createAsyncRequest(URI, HttpMethod)}方法创建请求.
 */
public interface AsyncClientHttpRequestFactory {

	/**
	 * 为指定的URI和HTTP方法创建新的异步{@link AsyncClientHttpRequest}.
	 * <p>返回的请求可以被写入, 然后通过调用{@link AsyncClientHttpRequest#executeAsync()}来执行.
	 * 
	 * @param uri 用于创建请求的URI
	 * @param httpMethod 要执行的HTTP方法
	 * 
	 * @return 创建的请求
	 * @throws IOException
	 */
	AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException;

}
