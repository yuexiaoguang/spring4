package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;

/**
 * 用于配置SockJS回退选项的辅助类, 用于
 * {@link org.springframework.web.socket.config.annotation.EnableWebSocket}和{@link WebSocketConfigurer}设置.
 */
public class SockJsServiceRegistration {

	private TaskScheduler taskScheduler;

	private String clientLibraryUrl;

	private Integer streamBytesLimit;

	private Boolean sessionCookieNeeded;

	private Long heartbeatTime;

	private Long disconnectDelay;

	private Integer httpMessageCacheSize;

	private Boolean webSocketEnabled;

	private final List<TransportHandler> transportHandlers = new ArrayList<TransportHandler>();

	private final List<TransportHandler> transportHandlerOverrides = new ArrayList<TransportHandler>();

	private final List<HandshakeInterceptor> interceptors = new ArrayList<HandshakeInterceptor>();

	private final List<String> allowedOrigins = new ArrayList<String>();

	private Boolean suppressCors;

	private SockJsMessageCodec messageCodec;


	public SockJsServiceRegistration(TaskScheduler defaultTaskScheduler) {
		this.taskScheduler = defaultTaskScheduler;
	}


	public SockJsServiceRegistration setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		return this;
	}

	/**
	 * 没有本机跨域通信的传输 (e.g. "eventsource", "htmlfile") 必须从不可见iframe中的"foreign"域获取一个简单页面,
	 * 以便iframe中的代码可以从本地域运行到SockJS服务器.
	 * 由于iframe需要加载SockJS javascript客户端库, 因此该属性允许指定从何处加载它.
	 * <p>默认指向"https://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js".
	 * 但是, 它也可以设置为指向应用程序提供的URL.
	 * <p>请注意, 可以指定相对URL, 在这种情况下, URL必须相对于iframe URL.
	 * 例如, 假设SockJS端点映射到"/sockjs", 并生成iframe URL "/sockjs/iframe.html",
	 * 则相对URL必须以"../../"开头, 以遍历到SockJS映射上方的位置.
	 * 在基于前缀的Servlet映射的情况下, 可能需要再遍历一次.
	 */
	public SockJsServiceRegistration setClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
		return this;
	}

	/**
	 * 流传输在客户端保存响应, 不释放传递的消息使用的内存.
	 * 这种传输需要偶尔回收连接.
	 * 此属性设置在关闭之前可通过单个HTTP流式传输请求发送的最小字节数.
	 * 之后客户端将打开一个新请求.
	 * 将此值设置为1可以有效地禁用流式传输, 并使流式传输的行为类似于轮询传输.
	 * <p>默认为 128K (i.e. 128 * 1024).
	 */
	public SockJsServiceRegistration setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
		return this;
	}

	/**
	 * SockJS协议要求服务器响应来自客户端的初始"/info"请求, 其具有"cookie_needed" boolean属性,
	 * 该属性指示是否需要使用JSESSIONID cookie以使应用程序正常运行, e.g. 用于负载平衡或用于Java Servlet容器以使用HTTP会话.
	 *
	 * <p>这对于支持XDomainRequest的IE 8,9尤其重要 -- XDomainRequest是一个经过修改的AJAX/XHR, 它可以跨域执行请求但不发送任何cookie.
	 * 在这些情况下, SockJS客户端更喜欢"xdr-streaming"上的"iframe-htmlfile"传输, 以便能够发送cookie.
	 *
	 * <p>默认值为"true", 以最大限度地提高应用程序在IE 8,9中正常工作的机会，并支持cookie (特别是JSESSIONID cookie).
	 * 但是, 如果不需要使用cookie (和HTTP会话), 应用程序可以选择将其设置为"false".
	 */
	public SockJsServiceRegistration setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
		return this;
	}

	/**
	 * 服务器未发送任何消息的最大时间(以毫秒为单位), 之后服务器应将心跳帧发送到客户端以防止连接中断.
	 * <p>默认为 25,000 (25 秒).
	 */
	public SockJsServiceRegistration setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
		return this;
	}

	/**
	 * 在没有接收连接之后, 但在客户端视为断开连接之前的最大时间(以毫秒为单位), i.e. 服务器可以将数据发送到客户端的活动连接.
	 * <p>默认为 5000.
	 */
	public SockJsServiceRegistration setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
		return this;
	}

	/**
	 * 在等待来自客户端的下一个HTTP轮询请求时, 会话可以缓存的服务器到客户端消息的数量.
	 * 所有HTTP传输都使用此属性, 因为即使是流传输也会定期回收HTTP请求.
	 * <p>HTTP请求之间的时间量应该相对较短, 并且不会超过允许断开连接延迟 (see {@link #setDisconnectDelay(long)}), 默认为5秒.
	 * <p>默认为 100.
	 */
	public SockJsServiceRegistration setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
		return this;
	}

	/**
	 * 某些负载平衡器不支持WebSocket.
	 * 此选项可用于禁用服务器端的WebSocket传输.
	 * <p>默认为 "true".
	 */
	public SockJsServiceRegistration setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
		return this;
	}

	public SockJsServiceRegistration setTransportHandlers(TransportHandler... handlers) {
		this.transportHandlers.clear();
		if (!ObjectUtils.isEmpty(handlers)) {
			this.transportHandlers.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	public SockJsServiceRegistration setTransportHandlerOverrides(TransportHandler... handlers) {
		this.transportHandlerOverrides.clear();
		if (!ObjectUtils.isEmpty(handlers)) {
			this.transportHandlerOverrides.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	public SockJsServiceRegistration setInterceptors(HandshakeInterceptor... interceptors) {
		this.interceptors.clear();
		if (!ObjectUtils.isEmpty(interceptors)) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}

	protected SockJsServiceRegistration setAllowedOrigins(String... allowedOrigins) {
		this.allowedOrigins.clear();
		if (!ObjectUtils.isEmpty(allowedOrigins)) {
			this.allowedOrigins.addAll(Arrays.asList(allowedOrigins));
		}
		return this;
	}

	/**
	 * 此选项可用于禁用为SockJS请求自动添加的CORS header.
	 * <p>默认为 "false".
	 */
	public SockJsServiceRegistration setSupressCors(boolean suppressCors) {
		this.suppressCors = suppressCors;
		return this;
	}

	/**
	 * 用于编码和解码SockJS消息的编解码器.
	 * <p>默认使用{@code Jackson2SockJsMessageCodec}, 要求Jackson库存在于类路径中.
	 * 
	 * @param codec 要使用的编解码器
	 */
	public SockJsServiceRegistration setMessageCodec(SockJsMessageCodec codec) {
		this.messageCodec = codec;
		return this;
	}

	protected SockJsService getSockJsService() {
		TransportHandlingSockJsService service = createSockJsService();
		service.setHandshakeInterceptors(this.interceptors);
		if (this.clientLibraryUrl != null) {
			service.setSockJsClientLibraryUrl(this.clientLibraryUrl);
		}
		if (this.streamBytesLimit != null) {
			service.setStreamBytesLimit(this.streamBytesLimit);
		}
		if (this.sessionCookieNeeded != null) {
			service.setSessionCookieNeeded(this.sessionCookieNeeded);
		}
		if (this.heartbeatTime != null) {
			service.setHeartbeatTime(this.heartbeatTime);
		}
		if (this.disconnectDelay != null) {
			service.setDisconnectDelay(this.disconnectDelay);
		}
		if (this.httpMessageCacheSize != null) {
			service.setHttpMessageCacheSize(this.httpMessageCacheSize);
		}
		if (this.webSocketEnabled != null) {
			service.setWebSocketEnabled(this.webSocketEnabled);
		}
		if (this.suppressCors != null) {
			service.setSuppressCors(this.suppressCors);
		}
		service.setAllowedOrigins(this.allowedOrigins);

		if (this.messageCodec != null) {
			service.setMessageCodec(this.messageCodec);
		}
		return service;
	}

	private TransportHandlingSockJsService createSockJsService() {
		if (!this.transportHandlers.isEmpty()) {
			Assert.state(this.transportHandlerOverrides.isEmpty(),
					"Specify either TransportHandlers or TransportHandler overrides, not both");
			return new TransportHandlingSockJsService(this.taskScheduler, this.transportHandlers);
		}
		else {
			return new DefaultSockJsService(this.taskScheduler, this.transportHandlerOverrides);
		}
	}
}
