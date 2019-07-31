package org.springframework.web.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

/**
 * {@code Filter}的基类, 用于在处理请求之前和之后执行日志记录操作.
 *
 * <p>子类应覆盖{@code beforeRequest(HttpServletRequest, String)}
 * 和{@code afterRequest(HttpServletRequest, String)}方法以执行实际的请求记录.
 *
 * <p>子类被传递消息以写入{@code beforeRequest}和{@code afterRequest}方法中的日志.
 * 默认仅记录请求的URI.
 * 但是, 将{@code includeQueryString}属性设置为{@code true}将导致请求的查询字符串也包含在内.
 * 可以通过{@code includePayload}标志记录请求的有效负载 (正文).
 * 请注意, 这只会记录读取的内容, 这可能不是整个有效负载.
 *
 * <p>可以使用{@code beforeMessagePrefix}, {@code afterMessagePrefix}, {@code beforeMessageSuffix}
 * 和{@code afterMessageSuffix}属性配置前后消息的前缀和后缀.
 */
public abstract class AbstractRequestLoggingFilter extends OncePerRequestFilter {

	public static final String DEFAULT_BEFORE_MESSAGE_PREFIX = "Before request [";

	public static final String DEFAULT_BEFORE_MESSAGE_SUFFIX = "]";

	public static final String DEFAULT_AFTER_MESSAGE_PREFIX = "After request [";

	public static final String DEFAULT_AFTER_MESSAGE_SUFFIX = "]";

	private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 50;


	private boolean includeQueryString = false;

	private boolean includeClientInfo = false;

	private boolean includeHeaders = false;

	private boolean includePayload = false;

	private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

	private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

	private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

	private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

	private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;


	/**
	 * 设置查询字符串是否应包含在日志消息中.
	 * <p>应使用{@code <init-param>}为{@code web.xml}中的过滤器定义中的参数名称"includeQueryString"进行配置.
	 */
	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	/**
	 * 返回查询字符串是否应包含在日志消息中.
	 */
	protected boolean isIncludeQueryString() {
		return this.includeQueryString;
	}

	/**
	 * 设置客户端地址和会话ID是否应包含在日志消息中.
	 * <p>应使用{@code <init-param>}为{@code web.xml}中的过滤器定义中的参数名称"includeClientInfo"进行配置.
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * 返回客户端地址和会话ID是否应包含在日志消息中.
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}

	/**
	 * 设置是否应将请求header包含在日志消息中.
	 * <p>应使用{@code <init-param>}为{@code web.xml}中的过滤器定义中的参数名称"includeHeaders"进行配置.
	 */
	public void setIncludeHeaders(boolean includeHeaders) {
		this.includeHeaders = includeHeaders;
	}

	/**
	 * 返回请求header是否应包含在日志消息中.
	 */
	public boolean isIncludeHeaders() {
		return this.includeHeaders;
	}

	/**
	 * 设置请求有效负载 (正文) 是否应包含在日志消息中.
	 * <p>应使用{@code <init-param>}为{@code web.xml}中的过滤器定义中的参数名称"includePayload"进行配置.
	 */
	public void setIncludePayload(boolean includePayload) {
		this.includePayload = includePayload;
	}

	/**
	 * 返回请求有效负载 (正文)是否应包含在日志消息中.
	 */
	protected boolean isIncludePayload() {
		return this.includePayload;
	}

	/**
	 * 设置要包含在日志消息中的有效负载主体的最大长度.
	 * 默认50 字符.
	 */
	public void setMaxPayloadLength(int maxPayloadLength) {
		Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' should be larger than or equal to 0");
		this.maxPayloadLength = maxPayloadLength;
	}

	/**
	 * 返回要包含在日志消息中的有效负载主体的最大长度.
	 */
	protected int getMaxPayloadLength() {
		return this.maxPayloadLength;
	}

	/**
	 * 设置应该在处理请求之前写入日志消息的前面的值.
	 */
	public void setBeforeMessagePrefix(String beforeMessagePrefix) {
		this.beforeMessagePrefix = beforeMessagePrefix;
	}

	/**
	 * 设置应在处理请求之前写入日志消息的后面的值.
	 */
	public void setBeforeMessageSuffix(String beforeMessageSuffix) {
		this.beforeMessageSuffix = beforeMessageSuffix;
	}

