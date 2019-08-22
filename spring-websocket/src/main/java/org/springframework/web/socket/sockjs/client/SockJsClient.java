package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 具有回退替代方案的
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}的SockJS实现,
 * 通过普通HTTP流和长轮询技术模拟WebSocket交互.
 *
 * <p>实现{@link Lifecycle}, 以便将生命周期事件传播到它配置的传输.
 */
public class SockJsClient implements WebSocketClient, Lifecycle {

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", SockJsClient.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(SockJsClient.class);

	private static final Set<String> supportedProtocols = new HashSet<String>(4);

	static {
		supportedProtocols.add("ws");
		supportedProtocols.add("wss");
		supportedProtocols.add("http");
		supportedProtocols.add("https");
	}


	private final List<Transport> transports;

	private String[] httpHeaderNames;

	private InfoReceiver infoReceiver;

	private SockJsMessageCodec messageCodec;

	private TaskScheduler connectTimeoutScheduler;

	private volatile boolean running = false;

	private final Map<URI, ServerInfo> serverInfoCache = new ConcurrentHashMap<URI, ServerInfo>();


	/**
	 * 使用给定的传输创建{@code SockJsClient}.
	 * <p>如果列表包含{@link XhrTransport} (或更具体地说是{@link InfoReceiver}的实现),
	 * 则该实例用于初始化{@link #setInfoReceiver(InfoReceiver) infoReceiver}属性,
	 * 否则默认为{@link RestTemplateXhrTransport}.
	 * 
	 * @param transports 要使用的(非空)传输列表
	 */
	public SockJsClient(List<Transport> transports) {
		Assert.notEmpty(transports, "No transports provided");
		this.transports = new ArrayList<Transport>(transports);
		this.infoReceiver = initInfoReceiver(transports);
		if (jackson2Present) {
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
	}

	private static InfoReceiver initInfoReceiver(List<Transport> transports) {
		for (Transport transport : transports) {
			if (transport instanceof InfoReceiver) {
				return ((InfoReceiver) transport);
			}
		}
		return new RestTemplateXhrTransport();
	}


	/**
	 * 应该从每次调用
	 * {@link SockJsClient#doHandshake(WebSocketHandler, WebSocketHttpHeaders, URI)}
	 * 的握手header中复制的HTTP header的名称,
	 * 并且还与作为该SockJS连接的一部分发出的其他HTTP请求一起使用, e.g. 初始信息请求, XHR发送或接收请求.
	 *
	 * <p>默认情况下, 如果未设置此属性, 则所有握手header也将用于其他HTTP请求.
	 * 如果只想将一部分握手header (e.g. auth headers)用于其他HTTP请求, 设置它.
	 *
	 * @param httpHeaderNames HTTP header名称
	 */
	public void setHttpHeaderNames(String... httpHeaderNames) {
		this.httpHeaderNames = httpHeaderNames;
	}

	/**
	 * 配置的HTTP header名称要从握手 header中复制, 也包含在其他HTTP请求中.
	 */
	public String[] getHttpHeaderNames() {
		return this.httpHeaderNames;
	}

	/**
	 * 在SockJS会话开始之前配置{@code InfoReceiver}以用于执行SockJS "Info"请求.
	 * <p>如果提供给构造函数的传输的列表包含{@link XhrTransport}或{@link InfoReceiver}的实现,
	 * 该实例将用于初始化此属性, 否则默认为{@link RestTemplateXhrTransport}.
	 * 
	 * @param infoReceiver 用于SockJS "Info"请求的传输
	 */
	public void setInfoReceiver(InfoReceiver infoReceiver) {
		Assert.notNull(infoReceiver, "InfoReceiver is required");
		this.infoReceiver = infoReceiver;
	}

	/**
	 * 返回配置的{@code InfoReceiver} (never {@code null}).
	 */
	public InfoReceiver getInfoReceiver() {
		return this.infoReceiver;
	}

	/**
	 * 设置要使用的SockJsMessageCodec.
	 * <p>默认情况下, 如果在类路径上有Jackson, 则使用
	 * {@link org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec Jackson2SockJsMessageCodec}.
	 */
	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		Assert.notNull(messageCodec, "'messageCodec' is required");
		this.messageCodec = messageCodec;
	}

	/**
	 * 返回要使用的SockJsMessageCodec.
	 */
	public SockJsMessageCodec getMessageCodec() {
		return this.messageCodec;
	}

