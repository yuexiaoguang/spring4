package org.springframework.http.client;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link AsyncClientHttpRequest}的抽象基类, 确保不会多次写入header和正文.
 */
abstract class AbstractAsyncClientHttpRequest implements AsyncClientHttpRequest {

	private final HttpHeaders headers = new HttpHeaders();

	private boolean executed = false;


	@Override
	public final HttpHeaders getHeaders() {
		return (this.executed ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public final OutputStream getBody() throws IOException {
		assertNotExecuted();
		return getBodyInternal(this.headers);
	}

	@Override
	public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
		assertNotExecuted();
		ListenableFuture<ClientHttpResponse> result = executeInternal(this.headers);
		this.executed = true;
		return result;
	}

	/**
	 * 断言此请求尚未{@linkplain #executeAsync() 执行}.
	 * 
	 * @throws IllegalStateException 如果此请求已被执行
	 */
	protected void assertNotExecuted() {
		Assert.state(!this.executed, "ClientHttpRequest already executed");
	}


	/**
	 * 返回主体的抽象模板方法.
	 * 
	 * @param headers the HTTP headers
	 * 
	 * @return 主体输出流
	 */
	protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

	/**
	 * 将给定header和内容写入HTTP请求的抽象模板方法.
	 * 
	 * @param headers the HTTP headers
	 * 
	 * @return 已执行请求的响应对象
	 */
	protected abstract ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers)
			throws IOException;

}
