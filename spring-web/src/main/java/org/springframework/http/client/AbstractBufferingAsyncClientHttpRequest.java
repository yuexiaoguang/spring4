package org.springframework.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link AsyncClientHttpRequest}的基类, 在通过线路发送之前缓冲输出的字节数组.
 */
abstract class AbstractBufferingAsyncClientHttpRequest extends AbstractAsyncClientHttpRequest {

	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);


	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.bufferedOutput;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers) throws IOException {
		byte[] bytes = this.bufferedOutput.toByteArray();
		if (headers.getContentLength() < 0) {
			headers.setContentLength(bytes.length);
		}
		ListenableFuture<ClientHttpResponse> result = executeInternal(headers, bytes);
		this.bufferedOutput = null;
		return result;
	}

	/**
	 * 将给定header和内容写入HTTP请求的抽象模板方法.
	 * 
	 * @param headers the HTTP headers
	 * @param bufferedOutput 主体内容
	 * 
	 * @return 已执行请求的响应对象
	 */
	protected abstract ListenableFuture<ClientHttpResponse> executeInternal(
			HttpHeaders headers, byte[] bufferedOutput) throws IOException;

}
