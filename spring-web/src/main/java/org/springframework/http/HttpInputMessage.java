package org.springframework.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * 表示HTTP输入消息, 由{@linkplain #getHeaders() headers}和可读的{@linkplain #getBody() body}组成.
 *
 * <p>通常由服务器端的HTTP请求句柄或客户端的HTTP响应句柄实现.
 */
public interface HttpInputMessage extends HttpMessage {

	/**
	 * 将消息正文作为输入流返回.
	 * 
	 * @return 输入流 (never {@code null})
	 * @throws IOException
	 */
	InputStream getBody() throws IOException;

}
