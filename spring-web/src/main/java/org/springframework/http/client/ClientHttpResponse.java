package org.springframework.http.client;

import java.io.Closeable;
import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;

/**
 * 表示客户端HTTP响应.
 * 通过调用{@link ClientHttpRequest#execute()}获得.
 *
 * <p>{@code ClientHttpResponse}必须被{@linkplain #close() 关闭}, 通常在{@code finally}块中.
 */
public interface ClientHttpResponse extends HttpInputMessage, Closeable {

	/**
	 * 返回响应的HTTP状态码.
	 * 
	 * @return HTTP状态
	 * @throws IOException
	 * @throws IllegalArgumentException 如果是未知的HTTP状态码
	 */
	HttpStatus getStatusCode() throws IOException;

	/**
	 * 返回HTTP状态码 (可能是非标准的, 无法通过{@link HttpStatus}枚举解析).
	 * 
	 * @return HTTP状态
	 * @throws IOException
	 */
	int getRawStatusCode() throws IOException;

	/**
	 * 返回响应的HTTP状态文本.
	 * 
	 * @return HTTP状态文本
	 * @throws IOException
	 */
	String getStatusText() throws IOException;

	/**
	 * 关闭此响应, 释放所有创建的资源.
	 */
	@Override
	void close();

}
