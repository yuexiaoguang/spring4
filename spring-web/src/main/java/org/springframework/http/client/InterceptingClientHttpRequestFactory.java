package org.springframework.http.client;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequestFactory}包装器, 支持{@link ClientHttpRequestInterceptor}.
 */
public class InterceptingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	private final List<ClientHttpRequestInterceptor> interceptors;


	/**
	 * @param requestFactory 要包装的请求工厂
	 * @param interceptors 要应用的拦截器 (can be {@code null})
	 */
	public InterceptingClientHttpRequestFactory(ClientHttpRequestFactory requestFactory,
			List<ClientHttpRequestInterceptor> interceptors) {

		super(requestFactory);
		this.interceptors = (interceptors != null ? interceptors : Collections.<ClientHttpRequestInterceptor>emptyList());
	}


	@Override
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory) {
		return new InterceptingClientHttpRequest(requestFactory, this.interceptors, uri, httpMethod);
	}

}
