package org.springframework.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link org.springframework.http.client.ClientHttpRequest}实现, 使用标准JDK工具执行缓冲请求.
 * 通过{@link org.springframework.http.client.SimpleClientHttpRequestFactory}创建.
 */
final class SimpleBufferingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private final HttpURLConnection connection;

	private final boolean outputStreaming;

	private final AsyncListenableTaskExecutor taskExecutor;


	SimpleBufferingAsyncClientHttpRequest(HttpURLConnection connection,
			boolean outputStreaming, AsyncListenableTaskExecutor taskExecutor) {

		this.connection = connection;
		this.outputStreaming = outputStreaming;
		this.taskExecutor = taskExecutor;
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.connection.getRequestMethod());
	}

	@Override
	public URI getURI() {
		try {
			return this.connection.getURL().toURI();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(
			final HttpHeaders headers, final byte[] bufferedOutput) throws IOException {

		return this.taskExecutor.submitListenable(new Callable<ClientHttpResponse>() {
			@Override
			public ClientHttpResponse call() throws Exception {
				SimpleBufferingClientHttpRequest.addHeaders(connection, headers);
				// JDK <1.8 不支持带有HTTP DELETE的getOutputStream
				if (getMethod() == HttpMethod.DELETE && bufferedOutput.length == 0) {
					connection.setDoOutput(false);
				}
				if (connection.getDoOutput() && outputStreaming) {
					connection.setFixedLengthStreamingMode(bufferedOutput.length);
				}
				connection.connect();
				if (connection.getDoOutput()) {
					FileCopyUtils.copy(bufferedOutput, connection.getOutputStream());
				}
				else {
					// 在无输出场景中立即触发请求
					connection.getResponseCode();
				}
				return new SimpleClientHttpResponse(connection);
			}
		});
	}

}
