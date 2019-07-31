package org.springframework.http.client;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;

/**
 * 支持{@link AsyncClientHttpRequestInterceptor}的{@link AsyncClientHttpRequestFactory}的包装器.
 */
public class InterceptingAsyncClientHttpRequestFactory implements AsyncClientHttpRequestFactory {

	private AsyncClientHttpRequestFactory delegate;

	private List<AsyncClientHttpRequestInterceptor> interceptors;


	/**
	 * @param delegate 要委托给的请求工厂
	 * @param interceptors 要使用的拦截器列表
	 */
	public InterceptingAsyncClientHttpRequestFactory(AsyncClientHttpRequestFactory delegate,
			List<AsyncClientHttpRequestInterceptor> interceptors) {

		this.delegate = delegate;
		this.interceptors = (interceptors != null ? interceptors : Collections.<AsyncClientHttpRequestInterceptor>emptyList());
	}


	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod method) {
		return new InterceptingAsyncClientHttpRequest(this.delegate, this.interceptors, uri, method);
	}

}
