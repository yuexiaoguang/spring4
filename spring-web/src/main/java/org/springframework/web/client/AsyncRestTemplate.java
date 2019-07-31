package org.springframework.web.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.InterceptingAsyncHttpAccessor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import org.springframework.web.util.AbstractUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * <strong>异步客户端HTTP访问的中心类.</strong>
 * 暴露与{@link RestTemplate}类似的方法, 但返回{@link ListenableFuture}包装器而不是具体结果.
 *
 * <p>{@code AsyncRestTemplate}通过{@link #getRestOperations()}方法公开同步{@link RestTemplate},
 * 并与此{@code RestTemplate}共享其{@linkplain #setErrorHandler 错误处理器}
 * 和{@linkplain #setMessageConverters 消息转换器}.
 *
 * <p><strong>Note:</strong> 默认情况下, {@code AsyncRestTemplate}依赖于标准的JDK工具来建立HTTP连接.
 * 可以通过使用接受{@link AsyncClientHttpRequestFactory}的构造函数切换到使用不同的HTTP库,
 * 例如Apache HttpComponents, Netty, 和OkHttp.
 *
 * <p>有关更多信息, 请参阅{@link RestTemplate} API文档.
 */
public class AsyncRestTemplate extends InterceptingAsyncHttpAccessor implements AsyncRestOperations {

	private final RestTemplate syncTemplate;


	/**
	 * <p>此构造函数使用{@link SimpleClientHttpRequestFactory}和{@link SimpleAsyncTaskExecutor}进行异步执行.
	 */
	public AsyncRestTemplate() {
		this(new SimpleAsyncTaskExecutor());
	}

	/**
	 * 使用给定的{@link AsyncTaskExecutor}.
	 * <p>此构造函数使用{@link SimpleClientHttpRequestFactory}结合给定的{@code AsyncTaskExecutor}进行异步执行.
	 */
	public AsyncRestTemplate(AsyncListenableTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "AsyncTaskExecutor must not be null");
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setTaskExecutor(taskExecutor);
		this.syncTemplate = new RestTemplate(requestFactory);
		setAsyncRequestFactory(requestFactory);
	}

	/**
	 * <p>此构造函数将给定的异步{@code AsyncClientHttpRequestFactory}转换为{@link ClientHttpRequestFactory}.
	 * 由于Spring中提供的{@code ClientHttpRequestFactory}的所有实现都实现了{@code AsyncClientHttpRequestFactory},
	 * 因此不应该导致{@code ClassCastException}.
	 */
	public AsyncRestTemplate(AsyncClientHttpRequestFactory asyncRequestFactory) {
		this(asyncRequestFactory, (ClientHttpRequestFactory) asyncRequestFactory);
	}

	/**
	 * @param asyncRequestFactory 异步请求工厂
	 * @param syncRequestFactory 同步请求工厂
	 */
	public AsyncRestTemplate(
			AsyncClientHttpRequestFactory asyncRequestFactory, ClientHttpRequestFactory syncRequestFactory) {

		this(asyncRequestFactory, new RestTemplate(syncRequestFactory));
	}

	/**
	 * @param requestFactory 要使用的异步请求工厂
	 * @param restTemplate 要使用的同步模板
	 */
	public AsyncRestTemplate(AsyncClientHttpRequestFactory requestFactory, RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		this.syncTemplate = restTemplate;
		setAsyncRequestFactory(requestFactory);
	}


	/**
	 * 设置错误处理器.
	 * <p>默认, AsyncRestTemplate使用
	 * {@link org.springframework.web.client.DefaultResponseErrorHandler}.
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.syncTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * 返回错误处理器.
	 */
	public ResponseErrorHandler getErrorHandler() {
		return this.syncTemplate.getErrorHandler();
	}

	/**
	 * 配置默认URI变量值. 这是一个快捷方式:
	 * <pre class="code">
	 * DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
	 * handler.setDefaultUriVariables(...);
	 *
	 * AsyncRestTemplate restTemplate = new AsyncRestTemplate();
	 * restTemplate.setUriTemplateHandler(handler);
	 * </pre>
	 * 
	 * @param defaultUriVariables 默认的URI变量值
	 */
	public void setDefaultUriVariables(Map<String, ?> defaultUriVariables) {
		UriTemplateHandler handler = this.syncTemplate.getUriTemplateHandler();
		Assert.isInstanceOf(AbstractUriTemplateHandler.class, handler,
				"Can only use this property in conjunction with a DefaultUriTemplateHandler");
		((AbstractUriTemplateHandler) handler).setDefaultUriVariables(defaultUriVariables);
	}

	/**
	 * 此属性与{@code RestTemplate}上的相应属性具有相同的目的.
	 * 有关更多详细信息, 请参阅{@link RestTemplate#setUriTemplateHandler}.
	 * 
	 * @param handler 要使用的URI模板处理器
	 */
	public void setUriTemplateHandler(UriTemplateHandler handler) {
		this.syncTemplate.setUriTemplateHandler(handler);
	}

	/**
	 * 返回配置的URI模板处理器.
	 */
	public UriTemplateHandler getUriTemplateHandler() {
		return this.syncTemplate.getUriTemplateHandler();
	}

	@Override
	public RestOperations getRestOperations() {
		return this.syncTemplate;
	}

	/**
	 * 设置要使用的消息正文转换器.
	 * <p>这些转换器用于转换HTTP请求和响应.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.syncTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * 返回要使用的消息正文转换器.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.syncTemplate.getMessageConverters();
	}


	// GET

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(String url, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> getForEntity(URI url, Class<T> responseType) throws RestClientException {
		AsyncRequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
	}


	// HEAD

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(String url, Object... uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor, uriVariables);
	}

	@Override
	public ListenableFuture<HttpHeaders> headForHeaders(URI url) throws RestClientException {
		ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
		return execute(url, HttpMethod.HEAD, null, headersExtractor);
	}


	// POST

	@Override
	public ListenableFuture<URI> postForLocation(String url, HttpEntity<?> request, Object... uriVars)
			throws RestClientException {

		AsyncRequestCallback callback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor, uriVars);
		return adaptToLocationHeader(future);
	}

	@Override
	public ListenableFuture<URI> postForLocation(String url, HttpEntity<?> request, Map<String, ?> uriVars)
			throws RestClientException {

		AsyncRequestCallback callback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor, uriVars);
		return adaptToLocationHeader(future);
	}

	@Override
	public ListenableFuture<URI> postForLocation(URI url, HttpEntity<?> request) throws RestClientException {
		AsyncRequestCallback callback = httpEntityCallback(request);
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.POST, callback, extractor);
		return adaptToLocationHeader(future);
	}

	private static ListenableFuture<URI> adaptToLocationHeader(ListenableFuture<HttpHeaders> future) {
		return new ListenableFutureAdapter<URI, HttpHeaders>(future) {
			@Override
			protected URI adapt(HttpHeaders headers) throws ExecutionException {
				return headers.getLocation();
			}
		};
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Object... uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(String url, HttpEntity<?> request,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> postForEntity(URI url, HttpEntity<?> request, Class<T> responseType)
			throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(request, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
	}


	// PUT

	@Override
	public ListenableFuture<?> put(String url, HttpEntity<?> request, Object... uriVariables) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> put(String url, HttpEntity<?> request, Map<String, ?> uriVariables) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> put(URI url, HttpEntity<?> request) throws RestClientException {
		AsyncRequestCallback requestCallback = httpEntityCallback(request);
		return execute(url, HttpMethod.PUT, requestCallback, null);
	}


	// DELETE

	@Override
	public ListenableFuture<?> delete(String url, Object... uriVariables) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> delete(String url, Map<String, ?> uriVariables) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null, uriVariables);
	}

	@Override
	public ListenableFuture<?> delete(URI url) throws RestClientException {
		return execute(url, HttpMethod.DELETE, null, null);
	}


	// OPTIONS

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Object... uriVars) throws RestClientException {
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor, uriVars);
		return adaptToAllowHeader(future);
	}

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(String url, Map<String, ?> uriVars) throws RestClientException {
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor, uriVars);
		return adaptToAllowHeader(future);
	}

	@Override
	public ListenableFuture<Set<HttpMethod>> optionsForAllow(URI url) throws RestClientException {
		ResponseExtractor<HttpHeaders> extractor = headersExtractor();
		ListenableFuture<HttpHeaders> future = execute(url, HttpMethod.OPTIONS, null, extractor);
		return adaptToAllowHeader(future);
	}

	private static ListenableFuture<Set<HttpMethod>> adaptToAllowHeader(ListenableFuture<HttpHeaders> future) {
		return new ListenableFutureAdapter<Set<HttpMethod>, HttpHeaders>(future) {
			@Override
			protected Set<HttpMethod> adapt(HttpHeaders headers) throws ExecutionException {
				return headers.getAllow();
			}
		};
	}

	// exchange

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
			Class<T> responseType, Object... uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
			Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
			Class<T> responseType) throws RestClientException {

		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return execute(url, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {

		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException {

		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor, uriVariables);
	}

	@Override
	public <T> ListenableFuture<ResponseEntity<T>> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType) throws RestClientException {

		Type type = responseType.getType();
		AsyncRequestCallback requestCallback = httpEntityCallback(requestEntity, type);
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
		return execute(url, method, requestCallback, responseExtractor);
	}


	// general execution

	@Override
	public <T> ListenableFuture<T> execute(String url, HttpMethod method, AsyncRequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {

		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<T> execute(String url, HttpMethod method, AsyncRequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables) throws RestClientException {

		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}

	@Override
	public <T> ListenableFuture<T> execute(URI url, HttpMethod method, AsyncRequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException {

		return doExecute(url, method, requestCallback, responseExtractor);
	}

	/**
	 * 在提供的URI上执行给定方法.
	 * 使用{@link RequestCallback}处理{@link org.springframework.http.client.ClientHttpRequest};
	 * {@link ResponseExtractor}的响应.
	 * 
	 * @param url 要连接的完全展开的URL
	 * @param method 要执行的HTTP方法 (GET, POST, etc.)
	 * @param requestCallback 准备请求的对象 (can be {@code null})
	 * @param responseExtractor 从响应中提取返回值的对象 (can be {@code null})
	 * 
	 * @return 任意的对象, 由{@link ResponseExtractor}返回
	 */
	protected <T> ListenableFuture<T> doExecute(URI url, HttpMethod method, AsyncRequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "'url' must not be null");
		Assert.notNull(method, "'method' must not be null");
		try {
			AsyncClientHttpRequest request = createAsyncRequest(url, method);
			if (requestCallback != null) {
				requestCallback.doWithRequest(request);
			}
			ListenableFuture<ClientHttpResponse> responseFuture = request.executeAsync();
			return new ResponseExtractorFuture<T>(method, url, responseFuture, responseExtractor);
		}
		catch (IOException ex) {
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + url + "\":" + ex.getMessage(), ex);
		}
	}

	private void logResponseStatus(HttpMethod method, URI url, ClientHttpResponse response) {
		if (logger.isDebugEnabled()) {
			try {
				logger.debug("Async " + method.name() + " request for \"" + url + "\" resulted in " +
						response.getRawStatusCode() + " (" + response.getStatusText() + ")");
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}

	private void handleResponseError(HttpMethod method, URI url, ClientHttpResponse response) throws IOException {
		if (logger.isWarnEnabled()) {
			try {
				logger.warn("Async " + method.name() + " request for \"" + url + "\" resulted in " +
						response.getRawStatusCode() + " (" + response.getStatusText() + "); invoking error handler");
			}
			catch (IOException ex) {
				// ignore
			}
		}
		getErrorHandler().handleError(response);
	}

	/**
	 * 返回一个请求回调实现, 该实现根据给定的响应类型和配置的{@linkplain #getMessageConverters() 消息转换器}准备请求.
	 */
	protected <T> AsyncRequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.acceptHeaderRequestCallback(responseType));
	}

	/**
	 * 返回将给定对象写入请求流的请求回调实现.
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(HttpEntity<T> requestBody) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(requestBody));
	}

	/**
	 * 返回将给定对象写入请求流的请求回调实现.
	 */
	protected <T> AsyncRequestCallback httpEntityCallback(HttpEntity<T> request, Type responseType) {
		return new AsyncRequestCallbackAdapter(this.syncTemplate.httpEntityCallback(request, responseType));
	}

	/**
	 * 返回{@link ResponseEntity}的响应提取器.
	 */
	protected <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
		return this.syncTemplate.responseEntityExtractor(responseType);
	}

	/**
	 * 返回{@link HttpHeaders}的响应提取器.
	 */
	protected ResponseExtractor<HttpHeaders> headersExtractor() {
		return this.syncTemplate.headersExtractor();
	}


	/**
	 * {@link #doExecute(URI, HttpMethod, AsyncRequestCallback, ResponseExtractor)}返回的Future
	 */
	private class ResponseExtractorFuture<T> extends ListenableFutureAdapter<T, ClientHttpResponse> {

		private final HttpMethod method;

		private final URI url;

		private final ResponseExtractor<T> responseExtractor;

		public ResponseExtractorFuture(HttpMethod method, URI url,
				ListenableFuture<ClientHttpResponse> clientHttpResponseFuture, ResponseExtractor<T> responseExtractor) {
			super(clientHttpResponseFuture);
			this.method = method;
			this.url = url;
			this.responseExtractor = responseExtractor;
		}

		@Override
		protected final T adapt(ClientHttpResponse response) throws ExecutionException {
			try {
				if (!getErrorHandler().hasError(response)) {
					logResponseStatus(this.method, this.url, response);
				}
				else {
					handleResponseError(this.method, this.url, response);
				}
				return convertResponse(response);
			}
			catch (Throwable ex) {
				throw new ExecutionException(ex);
			}
			finally {
				if (response != null) {
					response.close();
				}
			}
		}

		protected T convertResponse(ClientHttpResponse response) throws IOException {
			return (this.responseExtractor != null ? this.responseExtractor.extractData(response) : null);
		}
	}


	/**
	 * 将{@link RequestCallback}适配为{@link AsyncRequestCallback}接口.
	 */
	private static class AsyncRequestCallbackAdapter implements AsyncRequestCallback {

		private final RequestCallback adaptee;

		/**
		 * @param requestCallback 基于此适配器的回调
		 */
		public AsyncRequestCallbackAdapter(RequestCallback requestCallback) {
			this.adaptee = requestCallback;
		}

		@Override
		public void doWithRequest(final AsyncClientHttpRequest request) throws IOException {
			if (this.adaptee != null) {
				this.adaptee.doWithRequest(new ClientHttpRequest() {
					@Override
					public ClientHttpResponse execute() throws IOException {
						throw new UnsupportedOperationException("execute not supported");
					}
					@Override
					public OutputStream getBody() throws IOException {
						return request.getBody();
					}
					@Override
					public HttpMethod getMethod() {
						return request.getMethod();
					}
					@Override
					public URI getURI() {
						return request.getURI();
					}
					@Override
					public HttpHeaders getHeaders() {
						return request.getHeaders();
					}
				});
			}
		}
	}
}
