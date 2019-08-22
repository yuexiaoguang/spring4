package org.springframework.web.socket.sockjs.frame;

import java.io.IOException;
import java.io.InputStream;

/**
 * 在SockJS消息帧和消息之间进行编码和解码, 本质上是一组JSON编码的消息.
 * 例如:
 *
 * <pre class="code">
 * a["message1","message2"]
 * </pre>
 */
public interface SockJsMessageCodec {

	/**
	 * 将给定消息编码为SockJS消息帧.
	 * 除了将标准JSON引用应用于每条消息之外, 还有一些额外的JSON Unicode转义规则.
	 * 请参阅SockJS协议的"JSON Unicode Encoding"部分 (i.e. 协议测试套件).
	 * 
	 * @param messages 要编码的消息
	 * 
	 * @return SockJS消息帧的内容 (never {@code null})
	 */
	String encode(String... messages);

	/**
	 * 解码给定的SockJS消息帧.
	 * 
	 * @param content SockJS消息帧
	 * 
	 * @return 消息数组, 或{@code null}
	 * @throws IOException 如果无法解析内容
	 */
	String[] decode(String content) throws IOException;

	/**
	 * 解码给定的SockJS消息帧.
	 * 
	 * @param content SockJS消息帧
	 * 
	 * @return 消息数组, 或{@code null}
	 * @throws IOException 如果无法解析内容
	 */
	String[] decodeInputStream(InputStream content) throws IOException;

}
