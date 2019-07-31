package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequestFactory}实现的抽象基类, 装饰另一个请求工厂.
 */
public abstract class AbstractClientHttpRequestFactoryWrapper implements ClientHttpRequestFactory {

	private final ClientHttpRequestFactory requestFactory;


	/**
	 * @param requestFactory 要包装的请求工厂
	 */
	protected AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
		this.requestFactory = requestFactory;
	}


	/**
	 * 此实现只是使用提供给
	 * {@linkplain #AbstractClientHttpRequestFactoryWrapper(ClientHttpRequestFactory) 构造函数}
	 * 的包装的请求工厂调用{@link #createRequest(URI, HttpMethod, ClientHttpRequestFactory)}.
	 */
	@Override
	public final ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequest(uri, httpMethod, this.requestFactory);
	}

	/**
	 * 使用传递的请求工厂为指定的URI和HTTP方法创建新的{@link ClientHttpRequest}.
	 * <p>从{@link #createRequest(URI, HttpMethod)}调用.
	 * 
	 * @param uri 用于创建请求的URI
	 * @param httpMethod 要执行的HTTP方法
	 * @param requestFactory 包装的请求工厂
	 * 
	 * @return 创建的请求
	 * @throws IOException
	 */
	protected abstract ClientHttpRequest createRequest(
			URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) throws IOException;

}