	/**
	 * 配置{@code TaskScheduler}以安排连接超时任务, 其中根据初始SockJS "Info"请求的持续时间计算超时值.
	 * 连接超时任务确保更及时的回退, 但在其他方面完全是可选的.
	 * <p>默认未配置, 在这种情况下, 后备可能需要更长时间.
	 * 
	 * @param connectTimeoutScheduler 要使用的任务定时器
	 */
	public void setConnectTimeoutScheduler(TaskScheduler connectTimeoutScheduler) {
		this.connectTimeoutScheduler = connectTimeoutScheduler;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (Transport transport : this.transports) {
				if (transport instanceof Lifecycle) {
					if (!((Lifecycle) transport).isRunning()) {
						((Lifecycle) transport).start();
					}
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (Transport transport : this.transports) {
				if (transport instanceof Lifecycle) {
					if (((Lifecycle) transport).isRunning()) {
						((Lifecycle) transport).stop();
					}
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(
			WebSocketHandler handler, String uriTemplate, Object... uriVars) {

		Assert.notNull(uriTemplate, "uriTemplate must not be null");
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
		return doHandshake(handler, null, uri);
	}

	@Override
	public final ListenableFuture<WebSocketSession> doHandshake(
			WebSocketHandler handler, WebSocketHttpHeaders headers, URI url) {

		Assert.notNull(handler, "WebSocketHandler is required");
		Assert.notNull(url, "URL is required");

		String scheme = url.getScheme();
		if (!supportedProtocols.contains(scheme)) {
			throw new IllegalArgumentException("Invalid scheme: '" + scheme + "'");
		}

		SettableListenableFuture<WebSocketSession> connectFuture = new SettableListenableFuture<WebSocketSession>();
		try {
			SockJsUrlInfo sockJsUrlInfo = new SockJsUrlInfo(url);
			ServerInfo serverInfo = getServerInfo(sockJsUrlInfo, getHttpRequestHeaders(headers));
			createRequest(sockJsUrlInfo, headers, serverInfo).connect(handler, connectFuture);
		}
		catch (Throwable exception) {
			if (logger.isErrorEnabled()) {
				logger.error("Initial SockJS \"Info\" request to server failed, url=" + url, exception);
			}
			connectFuture.setException(exception);
		}
		return connectFuture;
	}

	private HttpHeaders getHttpRequestHeaders(HttpHeaders webSocketHttpHeaders) {
		if (getHttpHeaderNames() == null) {
			return webSocketHttpHeaders;
		}
		else {
			HttpHeaders httpHeaders = new HttpHeaders();
			for (String name : getHttpHeaderNames()) {
				if (webSocketHttpHeaders.containsKey(name)) {
					httpHeaders.put(name, webSocketHttpHeaders.get(name));
				}
			}
			return httpHeaders;
		}
	}

	private ServerInfo getServerInfo(SockJsUrlInfo sockJsUrlInfo, HttpHeaders headers) {
		URI infoUrl = sockJsUrlInfo.getInfoUrl();
		ServerInfo info = this.serverInfoCache.get(infoUrl);
		if (info == null) {
			long start = System.currentTimeMillis();
			String response = this.infoReceiver.executeInfoRequest(infoUrl, headers);
			long infoRequestTime = System.currentTimeMillis() - start;
			info = new ServerInfo(response, infoRequestTime);
			this.serverInfoCache.put(infoUrl, info);
		}
		return info;
	}

	private DefaultTransportRequest createRequest(SockJsUrlInfo urlInfo, HttpHeaders headers, ServerInfo serverInfo) {
		List<DefaultTransportRequest> requests = new ArrayList<DefaultTransportRequest>(this.transports.size());
		for (Transport transport : this.transports) {
			for (TransportType type : transport.getTransportTypes()) {
				if (serverInfo.isWebSocketEnabled() || !TransportType.WEBSOCKET.equals(type)) {
					requests.add(new DefaultTransportRequest(urlInfo, headers, getHttpRequestHeaders(headers),
							transport, type, getMessageCodec()));
				}
			}
		}
		if (CollectionUtils.isEmpty(requests)) {
			throw new IllegalStateException(
					"No transports: " + urlInfo + ", webSocketEnabled=" + serverInfo.isWebSocketEnabled());
		}
		for (int i = 0; i < requests.size() - 1; i++) {
			DefaultTransportRequest request = requests.get(i);
			request.setUser(getUser());
			if (this.connectTimeoutScheduler != null) {
				request.setTimeoutValue(serverInfo.getRetransmissionTimeout());
				request.setTimeoutScheduler(this.connectTimeoutScheduler);
			}
			request.setFallbackRequest(requests.get(i + 1));
		}
		return requests.get(0);
	}

	/**
	 * 返回与SockJS会话关联的用户, 并通过{@link org.springframework.web.socket.WebSocketSession#getPrincipal()}提供.
	 * <p>默认返回{@code null}.
	 * 
	 * @return 要与会话关联的用户 (possibly {@code null})
	 */
	protected Principal getUser() {
		return null;
	}

	/**
	 * 默认情况下, SockJS "Info"请求的结果, 包括服务器是否已禁用WebSocket以及请求所用的时间 (用于计算传输超时时间)都会被缓存.
	 * 此方法可用于清除缓存, 从而导致重新填充.
	 */
	public void clearServerInfoCache() {
		this.serverInfoCache.clear();
	}


	/**
	 * 一个简单的值对象, 用于保存SockJS "Info"请求的结果.
	 */
	private static class ServerInfo {

		private final boolean webSocketEnabled;

		private final long responseTime;

		public ServerInfo(String response, long responseTime) {
			this.responseTime = responseTime;
			this.webSocketEnabled = !response.matches(".*[\"']websocket[\"']\\s*:\\s*false.*");
		}

		public boolean isWebSocketEnabled() {
			return this.webSocketEnabled;
		}

		public long getRetransmissionTimeout() {
			return (this.responseTime > 100 ? 4 * this.responseTime : this.responseTime + 300);
		}
	}

}
