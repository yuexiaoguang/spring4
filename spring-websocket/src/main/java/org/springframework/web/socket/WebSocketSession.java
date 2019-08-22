package org.springframework.web.socket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

/**
 * WebSocket会话抽象. 允许通过WebSocket连接发送消息并关闭它.
 */
public interface WebSocketSession extends Closeable {

	/**
	 * 返回唯一的会话标识符.
	 */
	String getId();

	/**
	 * 返回用于打开WebSocket连接的URI.
	 */
	URI getUri();

	/**
	 * 返回握手请求中使用的header (never {@code null}).
	 */
	HttpHeaders getHandshakeHeaders();

	/**
	 * 返回包含与WebSocket会话关联的属性的Map.
	 * <p>在服务器端, 最初可以通过
	 * {@link org.springframework.web.socket.server.HandshakeInterceptor HandshakeInterceptor}填充映射.
	 * 在客户端, 可以通过
	 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}握手方法填充映射.
	 * 
	 * @return 带有会话属性的Map (never {@code null})
	 */
	Map<String, Object> getAttributes();

	/**
	 * 返回包含经过身份验证的用户名的{@link java.security.Principal}实例.
	 * <p>如果用户尚未通过身份验证, 则该方法返回<code>null</code>.
	 */
	Principal getPrincipal();

	/**
	 * 返回收到请求的地址.
	 */
	InetSocketAddress getLocalAddress();

	/**
	 * 返回远程客户端的地址.
	 */
	InetSocketAddress getRemoteAddress();

	/**
	 * 返回协商的子协议.
	 * 
	 * @return 协议标识符, 如果未成功指定或协商协议, 则为{@code null}
	 */
	String getAcceptedProtocol();

	/**
	 * 配置传入文本消息的最大大小.
	 */
	void setTextMessageSizeLimit(int messageSizeLimit);

	/**
	 * 获取传入文本消息的配置的最大大小.
	 */
	int getTextMessageSizeLimit();

	/**
	 * 配置传入的二进制消息的最大大小.
	 */
	void setBinaryMessageSizeLimit(int messageSizeLimit);

	/**
	 * 获取传入二进制消息的配置的最大大小.
	 */
	int getBinaryMessageSizeLimit();

	/**
	 * 确定协商的扩展名.
	 * 
	 * @return 扩展名列表, 如果未成功指定或协商扩展名, 则为空列表
	 */
	List<WebSocketExtension> getExtensions();

	/**
	 * 发送WebSocket消息: {@link TextMessage}或{@link BinaryMessage}.
	 */
	void sendMessage(WebSocketMessage<?> message) throws IOException;

	/**
	 * 返回连接是否仍处于打开状态.
	 */
	boolean isOpen();

	/**
	 * 关闭状态为1000的WebSocket连接, i.e. 等效于:
	 * <pre class="code">
	 * session.close(CloseStatus.NORMAL);
	 * </pre>
	 */
	@Override
	void close() throws IOException;

	/**
	 * 使用给定的关闭状态关闭WebSocket连接.
	 */
	void close(CloseStatus status) throws IOException;

}
