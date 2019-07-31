package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link AsyncClientHttpRequest}包装器, 丰富了它, 通过调用已注册的拦截器继续实际的请求执行.
 */
class InterceptingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private AsyncClientHttpRequestFactory requestFactory;

	private List<AsyncClientHttpRequestInterceptor> interceptors;

	private URI uri;

	private HttpMethod httpMethod;


	/**
	 * @param requestFactory 异步请求工厂
	 * @param interceptors 拦截器列表
	 * @param uri 请求URI
	 * @param httpMethod HTTP方法
	 */
	public InterceptingAsyncClientHttpRequest(AsyncClientHttpRequestFactory requestFactory,
			List<AsyncClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod httpMethod) {

		this.requestFactory = requestFactory;
		this.interceptors = interceptors;
		this.uri = uri;
		this.httpMethod = httpMethod;
	}


	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers, byte[] body)
			throws IOException {

		return new AsyncRequestExecution().executeAsync(this, body);
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return uri;
	}


	private class AsyncRequestExecution implements AsyncClientHttpRequestExecution {

		private Iterator<AsyncClientHttpRequestInterceptor> iterator;

		public AsyncRequestExecution() {
			this.iterator = interceptors.iterator();
		}

		@Override
		public ListenableFuture<ClientHttpResponse> executeAsync(HttpRequest request, byte[] body)
				throws IOException {

			if (this.iterator.hasNext()) {
				AsyncClientHttpRequestInterceptor interceptor = this.iterator.next();
				return interceptor.intercept(request, body, this);
			}
			else {
				URI uri = request.getURI();
				HttpMethod method = request.getMethod();
				HttpHeaders headers = request.getHeaders();

				AsyncClientHttpRequest delegate = requestFactory.createAsyncRequest(uri, method);
				delegate.getHeaders().putAll(headers);
				if (body.length > 0) {
					StreamUtils.copy(body, delegate.getBody());
				}

				return delegate.executeAsync();
			}
		}
	}

}
