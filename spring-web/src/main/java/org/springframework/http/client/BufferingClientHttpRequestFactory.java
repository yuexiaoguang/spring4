package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequestFactory}的包装器, 用于缓冲内存中的所有传出和传入流.
 *
 * <p>使用此包装器允许多次读取{@linkplain ClientHttpResponse#getBody() 响应正文}.
 */
public class BufferingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	/**
	 * @param requestFactory 要包装的目标请求工厂
	 */
	public BufferingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
		super(requestFactory);
	}


	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
			throws IOException {

		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		if (shouldBuffer(uri, httpMethod)) {
			return new BufferingClientHttpRequestWrapper(request);
		}
		else {
			return request;
		}
	}

	/**
	 * 指示是否应在内存中缓冲给定URI和方法的请求/响应交换.
	 * <p>对于所有URI和方法, 默认实现返回{@code true}.
	 * 子类可以重写此方法以更改此行为.
	 * 
	 * @param uri the URI
	 * @param httpMethod 方法
	 * 
	 * @return {@code true} 如果交换应该缓冲; 否则{@code false}
	 */
	protected boolean shouldBuffer(URI uri, HttpMethod httpMethod) {
		return true;
	}

}
