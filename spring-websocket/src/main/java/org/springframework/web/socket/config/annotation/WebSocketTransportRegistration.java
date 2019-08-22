package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * 配置WebSocket客户端接收和发送的消息的处理.
 */
public class WebSocketTransportRegistration {

	private Integer messageSizeLimit;

	private Integer sendTimeLimit;

	private Integer sendBufferSizeLimit;

	private final List<WebSocketHandlerDecoratorFactory> decoratorFactories =
			new ArrayList<WebSocketHandlerDecoratorFactory>(2);


	/**
	 * 配置传入子协议消息的最大大小.
	 * 例如, 当使用SockJS后备选项时, 可以将STOMP消息作为多个WebSocket消息或多个HTTP POST请求接收.
	 * <p>从理论上讲, WebSocket消息的大小几乎是无限的.
	 * 实际上, WebSocket服务器对传入的消息大小施加了限制.
	 * 例如, STOMP客户端倾向于在16K边界左右分割大型消息.
	 * 因此, 服务器必须能够缓冲部分内容, 并在接收到足够数据时进行解码.
	 * 使用此属性配置要使用的缓冲区的最大大小.
	 * <p>默认为 64K (i.e. 64 * 1024).
	 * <p><strong>NOTE</strong> STOMP规范的当前版本 1.2 没有具体讨论如何通过WebSocket发送STOMP消息.
	 * 规范的第2版将会有, 但同时现有的客户端库已经建立了服务器必须处理的实践.
	 */
	public WebSocketTransportRegistration setMessageSizeLimit(int messageSizeLimit) {
		this.messageSizeLimit = messageSizeLimit;
		return this;
	}

	/**
	 * 供内部使用的访问器.
	 */
	protected Integer getMessageSizeLimit() {
		return this.messageSizeLimit;
	}

	/**
	 * 配置在向WebSocket会话发送消息或在使用SockJS后备选项写入HTTP响应时允许的最大时间, 以毫秒为单位.
	 * <p>通常, WebSocket服务器期望一次从单个线程发送到单个WebSocket会话的消息.
	 * 使用 {@code @EnableWebSocketMessageBroker}配置时, 会自动保证这一点.
	 * 如果消息发送速度很慢, 或者至少比发送消息的速率慢, 则后续消息将被缓冲,
	 * 直到达到{@code sendTimeLimit}或{@code sendBufferSizeLimit}, 此时会话状态被清除并尝试关闭会话.
	 * <p><strong>NOTE</strong> 只有在尝试发送其他消息时才会检查会话时间限制.
	 * 因此, 如果只发送一条消息并且它挂起, 则在发送另一条消息或底层物理套接字超时之前, 会话不会超时.
	 * 所以这不是WebSocket服务器或HTTP连接超时的替代品, 而是旨在控制未发送消息的缓冲程度.
	 * <p><strong>NOTE</strong> 关闭会话可能无法成功实际关闭物理套接字, 也可能挂起.
	 * 特别是当使用阻塞IO时, 例如Tomcat 7中默认使用的BIO连接器.
	 * 因此, 建议确保服务器使用非阻塞IO, 例如Tomcat 8上默认使用的NIO连接器.
	 * 如果必须使用阻塞IO, 考虑自定义操作系统级别的TCP设置, 例如Linux上的{@code /proc/sys/net/ipv4/tcp_retries2}.
	 * <p>默认为 10 秒 (i.e. 10 * 10000).
	 * 
	 * @param timeLimit 超时值, 以毫秒为单位; 该值必须大于0, 否则将被忽略.
	 */
	public WebSocketTransportRegistration setSendTimeLimit(int timeLimit) {
		this.sendTimeLimit = timeLimit;
		return this;
	}

	/**
	 * 供内部使用的访问器.
	 */
	protected Integer getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * 配置在向WebSocket会话发送消息时, 或在使用SockJS后备选项时的HTTP响应, 要缓冲的最大数据量.
	 * <p>通常, WebSocket服务器期望一次从单个线程发送到单个WebSocket会话的消息.
	 * 使用{@code @EnableWebSocketMessageBroker}配置时, 会自动保证这一点.
	 * 如果消息发送速度很慢, 或者至少比发送消息的速率慢, 则后续消息将被缓冲,
	 * 直到达到{@code sendTimeLimit}或{@code sendBufferSizeLimit}, 此时会话状态被清除并尝试关闭会话.
	 * <p><strong>NOTE</strong> 关闭会话可能无法成功实际关闭物理套接字, 也可能挂起.
	 * 特别是当使用阻塞IO时, 例如Tomcat 7中默认使用的BIO连接器.
	 * 因此, 建议确保服务器使用非阻塞IO, 例如Tomcat 8上默认使用的NIO连接器.
	 * 如果必须使用阻塞IO, 考虑自定义操作系统级别的TCP设置, 例如Linux上的{@code /proc/sys/net/ipv4/tcp_retries2}.
	 * <p>默认为 512K (i.e. 512 * 1024).
	 * 
	 * @param sendBufferSizeLimit 发送消息时缓冲的最大字节数; 如果该值小于或等于0, 则有效地禁用缓冲.
	 */
	public WebSocketTransportRegistration setSendBufferSizeLimit(int sendBufferSizeLimit) {
		this.sendBufferSizeLimit = sendBufferSizeLimit;
		return this;
	}

	/**
	 * 供内部使用的访问器.
	 */
	protected Integer getSendBufferSizeLimit() {
		return this.sendBufferSizeLimit;
	}

	/**
	 * 配置一个或多个工厂以装饰用于处理WebSocket消息的处理器.
	 * 这在某些高级用例中可能很有用, 例如, 允许Spring Security在相应的HTTP会话到期时强制关闭WebSocket会话.
	 */
	public WebSocketTransportRegistration setDecoratorFactories(WebSocketHandlerDecoratorFactory... factories) {
		if (factories != null) {
			this.decoratorFactories.addAll(Arrays.asList(factories));
		}
		return this;
	}

	/**
	 * 添加一个工厂来装饰用于处理WebSocket消息的处理器.
	 * 这对于某些高级用例可能很有用, 例如, 允许Spring Security在相应的HTTP会话到期时强制关闭WebSocket会话.
	 */
	public WebSocketTransportRegistration addDecoratorFactory(WebSocketHandlerDecoratorFactory factory) {
		this.decoratorFactories.add(factory);
		return this;
	}

	protected List<WebSocketHandlerDecoratorFactory> getDecoratorFactories() {
		return this.decoratorFactories;
	}
}
