package org.springframework.http.client;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;

/**
 * Wrapper for a {@link AsyncClientHttpRequestFactory} that has support for
 * {@link AsyncClientHttpRequestInterceptor}s.
 */
public class InterceptingAsyncClientHttpRequestFactory implements AsyncClientHttpRequestFactory {

	private AsyncClientHttpRequestFactory delegate;

	private List<AsyncClientHttpRequestInterceptor> interceptors;


	/**
	 * Create new instance of {@link InterceptingAsyncClientHttpRequestFactory}
	 * with delegated request factory and list of interceptors.
	 * @param delegate the request factory to delegate to
	 * @param interceptors the list of interceptors to use
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
