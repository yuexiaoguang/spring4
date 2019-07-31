package org.springframework.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequestFactory}实现, 使用标准JDK工具.
 */
public class SimpleClientHttpRequestFactory implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {

	private static final int DEFAULT_CHUNK_SIZE = 4096;


	private Proxy proxy;

	private boolean bufferRequestBody = true;

	private int chunkSize = DEFAULT_CHUNK_SIZE;

	private int connectTimeout = -1;

	private int readTimeout = -1;

	private boolean outputStreaming = true;

	private AsyncListenableTaskExecutor taskExecutor;


	/**
	 * 设置用于此请求工厂的{@link Proxy}.
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * 指示此请求工厂是否应在内部缓冲{@linkplain ClientHttpRequest#getBody() 请求正文}.
	 * <p>默认{@code true}. 通过POST或PUT发送大量数据时, 建议将此属性更改为{@code false}, 以免内存不足.
	 * 这将导致{@link ClientHttpRequest}直接流入底层{@link HttpURLConnection}
	 * (如果预先知道{@link org.springframework.http.HttpHeaders#getContentLength() Content-Length}),
	 * 或者将使用"分块传输编码" (如果事先不知道{@code Content-Length}).
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	/**
	 * 设置当不在本地缓冲请求主体时在每个块中写入的字节数.
	 * <p>请注意, 此参数仅在
	 * {@link #setBufferRequestBody(boolean) bufferRequestBody}设置为{@code false}时使用,
	 * 并且事先不知道{@link org.springframework.http.HttpHeaders#getContentLength() Content-Length}.
	 */
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * 设置底层URLConnection的连接超时 (以毫秒为单位).
	 * 超时值0指定无限超时.
	 * <p>默认值是系统的默认超时.
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层URLConnection的读取超时 (以毫秒为单位).
	 * 超时值0指定无限超时.
	 * <p>默认值是系统的默认超时.
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * 设置是否可以将底层URLConnection设置为'输出流'模式.
	 * 默认{@code true}.
	 * <p>启用输出流时, 无法自动处理身份验证和重定向.
	 * 如果禁用输出流, 则永远不会调用底层连接的{@link HttpURLConnection#setFixedLengthStreamingMode}
	 * 和{@link HttpURLConnection#setChunkedStreamingMode}方法.
	 * 
	 * @param outputStreaming 如果输出流已启用
	 */
	public void setOutputStreaming(boolean outputStreaming) {
		this.outputStreaming = outputStreaming;
	}

	/**
	 * 设置此请求工厂的任务执行器.
	 * 设置此属性是{@linkplain #createAsyncRequest(URI, HttpMethod) 创建异步请求}所必需的.
	 * 
	 * @param taskExecutor 任务执行器
	 */
	public void setTaskExecutor(AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		if (this.bufferRequestBody) {
			return new SimpleBufferingClientHttpRequest(connection, this.outputStreaming);
		}
		else {
			return new SimpleStreamingClientHttpRequest(connection, this.chunkSize, this.outputStreaming);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>在调用此方法之前, 需要设置{@link #setTaskExecutor taskExecutor}属性.
	 */
	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		Assert.state(this.taskExecutor != null, "Asynchronous execution requires TaskExecutor to be set");

		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		if (this.bufferRequestBody) {
			return new SimpleBufferingAsyncClientHttpRequest(
					connection, this.outputStreaming, this.taskExecutor);
		}
		else {
			return new SimpleStreamingAsyncClientHttpRequest(
					connection, this.chunkSize, this.outputStreaming, this.taskExecutor);
		}
	}

	/**
	 * 打开并返回给定URL的连接.
	 * <p>默认实现使用给定的{@linkplain #setProxy(java.net.Proxy) 代理} - 打开连接.
	 * 
	 * @param url 用于打开连接的URL
	 * @param proxy 要使用的代理, 可能是{@code null}
	 * 
	 * @return 打开的连接
	 * @throws IOException
	 */
	protected HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
		URLConnection urlConnection = (proxy != null ? url.openConnection(proxy) : url.openConnection());
		if (!HttpURLConnection.class.isInstance(urlConnection)) {
			throw new IllegalStateException("HttpURLConnection required for [" + url + "] but got: " + urlConnection);
		}
		return (HttpURLConnection) urlConnection;
	}

	/**
	 * 准备给定的{@link HttpURLConnection}的模板方法.
	 * <p>默认实现为输入和输出准备连接, 并设置HTTP方法.
	 * 
	 * @param connection 要准备的连接
	 * @param httpMethod HTTP请求方法 ({@code GET}, {@code POST}, etc.)
	 * 
	 * @throws IOException
	 */
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		connection.setDoInput(true);

		if ("GET".equals(httpMethod)) {
			connection.setInstanceFollowRedirects(true);
		}
		else {
			connection.setInstanceFollowRedirects(false);
		}

		if ("POST".equals(httpMethod) || "PUT".equals(httpMethod) ||
				"PATCH".equals(httpMethod) || "DELETE".equals(httpMethod)) {
			connection.setDoOutput(true);
		}
		else {
			connection.setDoOutput(false);
		}

		connection.setRequestMethod(httpMethod);
	}

}
