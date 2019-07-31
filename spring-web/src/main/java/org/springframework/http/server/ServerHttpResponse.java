package org.springframework.http.server;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;

/**
 * 表示服务器端HTTP响应.
 */
public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

	/**
	 * 设置响应的HTTP状态码.
	 * 
	 * @param status HTTP状态
	 */
	void setStatusCode(HttpStatus status);

	/**
	 * 刷新header和响应内容.
	 * <p>第一次刷新后, 无法再更改header.
	 * 只有进一步的内容编写和内容刷新是可能的.
	 */
	@Override
	void flush() throws IOException;

	/**
	 * 关闭此响应, 释放所有创建的资源.
	 */
	@Override
	void close();

}
