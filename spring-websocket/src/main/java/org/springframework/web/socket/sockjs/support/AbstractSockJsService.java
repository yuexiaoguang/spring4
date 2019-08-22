package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.util.WebUtils;

/**
 * {@link SockJsService}实现的抽象基类, 提供SockJS路径解析和静态SockJS请求的处理 (e.g. "/info", "/iframe.html", etc).
 * 子类必须处理会话URL (i.e. 特定于传输的请求).
 *
 * 默认只允许相同来源的请求. 使用{@link #setAllowedOrigins}指定允许的来源列表 (包含 "*"的列表将允许所有来源).
 */
public abstract class AbstractSockJsService implements SockJsService, CorsConfigurationSource {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private static final long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);

	private static final Random random = new Random();

	private static final String XFRAME_OPTIONS_HEADER = "X-Frame-Options";


	protected final Log logger = LogFactory.getLog(getClass());

	private final TaskScheduler taskScheduler;

	private String name = "SockJSService@" + ObjectUtils.getIdentityHexString(this);

	private String clientLibraryUrl = "https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js";

	private int streamBytesLimit = 128 * 1024;

	private boolean sessionCookieNeeded = true;

	private long heartbeatTime = 25 * 1000;

	private long disconnectDelay = 5 * 1000;

	private int httpMessageCacheSize = 100;

	private boolean webSocketEnabled = true;

	private boolean suppressCors = false;

	protected final Set<String> allowedOrigins = new LinkedHashSet<String>();


	public AbstractSockJsService(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "TaskScheduler must not be null");
		this.taskScheduler = scheduler;
	}


	/**
	 * 用于调度心跳消息的定时器实例.
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * 设置此服务的唯一名称 (主要用于日志记录).
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回与此服务关联的唯一名称.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 没有本机跨域通信的传输 (e.g. "eventsource", "htmlfile") 必须从不可见iframe中的"foreign"域获取一个简单页面,
	 * 以便iframe中的代码可以从本地域运行到SockJS服务器.
	 * 由于iframe需要加载SockJS javascript客户端库, 因此该属性允许指定从何处加载它.
	 * <p>默认指向"https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js".
	 * 但是, 它也可以设置为指向应用程序提供的URL.
	 * <p>请注意, 可以指定相对URL, 在这种情况下, URL必须相对于iframe URL.
	 * 例如, 假设SockJS端点映射到"/sockjs", 并生成iframe URL "/sockjs/iframe.html",
	 * 则相对URL必须以"../../"开头, 以遍历到SockJS映射上方的位置.
	 * 在基于前缀的Servlet映射的情况下, 可能需要再遍历一次.
	 */
	public void setSockJsClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
	}

	/**
	 * 将URL返回到SockJS JavaScript客户端库.
	 */
	public String getSockJsClientLibraryUrl() {
		return this.clientLibraryUrl;
	}

	/**
	 * 流传输在客户端保存响应, 不释放传递的消息使用的内存.
	 * 这种传输需要偶尔回收连接.
	 * 此属性设置可在单个HTTP流式传输请求关闭之前发送的最小字节数.
	 * 之后客户端将打开一个新请求.
	 * 将此值设置为1可以有效地禁用流式传输, 并使流式传输的行为类似于轮询传输.
	 * <p>默认为 128K (i.e. 128 * 1024).
	 */
	public void setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
	}

	/**
	 * 返回在单个HTTP流式传输请求关闭之前发送的最小字节数.
	 */
	public int getStreamBytesLimit() {
		return this.streamBytesLimit;
	}

	/**
	 * SockJS协议要求服务器响应来自客户端的初始"/info"请求, 该请求具有"cookie_needed" boolean属性,
	 * 该属性指示是否需要使用 JSESSIONID cookie以使应用程序正常运行, e.g. 用于负载平衡或用于Java Servlet容器以使用HTTP会话.
	 * <p>这对于支持XDomainRequest的IE 8,9尤其重要 -- XDomainRequest是一个经过修改的AJAX/XHR, 它可以跨域执行请求但不发送任何cookie.
	 * 在这些情况下, SockJS客户端更喜欢"xdr-streaming"上的"iframe-htmlfile"传输, 以便能够发送cookie.
	 * <p>当此属性设置为true时, SockJS协议还期望SockJS服务回显JSESSIONID cookie.
	 * 但是, 当在Servlet容器中运行时, 这不是必需的, 因为容器会处理它.
	 * <p>默认值为"true", 以最大限度地提高应用程序在IE 8,9中正常工作的机会, 并支持cookie (特别是JSESSIONID cookie).
	 * 但是，如果不需要使用cookie (和HTTP会话), 应用程序可以选择将其设置为"false".
	 */
	public void setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
	}

	/**
	 * 返回是否需要JSESSIONID cookie才能使应用程序正常运行.
	 */
	public boolean isSessionCookieNeeded() {
		return this.sessionCookieNeeded;
	}

	/**
	 * 指定服务器未发送任何消息的时间量(以毫秒为单位), 之后服务器应向客户端发送心跳帧以防止连接中断.
	 * <p>默认为 25,000 (25 seconds).
	 */
	public void setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
	}

	/**
	 * 返回服务器未发送任何消息的时间量(以毫秒为单位).
	 */
	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	/**
	 * 在没有接收连接之后将客户端视为断开连接之前的时间量(以毫秒为单位), i.e. 服务器可以将数据发送到客户端的活动连接.
	 * <p>默认为 5000.
	 */
	public void setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
	}

	/**
	 * 返回客户端被视为断开连接之前的时间量(以毫秒为单位).
	 */
	public long getDisconnectDelay() {
		return this.disconnectDelay;
	}

	/**
	 * 在等待来自客户端的下一个HTTP轮询请求时, 会话可以缓存的服务器到客户端消息的数量.
	 * 所有HTTP传输都使用此属性, 因为即使是流传输也会定期回收HTTP请求.
	 * <p>HTTP请求之间的时间量应该相对较短, 并且不会超过允许的断开连接的延迟 (see {@link #setDisconnectDelay(long)});
	 * 默认为5秒.
	 * <p>默认为 100.
	 */
	public void setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
	}

	/**
	 * 返回HTTP消息缓存的大小.
	 */
	public int getHttpMessageCacheSize() {
		return this.httpMessageCacheSize;
	}

	/**
	 * 某些负载平衡器不支持WebSocket.
	 * 此选项可用于禁用服务器端的WebSocket传输.
	 * <p>默认为 "true".
	 */
	public void setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
	}

	/**
	 * 返回是否启用WebSocket传输.
	 */
	public boolean isWebSocketEnabled() {
		return this.webSocketEnabled;
	}

	/**
	 * 此选项可用于禁用为SockJS请求自动添加CORS header.
	 * <p>默认为 "false".
	 */
	public void setSuppressCors(boolean suppressCors) {
		this.suppressCors = suppressCors;
	}

	public boolean shouldSuppressCors() {
		return this.suppressCors;
	}

	/**
	 * 配置允许的{@code Origin} header值. 此检查主要是为浏览器设计的.
	 * 没有什么可以阻止其他类型的客户端修改{@code Origin} header值.
	 * <p>启用S​​ockJS并限制来源时, 将禁用不允许检查请求来源 (基于JSONP和Iframe的传输)的传输类型.
	 * 因此, 当来源受到限制时, 不支持IE 6到9.
	 * <p>每个提供的允许来源必须具有方案, 并且可选地具有端口
	 * (e.g. "http://example.org", "http://example.org:9090").
	 * 允许的来源字符串也可以是 "*", 在这种情况下允许所有来源.
	 * 
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.allowedOrigins.clear();
		this.allowedOrigins.addAll(allowedOrigins);
	}

	public Collection<String> getAllowedOrigins() {
		return Collections.unmodifiableSet(this.allowedOrigins);
	}


	/**
	 * 此方法确定SockJS路径并处理SockJS静态URL.
	 * 会话URL和原始WebSocket请求被委托给抽象方法.
	 */
	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			String sockJsPath, WebSocketHandler wsHandler) throws SockJsException {

		if (sockJsPath == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Expected SockJS path. Failing request: " + request.getURI());
			}
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		try {
			request.getHeaders();
		}
		catch (InvalidMediaTypeException ex) {
			// 根据SockJS协议, 内容类型可以忽略 (它总是json)
		}

		String requestInfo = (logger.isDebugEnabled() ? request.getMethod() + " " + request.getURI() : null);

		try {
			if (sockJsPath.equals("") || sockJsPath.equals("/")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				response.getHeaders().setContentType(new MediaType("text", "plain", UTF8_CHARSET));
				response.getBody().write("Welcome to SockJS!\n".getBytes(UTF8_CHARSET));
			}

			else if (sockJsPath.equals("/info")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				this.infoHandler.handle(request, response);
			}

			else if (sockJsPath.matches("/iframe[0-9-.a-z_]*.html")) {
				if (!this.allowedOrigins.isEmpty() && !this.allowedOrigins.contains("*")) {
					if (requestInfo != null) {
						logger.debug("Iframe support is disabled when an origin check is required. " +
								"Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				if (this.allowedOrigins.isEmpty()) {
					response.getHeaders().add(XFRAME_OPTIONS_HEADER, "SAMEORIGIN");
				}
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				this.iframeHandler.handle(request, response);
			}

			else if (sockJsPath.equals("/websocket")) {
				if (isWebSocketEnabled()) {
					if (requestInfo != null) {
						logger.debug("Processing transport request: " + requestInfo);
					}
					handleRawWebSocketRequest(request, response, wsHandler);
				}
				else if (requestInfo != null) {
					logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
				}
			}

			else {
				String[] pathSegments = StringUtils.tokenizeToStringArray(sockJsPath.substring(1), "/");
				if (pathSegments.length != 3) {
					if (logger.isWarnEnabled()) {
						logger.warn("Invalid SockJS path '" + sockJsPath + "' - required to have 3 path segments");
					}
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}

				String serverId = pathSegments[0];
				String sessionId = pathSegments[1];
				String transport = pathSegments[2];

				if (!isWebSocketEnabled() && transport.equals("websocket")) {
					if (requestInfo != null) {
						logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				else if (!validateRequest(serverId, sessionId, transport) || !validatePath(request)) {
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}

				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				handleTransportRequest(request, response, wsHandler, sessionId, transport);
			}
			response.close();
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to write to the response", null, ex);
		}
	}

	protected boolean validateRequest(String serverId, String sessionId, String transport) {
		if (!StringUtils.hasText(serverId) || !StringUtils.hasText(sessionId) || !StringUtils.hasText(transport)) {
			logger.warn("No server, session, or transport path segment in SockJS request.");
			return false;
		}

		// Server and session id's must not contain "."
		if (serverId.contains(".") || sessionId.contains(".")) {
			logger.warn("Either server or session contains a \".\" which is not allowed by SockJS protocol.");
			return false;
		}

		return true;
	}

	/**
	 * 确保路径不包含文件扩展名, 可以是文件名 (e.g. "/jsonp.bat"), 也可能是路径参数 ("/jsonp;Setup.bat")之后, 它们可用于RFD攻击.
	 * <p>由于路径的最后部分应该是传输类型, 因此扩展的存在将不起作用.
	 * 需要做的就是检查是否有任何路径参数, 这些参数在请求映射期间已从SockJS路径中删除, 如果找到则拒绝请求.
	 */
	private boolean validatePath(ServerHttpRequest request) {
		String path = request.getURI().getPath();
		int index = path.lastIndexOf('/') + 1;
		String filename = path.substring(index);
		return (filename.indexOf(';') == -1);
	}

	protected boolean checkOrigin(ServerHttpRequest request, ServerHttpResponse response, HttpMethod... httpMethods)
			throws IOException {

		if (WebUtils.isSameOrigin(request)) {
			return true;
		}

		if (!WebUtils.isValidOrigin(request, this.allowedOrigins)) {
			if (logger.isWarnEnabled()) {
				logger.warn("Origin header value '" + request.getHeaders().getOrigin() + "' not allowed.");
			}
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return false;
		}

		return true;
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		if (!this.suppressCors && CorsUtils.isCorsRequest(request)) {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(new ArrayList<String>(this.allowedOrigins));
			config.addAllowedMethod("*");
			config.setAllowCredentials(true);
			config.setMaxAge(ONE_YEAR);
			config.addAllowedHeader("*");
			return config;
		}
		return null;
	}

	protected void addCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("public, max-age=" + ONE_YEAR);
		response.getHeaders().setExpires(new Date().getTime() + ONE_YEAR * 1000);
	}

	protected void addNoCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
	}

	protected void sendMethodNotAllowed(ServerHttpResponse response, HttpMethod... httpMethods) {
		logger.warn("Sending Method Not Allowed (405)");
		response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
		response.getHeaders().setAllow(new LinkedHashSet<HttpMethod>(Arrays.asList(httpMethods)));
	}


	/**
	 * 处理原始WebSocket通信的请求, i.e. 没有任何SockJS消息帧.
	 */
	protected abstract void handleRawWebSocketRequest(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler webSocketHandler) throws IOException;

	/**
	 * 处理SockJS会话URL (i.e. 特定于传输的请求).
	 */
	protected abstract void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, String sessionId, String transport) throws SockJsException;


	private interface SockJsRequestHandler {

		void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException;
	}


	private final SockJsRequestHandler infoHandler = new SockJsRequestHandler() {

		private static final String INFO_CONTENT =
				"{\"entropy\":%s,\"origins\":[\"*:*\"],\"cookie_needed\":%s,\"websocket\":%s}";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (request.getMethod() == HttpMethod.GET) {
				addNoCacheHeaders(response);
				if (checkOrigin(request, response)) {
					response.getHeaders().setContentType(new MediaType("application", "json", UTF8_CHARSET));
					String content = String.format(
							INFO_CONTENT, random.nextInt(), isSessionCookieNeeded(), isWebSocketEnabled());
					response.getBody().write(content.getBytes());
				}

			}
			else if (request.getMethod() == HttpMethod.OPTIONS) {
				if (checkOrigin(request, response)) {
					addCacheHeaders(response);
					response.setStatusCode(HttpStatus.NO_CONTENT);
				}
			}
			else {
				sendMethodNotAllowed(response, HttpMethod.GET, HttpMethod.OPTIONS);
			}
		}
	};


	private final SockJsRequestHandler iframeHandler = new SockJsRequestHandler() {

		private static final String IFRAME_CONTENT =
				"<!DOCTYPE html>\n" +
		        "<html>\n" +
		        "<head>\n" +
		        "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
		        "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
		        "  <script>\n" +
		        "    document.domain = document.domain;\n" +
		        "    _sockjs_onload = function(){SockJS.bootstrap_iframe();};\n" +
		        "  </script>\n" +
		        "  <script src=\"%s\"></script>\n" +
		        "</head>\n" +
		        "<body>\n" +
		        "  <h2>Don't panic!</h2>\n" +
		        "  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>\n" +
		        "</body>\n" +
		        "</html>";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (request.getMethod() != HttpMethod.GET) {
				sendMethodNotAllowed(response, HttpMethod.GET);
				return;
			}

			String content = String.format(IFRAME_CONTENT, getSockJsClientLibraryUrl());
			byte[] contentBytes = content.getBytes(UTF8_CHARSET);
			StringBuilder builder = new StringBuilder("\"0");
			DigestUtils.appendMd5DigestAsHex(contentBytes, builder);
			builder.append('"');
			String etagValue = builder.toString();

			List<String> ifNoneMatch = request.getHeaders().getIfNoneMatch();
			if (!CollectionUtils.isEmpty(ifNoneMatch) && ifNoneMatch.get(0).equals(etagValue)) {
				response.setStatusCode(HttpStatus.NOT_MODIFIED);
				return;
			}

			response.getHeaders().setContentType(new MediaType("text", "html", UTF8_CHARSET));
			response.getHeaders().setContentLength(contentBytes.length);

			// 没有缓存, 以便每次检查IFrame是否被授权
			addNoCacheHeaders(response);
			response.getHeaders().setETag(etagValue);
			response.getBody().write(contentBytes);
		}
	};
}
