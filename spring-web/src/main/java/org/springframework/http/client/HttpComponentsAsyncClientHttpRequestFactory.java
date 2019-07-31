package org.springframework.http.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link HttpComponentsClientHttpRequestFactory}的异步扩展.
 * 使用<a href="http://hc.apache.org/httpcomponents-asyncclient-dev/">Apache HttpComponents HttpAsyncClient 4.0</a>创建请求.
 */
public class HttpComponentsAsyncClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory
		implements AsyncClientHttpRequestFactory, InitializingBean {

	private HttpAsyncClient asyncClient;


	/**
	 * 使用默认的{@link HttpAsyncClient} 和 {@link HttpClient}.
	 */
	public HttpComponentsAsyncClientHttpRequestFactory() {
		super();
		this.asyncClient = HttpAsyncClients.createSystem();
	}

	/**
	 * 使用给定的{@link HttpAsyncClient}实例和默认的{@link HttpClient}.
	 * 
	 * @param asyncClient 用于此请求工厂的HttpAsyncClient实例
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpAsyncClient asyncClient) {
		super();
		setAsyncClient(asyncClient);
	}

	/**
	 * 使用给定的{@link CloseableHttpAsyncClient}实例和默认的{@link HttpClient}.
	 * 
	 * @param asyncClient 用于此请求工厂的CloseableHttpAsyncClient实例
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(CloseableHttpAsyncClient asyncClient) {
		super();
		setAsyncClient(asyncClient);
	}

	/**
	 * @param httpClient 用于此请求工厂的HttpClient实例
	 * @param asyncClient 用于此请求工厂的HttpAsyncClient实例
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(HttpClient httpClient, HttpAsyncClient asyncClient) {
		super(httpClient);
		setAsyncClient(asyncClient);
	}

	/**
	 * @param httpClient 用于此请求工厂的CloseableHttpClient实例
	 * @param asyncClient 用于此请求工厂的CloseableHttpAsyncClient实例
	 */
	public HttpComponentsAsyncClientHttpRequestFactory(
			CloseableHttpClient httpClient, CloseableHttpAsyncClient asyncClient) {

		super(httpClient);
		setAsyncClient(asyncClient);
	}


	/**
	 * 设置用于{@linkplain #createAsyncRequest(URI, HttpMethod) 同步执行}的{@code HttpAsyncClient}.
	 */
	public void setAsyncClient(HttpAsyncClient asyncClient) {
		Assert.notNull(asyncClient, "HttpAsyncClient must not be null");
		this.asyncClient = asyncClient;
	}

	/**
	 * 返回用于{@linkplain #createAsyncRequest(URI, HttpMethod) 同步执行}的{@code HttpAsyncClient}.
	 */
	public HttpAsyncClient getAsyncClient() {
		return this.asyncClient;
	}

	/**
	 * 设置用于{@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行}的{@code CloseableHttpAsyncClient}.
	 * 
	 * @deprecated as of 4.3.10, in favor of {@link #setAsyncClient(HttpAsyncClient)}
	 */
	@Deprecated
	public void setHttpAsyncClient(CloseableHttpAsyncClient asyncClient) {
		this.asyncClient = asyncClient;
	}

	/**
	 * 返回用于
	 * {@linkplain #createAsyncRequest(URI, HttpMethod) 异步执行}的{@code CloseableHttpAsyncClient}.
	 * 
	 * @deprecated as of 4.3.10, in favor of {@link #getAsyncClient()}
	 */
	@Deprecated
	public CloseableHttpAsyncClient getHttpAsyncClient() {
		Assert.state(this.asyncClient == null || this.asyncClient instanceof CloseableHttpAsyncClient,
				"No CloseableHttpAsyncClient - use getAsyncClient() instead");
		return (CloseableHttpAsyncClient) this.asyncClient;
	}


	@Override
	public void afterPropertiesSet() {
		startAsyncClient();
	}

	private void startAsyncClient() {
        HttpAsyncClient asyncClient = getAsyncClient();
		if (asyncClient instanceof CloseableHttpAsyncClient) {
			CloseableHttpAsyncClient closeableAsyncClient = (CloseableHttpAsyncClient) asyncClient;
			if (!closeableAsyncClient.isRunning()) {
				closeableAsyncClient.start();
			}
		}
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		startAsyncClient();

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
				config = createRequestConfig(getAsyncClient());
			}
			if (config != null) {
				context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
			}
		}

		return new HttpComponentsAsyncClientHttpRequest(getAsyncClient(), httpRequest, context);
	}

	@Override
	public void destroy() throws Exception {
		try {
			super.destroy();
		}
		finally {
			HttpAsyncClient asyncClient = getAsyncClient();
			if (asyncClient instanceof Closeable) {
				((Closeable) asyncClient).close();
			}
		}
	}
}
