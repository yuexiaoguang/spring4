package org.springframework.http.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequestFactory}实现,
 * 使用 <a href="http://square.github.io/okhttp/">OkHttp</a> 2.x创建请求.
 */
public class OkHttpClientHttpRequestFactory
		implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory, DisposableBean {

	private final OkHttpClient client;

	private final boolean defaultClient;


	public OkHttpClientHttpRequestFactory() {
		this.client = new OkHttpClient();
		this.defaultClient = true;
	}

	/**
	 * @param client 要使用的客户端
	 */
	public OkHttpClientHttpRequestFactory(OkHttpClient client) {
		Assert.notNull(client, "OkHttpClient must not be null");
		this.client = client;
		this.defaultClient = false;
	}


	/**
	 * 设置底层读取超时, 以毫秒为单位.
	 * 值0指定无限超时.
	 */
	public void setReadTimeout(int readTimeout) {
		this.client.setReadTimeout(readTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * 设置底层写入超时, 以毫秒为单位.
	 * 值0指定无限超时.
	 */
	public void setWriteTimeout(int writeTimeout) {
		this.client.setWriteTimeout(writeTimeout, TimeUnit.MILLISECONDS);
	}

	/**
	 * 设置底层连接超时, 以毫秒为单位.
	 * 值0指定无限超时.
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.client.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttpClientHttpRequest(this.client, uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) {
		return new OkHttpAsyncClientHttpRequest(this.client, uri, httpMethod);
	}


	@Override
	public void destroy() throws IOException {
		if (this.defaultClient) {
			// 如果在构造函数中创建它, 清理客户端
			if (this.client.getCache() != null) {
				this.client.getCache().close();
			}
			this.client.getDispatcher().getExecutorService().shutdown();
		}
	}


	static Request buildRequest(HttpHeaders headers, byte[] content, URI uri, HttpMethod method)
			throws MalformedURLException {

		com.squareup.okhttp.MediaType contentType = getContentType(headers);
		RequestBody body = (content.length > 0 ||
				com.squareup.okhttp.internal.http.HttpMethod.requiresRequestBody(method.name()) ?
				RequestBody.create(contentType, content) : null);

		Request.Builder builder = new Request.Builder().url(uri.toURL()).method(method.name(), body);
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				builder.addHeader(headerName, headerValue);
			}
		}
		return builder.build();
	}

	private static com.squareup.okhttp.MediaType getContentType(HttpHeaders headers) {
		String rawContentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
		return (StringUtils.hasText(rawContentType) ?
				com.squareup.okhttp.MediaType.parse(rawContentType) : null);
	}

}