	/**
	 * 设置应在处理请求后写入日志消息的前面的值.
	 */
	public void setAfterMessagePrefix(String afterMessagePrefix) {
		this.afterMessagePrefix = afterMessagePrefix;
	}

	/**
	 * 设置应在处理请求后写入日志消息的后面的值.
	 */
	public void setAfterMessageSuffix(String afterMessageSuffix) {
		this.afterMessageSuffix = afterMessageSuffix;
	}


	/**
	 * 默认值为"false", 以便过滤器可以在请求处理开始时记录"before"消息,
	 * 并在最后一个异步调度线程退出时的最后记录"after"消息.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 将请求转发到链中的下一个过滤器, 并委托给子类以在处理请求之前和之后执行实际请求记录.
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isFirstRequest = !isAsyncDispatch(request);
		HttpServletRequest requestToUse = request;

		if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
			requestToUse = new ContentCachingRequestWrapper(request, getMaxPayloadLength());
		}

		boolean shouldLog = shouldLog(requestToUse);
		if (shouldLog && isFirstRequest) {
			beforeRequest(requestToUse, getBeforeMessage(requestToUse));
		}
		try {
			filterChain.doFilter(requestToUse, response);
		}
		finally {
			if (shouldLog && !isAsyncStarted(requestToUse)) {
				afterRequest(requestToUse, getAfterMessage(requestToUse));
			}
		}
	}

	/**
	 * 获取在请求之前要写入日志的消息.
	 */
	private String getBeforeMessage(HttpServletRequest request) {
		return createMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	/**
	 * 获取请求后写入日志的消息.
	 */
	private String getAfterMessage(HttpServletRequest request) {
		return createMessage(request, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	/**
	 * 为给定的请求, 前缀和后缀创建日志消息.
	 * <p>如果{@code includeQueryString}是{@code true}, 那么日志消息的内部部分将采用{@code request_uri?query_string}的形式;
	 * 否则消息将只是{@code request_uri}的形式.
	 * <p>最终消息由所描述的内部部分和提供的前缀和后缀组成.
	 */
	protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);
		msg.append("uri=").append(request.getRequestURI());

		if (isIncludeQueryString()) {
			String queryString = request.getQueryString();
			if (queryString != null) {
				msg.append('?').append(queryString);
			}
		}

		if (isIncludeClientInfo()) {
			String client = request.getRemoteAddr();
			if (StringUtils.hasLength(client)) {
				msg.append(";client=").append(client);
			}
			HttpSession session = request.getSession(false);
			if (session != null) {
				msg.append(";session=").append(session.getId());
			}
			String user = request.getRemoteUser();
			if (user != null) {
				msg.append(";user=").append(user);
			}
		}

		if (isIncludeHeaders()) {
			msg.append(";headers=").append(new ServletServerHttpRequest(request).getHeaders());
		}

		if (isIncludePayload()) {
			ContentCachingRequestWrapper wrapper =
					WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
			if (wrapper != null) {
				byte[] buf = wrapper.getContentAsByteArray();
				if (buf.length > 0) {
					int length = Math.min(buf.length, getMaxPayloadLength());
					String payload;
					try {
						payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
					}
					catch (UnsupportedEncodingException ex) {
						payload = "[unknown]";
					}
					msg.append(";payload=").append(payload);
				}
			}
		}

		msg.append(suffix);
		return msg.toString();
	}


	/**
	 * 确定是否为当前请求调用{@link #beforeRequest}/{@link #afterRequest}方法,
	 * i.e. 日志记录当前是否处于活动状态 (并且日志消息值得构建).
	 * <p>默认实现始终返回{@code true}. 子类可以通过日志级别检查来覆盖它.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return {@code true} 如果应该调用before/after方法; 否则{@code false}
	 */
	protected boolean shouldLog(HttpServletRequest request) {
		return true;
	}

	/**
	 * 具体的子类应该实现此方法以在处理请求之前写入日志消息.
	 * 
	 * @param request 当前的HTTP请求
	 * @param message 要记录的消息
	 */
	protected abstract void beforeRequest(HttpServletRequest request, String message);

	/**
	 * 具体的子类应该实现此方法以在处理请求之后写入日志消息.
	 * 
	 * @param request 当前的HTTP请求
	 * @param message 要记录的消息
	 */
	protected abstract void afterRequest(HttpServletRequest request, String message);

}
