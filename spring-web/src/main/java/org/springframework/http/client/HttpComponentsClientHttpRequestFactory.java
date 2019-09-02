package org.springframework.http.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory}实现,
 * 使用<a href="http://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents HttpClient</a>创建请求.
 *
 * <p>允许使用预先配置的{@link HttpClient}实例 - 可能包括身份验证, HTTP连接池等.
 *
 * <p><b>NOTE:</b> 从Spring 4.0开始, 需要Apache HttpComponents 4.3或更高版本.
 */
public class HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

	private static Class<?> abstractHttpClientClass;

	static {
		try {
			// 查找AbstractHttpClient class (deprecated as of HttpComponents 4.3)
			abstractHttpClientClass = ClassUtils.forName("org.apache.http.impl.client.AbstractHttpClient",
					HttpComponentsClientHttpRequestFactory.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// Probably removed from HttpComponents in the meantime...
		}
	}


	private HttpClient httpClient;

	private RequestConfig requestConfig;

	private boolean bufferRequestBody = true;


	public HttpComponentsClientHttpRequestFactory() {
		this.httpClient = HttpClients.createSystem();
	}

	/**
	 * @param httpClient 用于此请求工厂的HttpClient实例
	 */
	public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
		setHttpClient(httpClient);
	}


	/**
	 * 设置用于 {@linkplain #createRequest(URI, HttpMethod) 同步执行}的{@code HttpClient}.
	 */
	public void setHttpClient(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient must not be null");
		this.httpClient = httpClient;
	}

	/**
	 * 返回用于{@linkplain #createRequest(URI, HttpMethod) 同步执行}的{@code HttpClient}.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * 设置底层HttpClient的连接超时.
	 * 值0指定无限超时.
	 * <p>可以通过在自定义{@link HttpClient}上指定{@link RequestConfig}实例来配置其他属性.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public void setConnectTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setConnectTimeout(timeout).build();
		setLegacyConnectionTimeout(getHttpClient(), timeout);
	}

	/**
	 * 将指定的连接超时应用于已弃用的{@link HttpClient}实现.
	 * <p>从HttpClient 4.3开始, 默认参数必须通过{@link RequestConfig}实例公开, 而不是在客户端上设置参数.
	 * 不幸的是, 这种行为不向后兼容, 旧的{@link HttpClient}实现将忽略上下文中设置的{@link RequestConfig}对象.
	 * <p>如果指定的客户端是较旧的实现, 通过弃用的API设置自定义连接超时.
	 * 否则, 通过{@link RequestConfig}设置它, 并与与较新的客户端一起返回.
	 * 
	 * @param client 要配置的客户端
	 * @param timeout 自定义连接超时
	 */
	@SuppressWarnings("deprecation")
	private void setLegacyConnectionTimeout(HttpClient client, int timeout) {
		if (abstractHttpClientClass != null && abstractHttpClientClass.isInstance(client)) {
			client.getParams().setIntParameter(org.apache.http.params.CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
		}
	}

	/**
	 * 设置使用底层HttpClient从连接管理器请求连接时使用的超时, 以毫秒为单位.
	 * 值0指定无限超时.
	 * <p>可以通过在自定义{@link HttpClient}上指定{@link RequestConfig}实例来配置其他属性.
	 * 
	 * @param connectionRequestTimeout 请求连接的超时值, 以毫秒为单位
	 */
	public void setConnectionRequestTimeout(int connectionRequestTimeout) {
		this.requestConfig = requestConfigBuilder().setConnectionRequestTimeout(connectionRequestTimeout).build();
	}

	/**
	 * 设置底层HttpClient的套接字读取超时.
	 * 值0指定无限超时.
	 * <p>可以通过在自定义{@link HttpClient}上指定{@link RequestConfig}实例来配置其他属性.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public void setReadTimeout(int timeout) {
		Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
		this.requestConfig = requestConfigBuilder().setSocketTimeout(timeout).build();
		setLegacySocketTimeout(getHttpClient(), timeout);
	}

	/**
	 * 将指定的套接字超时应用于已弃用的{@link HttpClient}实现.
	 * See {@link #setLegacyConnectionTimeout}.
	 * 
	 * @param client 要配置的客户端
	 * @param timeout 指定的套接字超时
	 */
	@SuppressWarnings("deprecation")
	private void setLegacySocketTimeout(HttpClient client, int timeout) {
		if (abstractHttpClientClass != null && abstractHttpClientClass.isInstance(client)) {
			client.getParams().setIntParameter(org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT, timeout);
		}
	}

	/**
	 * 指示此请求工厂是否应在内部缓冲请求主体.
	 * <p>默认 {@code true}. 通过POST或PUT发送大量数据时, 建议将此属性更改为{@code false}, 以免内存不足.
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
		postProcessHttpRequest(httpRequest);
		HttpContext context = createHttpContext(httpMethod, uri);
		if (context == null) {
			context = HttpClientContext.create();
		}

		// 请求配置未在上下文中设置
		if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
			// 使用用户提供的请求配置
			RequestConfig config = null;
			if (httpRequest instanceof Configurable) {
				config = ((Configurable) httpRequest).getConfig();
			}
			if (config == null) {
				config = createRequestConfig(getHttpClient());
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}

		if (this.bufferRequestBody) {
			return new HttpComponentsClientHttpRequest(getHttpClient(), httpRequest, context);
		}
		else {
			return new HttpComponentsStreamingClientHttpRequest(getHttpClient(), httpRequest, context);
		}
	}


	/**
	 * 返回修改工厂级{@link RequestConfig}的构建器.
	 */
	private RequestConfig.Builder requestConfigBuilder() {
		return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
	}

	/**
	 * 创建与给定的客户端一起使用的默认的{@link RequestConfig}.
	 * 可以返回{@code null}以指示不应设置自定义请求配置, 并且应使用{@link HttpClient}的默认值.
	 * <p>默认实现尝试将客户端的默认值与此工厂实例的本地自定义项合并.
	 * 
	 * @param client 要检查的{@link HttpClient} (或{@code HttpAsyncClient})
	 * 
	 * @return 要使用的实际RequestConfig (may be {@code null})
	 */
	protected RequestConfig createRequestConfig(Object client) {
		if (client instanceof Configurable) {
			RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
			return mergeRequestConfig(clientRequestConfig);
		}
		return this.requestConfig;
	}

	/**
	 * 将给定的{@link HttpClient}级 {@link RequestConfig}与工厂级{@link RequestConfig}合并.
	 * 
	 * @param clientConfig 当前持有的配置
	 * 
	 * @return 合并的请求配置
	 */
	protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
		if (this.requestConfig == null) {  // nothing to merge
			return clientConfig;
		}

		RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
		int connectTimeout = this.requestConfig.getConnectTimeout();
		if (connectTimeout >= 0) {
			builder.setConnectTimeout(connectTimeout);
		}
		int connectionRequestTimeout = this.requestConfig.getConnectionRequestTimeout();
		if (connectionRequestTimeout >= 0) {
			builder.setConnectionRequestTimeout(connectionRequestTimeout);
		}
		int socketTimeout = this.requestConfig.getSocketTimeout();
		if (socketTimeout >= 0) {
			builder.setSocketTimeout(socketTimeout);
		}
		return builder.build();
	}

	/**
	 * 为给定的HTTP方法和URI规范创建Commons HttpMethodBase对象.
	 * 
	 * @param httpMethod HTTP方法
	 * @param uri the URI
	 * 
	 * @return Commons HttpMethodBase对象
	 */
	protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
		switch (httpMethod) {
			case GET:
				return new HttpGet(uri);
			case HEAD:
				return new HttpHead(uri);
			case POST:
				return new HttpPost(uri);
			case PUT:
				return new HttpPut(uri);
			case PATCH:
				return new HttpPatch(uri);
			case DELETE:
				return new HttpDelete(uri);
			case OPTIONS:
				return new HttpOptions(uri);
			case TRACE:
				return new HttpTrace(uri);
			default:
				throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
		}
	}

	/**
	 * 模板方法, 允许在作为{@link HttpComponentsClientHttpRequest}的一部分返回之前操纵{@link HttpUriRequest}.
	 * <p>默认实现为空.
	 * 
	 * @param request 要处理的请求
	 */
	protected void postProcessHttpRequest(HttpUriRequest request) {
	}

	/**
	 * 为给定的HTTP方法和URI创建{@link HttpContext}的模板方法.
	 * <p>默认实现返回{@code null}.
	 * 
	 * @param httpMethod HTTP方法
	 * @param uri the URI
	 * 
	 * @return http上下文
	 */
	protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
		return null;
	}


	/**
	 * 关闭底层{@link org.apache.http.conn.HttpClientConnectionManager ClientConnectionManager}的连接池的关闭挂钩.
	 */
	@Override
	public void destroy() throws Exception {
		HttpClient httpClient = getHttpClient();
		if (httpClient instanceof Closeable) {
			((Closeable) httpClient).close();
		}
	}


	/**
	 * {@link org.apache.http.client.methods.HttpDelete}的替代方法,
	 * 它扩展了{@link org.apache.http.client.methods.HttpEntityEnclosingRequestBase}
	 * 而不是{@link org.apache.http.client.methods.HttpRequestBase}, 因此允许使用请求正文进行HTTP删除.
	 * 用于RestTemplate交换方法, 允许HTTP DELETE与实体的组合.
	 */
	private static class HttpDelete extends HttpEntityEnclosingRequestBase {

		public HttpDelete(URI uri) {
			super();
			setURI(uri);
		}

		@Override
		public String getMethod() {
			return "DELETE";
		}
	}

}
