package org.springframework.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 表示HTTP输出消息, 由{@linkplain #getHeaders() headers}和可写的 {@linkplain #getBody() body}组成.
 *
 * <p>通常由客户端的HTTP请求句柄或服务器端的HTTP响应句柄实现.
 */
public interface HttpOutputMessage extends HttpMessage {

	/**
	 * 将消息正文作为输出流返回.
	 * 
	 * @return 输出流主体 (never {@code null})
	 * @throws IOException
	 */
	OutputStream getBody() throws IOException;

}
