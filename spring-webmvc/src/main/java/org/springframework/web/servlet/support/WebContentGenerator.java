package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * 适用于任何类型的Web内容生成器的便捷超类,
 * 如{@link org.springframework.web.servlet.mvc.AbstractController}
 * 和{@link org.springframework.web.servlet.mvc.WebContentInterceptor}.
 * 也可以用于拥有自己的{@link org.springframework.web.servlet.HandlerAdapter}的自定义处理器.
 *
 * <p>支持HTTP缓存控制选项.
 * 可以通过{@link #setCacheSeconds "cacheSeconds"}和{@link #setCacheControl "cacheControl"}属性来控制相应HTTP header的使用.
 *
 * <p><b>NOTE:</b> 从Spring 4.2开始, 当仅使用{@link #setCacheSeconds}时, 此生成器的默认行为发生了变化,
 * 发送了与当前浏览器和代理实现一致的HTTP响应header (i.e. 不再有HTTP 1.0 header).
 * 使用其中一种新弃用的方法{@link #setUseExpiresHeader}, {@link #setUseCacheControlHeader},
 * {@link #setUseCacheControlNoStore} 或 {@link #setAlwaysMustRevalidate}可以轻松恢复以前的行为.
 */
public abstract class WebContentGenerator extends WebApplicationObjectSupport {

	/** HTTP method "GET" */
	public static final String METHOD_GET = "GET";

	/** HTTP method "HEAD" */
	public static final String METHOD_HEAD = "HEAD";

	/** HTTP method "POST" */
	public static final String METHOD_POST = "POST";

	private static final String HEADER_PRAGMA = "Pragma";

	private static final String HEADER_EXPIRES = "Expires";

	protected static final String HEADER_CACHE_CONTROL = "Cache-Control";

	/** 检查 Servlet 3.0+ HttpServletResponse.getHeaders(String) */
	private static final boolean servlet3Present =
			ClassUtils.hasMethod(HttpServletResponse.class, "getHeaders", String.class);


	/** 受支持的HTTP方法 */
	private Set<String> supportedMethods;

	private String allowHeader;

	private boolean requireSession = false;

	private CacheControl cacheControl;

	private int cacheSeconds = -1;

	private String[] varyByRequestHeaders;


	// deprecated fields

	/** Use HTTP 1.0 expires header? */
	private boolean useExpiresHeader = false;

	/** Use HTTP 1.1 cache-control header? */
	private boolean useCacheControlHeader = true;

	/** Use HTTP 1.1 cache-control header value "no-store"? */
	private boolean useCacheControlNoStore = true;

	private boolean alwaysMustRevalidate = false;


	/**
	 * 创建一个新的WebContentGenerator, 它默认支持HTTP方法GET, HEAD和POST.
	 */
	public WebContentGenerator() {
		this(true);
	}

	/**
	 * @param restrictDefaultSupportedMethods {@code true} 如果此生成器默认应支持HTTP方法GET, HEAD和POST;
	 * 或者{@code false}如果它应该不受限制
	 */
	public WebContentGenerator(boolean restrictDefaultSupportedMethods) {
		if (restrictDefaultSupportedMethods) {
			this.supportedMethods = new LinkedHashSet<String>(4);
			this.supportedMethods.add(METHOD_GET);
			this.supportedMethods.add(METHOD_HEAD);
			this.supportedMethods.add(METHOD_POST);
		}
		initAllowHeader();
	}

	/**
	 * @param supportedMethods 此内容生成器支持的HTTP方法
	 */
	public WebContentGenerator(String... supportedMethods) {
		setSupportedMethods(supportedMethods);
	}


	/**
	 * 设置此内容生成器应支持的HTTP方法.
	 * <p>对于简单的表单控制器类型, 默认为GET, HEAD和POST; 一般控制器和拦截器不受限制.
	 */
	public final void setSupportedMethods(String... methods) {
		if (!ObjectUtils.isEmpty(methods)) {
			this.supportedMethods = new LinkedHashSet<String>(Arrays.asList(methods));
		}
		else {
			this.supportedMethods = null;
		}
		initAllowHeader();
	}

	/**
	 * 返回此内容生成器支持的HTTP方法.
	 */
	public final String[] getSupportedMethods() {
		return StringUtils.toStringArray(this.supportedMethods);
	}

	private void initAllowHeader() {
		Collection<String> allowedMethods;
		if (this.supportedMethods == null) {
			allowedMethods = new ArrayList<String>(HttpMethod.values().length - 1);
			for (HttpMethod method : HttpMethod.values()) {
				if (method != HttpMethod.TRACE) {
					allowedMethods.add(method.name());
				}
			}
		}
		else if (this.supportedMethods.contains(HttpMethod.OPTIONS.name())) {
			allowedMethods = this.supportedMethods;
		}
		else {
			allowedMethods = new ArrayList<String>(this.supportedMethods);
			allowedMethods.add(HttpMethod.OPTIONS.name());

		}
		this.allowHeader = StringUtils.collectionToCommaDelimitedString(allowedMethods);
	}

	/**
	 * 返回"Allow" header值, 以便根据配置的{@link #setSupportedMethods 支持的方法}响应HTTP OPTIONS请求,
	 * 即使不作为支持的方法存在, 也会自动将"OPTIONS"添加到列表中.
	 * 这意味着只要在调用{@link #checkRequest(HttpServletRequest)}之前处理HTTP OPTIONS请求,
	 * 子类就不必显式地将"OPTIONS"列为受支持的方法.
	 */
	protected String getAllowHeader() {
		return this.allowHeader;
	}

	/**
	 * 设置是否需要会话来处理请求.
	 */
	public final void setRequireSession(boolean requireSession) {
		this.requireSession = requireSession;
	}

	/**
	 * 返回是否需要会话来处理请求.
	 */
	public final boolean isRequireSession() {
		return this.requireSession;
	}

	/**
	 * 设置{@link org.springframework.http.CacheControl}实例以构建Cache-Control HTTP响应header.
	 */
	public final void setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * 获取构建Cache-Control HTTP响应header的{@link org.springframework.http.CacheControl}实例.
	 */
	public final CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * 通过将与缓存相关的HTTP header写入响应, 缓存内容给定的秒数:
	 * <ul>
	 * <li>seconds == -1 (默认值): 没有与生成缓存相关的header</li>
	 * <li>seconds == 0: "Cache-Control: no-store" 将禁止缓存</li>
	 * <li>seconds > 0: "Cache-Control: max-age=seconds" 将缓存内容</li>
	 * </ul>
	 * <p>对于更具体的需求, 应使用自定义{@link org.springframework.http.CacheControl}.
	 */
	public final void setCacheSeconds(int seconds) {
		this.cacheSeconds = seconds;
	}

	/**
	 * 返回缓存内容的秒数.
	 */
	public final int getCacheSeconds() {
		return this.cacheSeconds;
	}

	/**
	 * 配置一个或多个请求header名称 (e.g. "Accept-Language")以添加到"Vary"响应header,
	 * 以通知客户端响应受内容协商和基于给定请求header的值的差异的影响.
	 * 仅当响应"Vary" header中尚未存在时, 才会添加配置的请求header名称.
	 * <p><strong>Note:</strong> 此属性仅在Servlet 3.0+上受支持, 它允许检查现有的响应header值.
	 * 
	 * @param varyByRequestHeaders 一个或多个请求header名称
	 */
	public final void setVaryByRequestHeaders(String... varyByRequestHeaders) {
		this.varyByRequestHeaders = varyByRequestHeaders;
	}

	/**
	 * 返回"Vary" 响应 header的配置的请求header名称.
	 */
	public final String[] getVaryByRequestHeaders() {
		return this.varyByRequestHeaders;
	}

	/**
	 * 设置是否使用HTTP 1.0 expires标头.
	 * 默认为"false", 截至4.2.
	 * <p>Note: 只有在为当前请求启用 (或明确禁止)缓存时, 才会应用缓存header.
	 * 
	 * @deprecated 从4.2开始, 从那时起, 将需要 HTTP 1.1 cache-control header, HTTP 1.0 header将消失
	 */
	@Deprecated
	public final void setUseExpiresHeader(boolean useExpiresHeader) {
		this.useExpiresHeader = useExpiresHeader;
	}

	/**
	 * 返回是否使用HTTP 1.0 expires header.
	 * 
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseExpiresHeader() {
		return this.useExpiresHeader;
	}

	/**
	 * 设置是否使用HTTP 1.1 cache-control header. 默认为"true".
	 * <p>Note: 只有在为当前请求启用 (或明确禁止) 缓存时, 才会应用缓存header.
	 * 
	 * @deprecated 从4.2开始, 从那时起, 将需要 HTTP 1.1 cache-control header, HTTP 1.0 header将消失
	 */
	@Deprecated
	public final void setUseCacheControlHeader(boolean useCacheControlHeader) {
		this.useCacheControlHeader = useCacheControlHeader;
	}

	/**
	 * 返回是否使用HTTP 1.1 cache-control header.
	 * 
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseCacheControlHeader() {
		return this.useCacheControlHeader;
	}

	/**
	 * 设置在禁止缓存时是否使用HTTP 1.1 cache-control header值"no-store". 默认为"true".
	 * 
	 * @deprecated as of 4.2, in favor of {@link #setCacheControl}
	 */
	@Deprecated
	public final void setUseCacheControlNoStore(boolean useCacheControlNoStore) {
		this.useCacheControlNoStore = useCacheControlNoStore;
	}

	/**
	 * 返回是否使用HTTP 1.1 cache-control header值 "no-store".
	 * 
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseCacheControlNoStore() {
		return this.useCacheControlNoStore;
	}

	/**
	 * 添加'must-revalidate'到每个Cache-Control header的选项.
	 * 这对于带注解的控制器方法可能很有用, 它可以以编程方式计算 last-modified, 如
	 * {@link org.springframework.web.context.request.WebRequest#checkNotModified(long)}中所述.
	 * <p>默认为"false".
	 * 
	 * @deprecated as of 4.2, in favor of {@link #setCacheControl}
	 */
	@Deprecated
	public final void setAlwaysMustRevalidate(boolean mustRevalidate) {
		this.alwaysMustRevalidate = mustRevalidate;
	}

	/**
	 * 返回是否将'must-revalidate'添加到每个 Cache-Control header.
	 * 
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isAlwaysMustRevalidate() {
		return this.alwaysMustRevalidate;
	}


	/**
	 * 检查给定的请求以获取支持的方法和必需的会话.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @throws ServletException 如果由于检查失败而无法处理请求
	 */
	protected final void checkRequest(HttpServletRequest request) throws ServletException {
		// 检查是否应该支持请求方法.
		String method = request.getMethod();
		if (this.supportedMethods != null && !this.supportedMethods.contains(method)) {
			throw new HttpRequestMethodNotSupportedException(method, this.supportedMethods);
		}

		// 检查是否需要会话.
		if (this.requireSession && request.getSession(false) == null) {
			throw new HttpSessionRequiredException("Pre-existing session required but none found");
		}
	}

	/**
	 * 根据此生成器的设置准备给定的响应.
	 * 应用为此生成器指定的缓存秒数.
	 * 
	 * @param response 当前的HTTP请求
	 */
	protected final void prepareResponse(HttpServletResponse response) {
		if (this.cacheControl != null) {
			applyCacheControl(response, this.cacheControl);
		}
		else {
			applyCacheSeconds(response, this.cacheSeconds);
		}
		if (servlet3Present && this.varyByRequestHeaders != null) {
			for (String value : getVaryRequestHeadersToAdd(response)) {
				response.addHeader("Vary", value);
			}
		}
	}

	/**
	 * 根据给定的设置设置HTTP Cache-Control header.
	 * 
	 * @param response 当前的HTTP请求
	 * @param cacheControl 预配置的缓存控制设置
	 */
	protected final void applyCacheControl(HttpServletResponse response, CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			// Set computed HTTP 1.1 Cache-Control header
			response.setHeader(HEADER_CACHE_CONTROL, ccValue);

			if (response.containsHeader(HEADER_PRAGMA)) {
				// Reset HTTP 1.0 Pragma header if present
				response.setHeader(HEADER_PRAGMA, "");
			}
			if (response.containsHeader(HEADER_EXPIRES)) {
				// Reset HTTP 1.0 Expires header if present
				response.setHeader(HEADER_EXPIRES, "");
			}
		}
	}

	/**
	 * 应用给定的缓存秒数, 并生成相应的HTTP header,
	 * i.e. 在正值的情况下允许缓存给定的秒数, 如果给定0值则禁止缓存, 其他值时不执行任何操作.
	 * 不告诉浏览器重新验证资源.
	 * 
	 * @param response 当前的HTTP响应
	 * @param cacheSeconds 缓存响应的秒数, 0 表示禁止缓存
	 */
	@SuppressWarnings("deprecation")
	protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds) {
		if (this.useExpiresHeader || !this.useCacheControlHeader) {
			// 与以前的Spring版本一样, 不推荐使用HTTP 1.0缓存行为
			if (cacheSeconds > 0) {
				cacheForSeconds(response, cacheSeconds);
			}
			else if (cacheSeconds == 0) {
				preventCaching(response);
			}
		}
		else {
			CacheControl cControl;
			if (cacheSeconds > 0) {
				cControl = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS);
				if (this.alwaysMustRevalidate) {
					cControl = cControl.mustRevalidate();
				}
			}
			else if (cacheSeconds == 0) {
				cControl = (this.useCacheControlNoStore ? CacheControl.noStore() : CacheControl.noCache());
			}
			else {
				cControl = CacheControl.empty();
			}
			applyCacheControl(response, cControl);
		}
	}


	/**
	 * @deprecated 从4.2开始, 由于{@code lastModified}标志被有效忽略, 因此只有在显式配置时才会生成must-revalidate header
	 */
	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, boolean lastModified) throws ServletException {

		checkRequest(request);
		prepareResponse(response);
	}

	/**
	 * @deprecated 从4.2开始, 由于{@code lastModified}标志被有效忽略, 因此只有在显式配置时才会生成must-revalidate header
	 */
	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, int cacheSeconds, boolean lastModified)
			throws ServletException {

		checkRequest(request);
		applyCacheSeconds(response, cacheSeconds);
	}

	/**
	 * 应用给定的缓存秒数, 并生成相应的HTTP header.
	 * <p>也就是说, 在正值的情况下允许缓存给定的秒数, 如果给定0值则禁止缓存, 否则什么也不做 (i.e. 将缓存留给客户端).
	 * 
	 * @param response 当前的HTTP响应
	 * @param cacheSeconds 响应应该可缓存的秒数; 0以禁止缓存; 负值将缓存留给客户端.
	 * @param mustRevalidate 客户端是否应该重新验证资源 (通常仅对具有last-modified支持的控制器是必需的)
	 * 
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds, boolean mustRevalidate) {
		if (cacheSeconds > 0) {
			cacheForSeconds(response, cacheSeconds, mustRevalidate);
		}
		else if (cacheSeconds == 0) {
			preventCaching(response);
		}
	}

	/**
	 * 设置HTTP header以允许缓存给定的秒数.
	 * 不告诉浏览器重新验证资源.
	 * 
	 * @param response 当前的HTTP响应
	 * @param seconds 响应应该缓存的秒数
	 * 
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void cacheForSeconds(HttpServletResponse response, int seconds) {
		cacheForSeconds(response, seconds, false);
	}

	/**
	 * 设置允许缓存给定的秒数的HTTP header.
	 * 如果mustRevalidate为{@code true}, 则告诉浏览器重新验证资源.
	 * 
	 * @param response 当前的HTTP响应
	 * @param seconds 响应应该缓存的秒数
	 * @param mustRevalidate 客户端是否应该重新验证资源 (通常仅对具有last-modified支持的控制器是必需的)
	 * 
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void cacheForSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (this.useExpiresHeader) {
			// HTTP 1.0 header
			response.setDateHeader(HEADER_EXPIRES, System.currentTimeMillis() + seconds * 1000L);
		}
		else if (response.containsHeader(HEADER_EXPIRES)) {
			// Reset HTTP 1.0 Expires header if present
			response.setHeader(HEADER_EXPIRES, "");
		}

		if (this.useCacheControlHeader) {
			// HTTP 1.1 header
			String headerValue = "max-age=" + seconds;
			if (mustRevalidate || this.alwaysMustRevalidate) {
				headerValue += ", must-revalidate";
			}
			response.setHeader(HEADER_CACHE_CONTROL, headerValue);
		}

		if (response.containsHeader(HEADER_PRAGMA)) {
			// Reset HTTP 1.0 Pragma header if present
			response.setHeader(HEADER_PRAGMA, "");
		}
	}

	/**
	 * 防止响应被缓存.
	 * 仅在HTTP 1.0兼容模式下调用.
	 * <p>See {@code http://www.mnot.net/cache_docs}.
	 * 
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void preventCaching(HttpServletResponse response) {
		response.setHeader(HEADER_PRAGMA, "no-cache");

		if (this.useExpiresHeader) {
			// HTTP 1.0 Expires header
			response.setDateHeader(HEADER_EXPIRES, 1L);
		}

		if (this.useCacheControlHeader) {
			// HTTP 1.1 Cache-Control header: "no-cache"是标准值, "no-store"是防止Firefox上缓存的必要条件.
			response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
			if (this.useCacheControlNoStore) {
				response.addHeader(HEADER_CACHE_CONTROL, "no-store");
			}
		}
	}


	private Collection<String> getVaryRequestHeadersToAdd(HttpServletResponse response) {
		if (!response.containsHeader(HttpHeaders.VARY)) {
			return Arrays.asList(getVaryByRequestHeaders());
		}
		Collection<String> result = new ArrayList<String>(getVaryByRequestHeaders().length);
		Collections.addAll(result, getVaryByRequestHeaders());
		for (String header : response.getHeaders(HttpHeaders.VARY)) {
			for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
				if ("*".equals(existing)) {
					return Collections.emptyList();
				}
				for (String value : getVaryByRequestHeaders()) {
					if (value.equalsIgnoreCase(existing)) {
						result.remove(value);
					}
				}
			}
		}
		return result;
	}

}
