package org.springframework.http;

import java.util.concurrent.TimeUnit;

import org.springframework.util.StringUtils;

/**
 * 用于创建"Cache-Control" HTTP响应header的构建器.
 *
 * <p>向HTTP响应添加Cache-Control指令可以显着改善与Web应用程序交互时的客户端体验.
 * 此构建器仅创建具有响应指令的固定"Cache-Control" header, 并考虑了几个用例.
 *
 * <ul>
 * <li>缓存HTTP响应{@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS)}将导致{@code Cache-Control: "max-age=3600"}</li>
 * <li>禁止使用缓存{@code CacheControl cc = CacheControl.noStore()}将导致{@code Cache-Control: "no-store"}</li>
 * <li>诸如{@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform().cachePublic()}
 * 等高级案例将导致{@code Cache-Control: "max-age=3600, no-transform, public"}</li>
 * </ul>
 *
 * <p>请注意, 为了提高效率, 应该沿着HTTP验证器编写Cache-Control header, 例如"Last-Modified"或"ETag" header.
 */
public class CacheControl {

	private long maxAge = -1;

	private boolean noCache = false;

	private boolean noStore = false;

	private boolean mustRevalidate = false;

	private boolean noTransform = false;

	private boolean cachePublic = false;

	private boolean cachePrivate = false;

	private boolean proxyRevalidate = false;

	private long staleWhileRevalidate = -1;

	private long staleIfError = -1;

	private long sMaxAge = -1;


	protected CacheControl() {
	}


	/**
	 * <p>这非常适合使用没有"max-age", "no-cache" 或 "no-store"的其他可选指令.
	 * 
	 * @return {@code this}, 方便链接
	 */
	public static CacheControl empty() {
		return new CacheControl();
	}

	/**
	 * 添加"max-age="指令.
	 * <p>该指令非常适合公共缓存资源, 因为它们知道它们不会在配置的时间内发生变化.
	 * 如果资源不应通过共享缓存进行缓存({@link #cachePrivate()}) 或转换({@link #noTransform()}), 也可以使用其他指令.
	 * <p>为了防止缓存重用缓存的响应, 即使它已经过时 (i.e. 超过"max-age"延迟), 应该设置"must-revalidate"指令 ({@link #mustRevalidate()}
	 * 
	 * @param maxAge 应缓存响应的最长时间
	 * @param unit {@code maxAge}参数的时间单位
	 * 
	 * @return {@code this}, 方便链接
	 */
	public static CacheControl maxAge(long maxAge, TimeUnit unit) {
		CacheControl cc = new CacheControl();
		cc.maxAge = unit.toSeconds(maxAge);
		return cc;
	}

	/**
	 * 添加"no-cache"指令.
	 * <p>此指令非常适合告诉缓存, 只有在客户端使用服务器重新验证响应时, 才能重用响应.
	 * 该指令不会完全禁用缓存, 以及可能导致客户端发送条件请求 (带有"ETag", "If-Modified-Since" header)
	 * 和服务器响应"304 - Not Modified"状态.
	 * <p>为了禁用缓存并最小化请求/响应交换, 应使用{@link #noStore()}指令而不是{@code #noCache()}.
	 * 
	 * @return {@code this}, 方便链接
	 */
	public static CacheControl noCache() {
		CacheControl cc = new CacheControl();
		cc.noCache = true;
		return cc;
	}

	/**
	 * 添加"no-store"指令.
	 * <p>该指令非常适合禁止缓存 (浏览器和代理)响应内容.
	 * 
	 * @return {@code this}, 方便链接
	 */
	public static CacheControl noStore() {
		CacheControl cc = new CacheControl();
		cc.noStore = true;
		return cc;
	}


	/**
	 * 添加"must-revalidate"指令.
	 * <p>该指令表明一旦它过期, 缓存不得使用响应满足后续请求, 而不在源服务器上成功验证.
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl mustRevalidate() {
		this.mustRevalidate = true;
		return this;
	}

	/**
	 * 添加"no-transform"指令.
	 * <p>该指令表明中介 (缓存和其他) 不应转换响应内容.
	 * 这可能有助于强制缓存和CDN不自动gzip或优化响应内容.
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl noTransform() {
		this.noTransform = true;
		return this;
	}

	/**
	 * 添加"public"指令.
	 * <p>该指令指示任何缓存都可以存储响应, 即使响应通常是不可缓存的或只能在私有缓存中缓存.
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl cachePublic() {
		this.cachePublic = true;
		return this;
	}

	/**
	 * 添加"private"指令.
	 * <p>该指令指示响应消息仅供单个用户使用, 并且不得由共享缓存存储.
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl cachePrivate() {
		this.cachePrivate = true;
		return this;
	}

	/**
	 * 添加"proxy-revalidate"指令.
	 * <p>该指令与"must-revalidate"指令具有相同的含义, 但它不适用于私有缓存 (i.e. 浏览器, HTTP客户端).
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl proxyRevalidate() {
		this.proxyRevalidate = true;
		return this;
	}

	/**
	 * 添加"s-maxage"指令.
	 * <p>该指令指示在共享缓存中, 此指令指定的最大时间将覆盖其他指令指定的最大时间.
	 * 
	 * @param sMaxAge 应缓存响应的最长时间
	 * @param unit {@code sMaxAge}参数的时间单位
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl sMaxAge(long sMaxAge, TimeUnit unit) {
		this.sMaxAge = unit.toSeconds(sMaxAge);
		return this;
	}

	/**
	 * 添加"stale-while-revalidate"指令.
	 * <p>该指令指示缓存可以在它过期后的响应中提供, 直到指定的秒数.
	 * 如果缓存的响应由于存在此扩展而在过期后服务, 则缓存应该尝试重新验证它, 同时仍然提供过时的响应 (i.e. 没有阻塞).
	 * 
	 * @param staleWhileRevalidate 重新验证时应使用响应的最长时间
	 * @param unit {@code staleWhileRevalidate}参数的时间单位
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl staleWhileRevalidate(long staleWhileRevalidate, TimeUnit unit) {
		this.staleWhileRevalidate = unit.toSeconds(staleWhileRevalidate);
		return this;
	}

	/**
	 * 添加"stale-if-error"指令.
	 * <p>该指令指示当遇到错误时, 可以使用缓存的过期的响应来满足请求, 而不管其他新信息如何.
	 * 
	 * @param staleIfError 遇到错误时应使用响应的最长时间
	 * @param unit {@code staleIfError}参数的时间单位
	 * 
	 * @return {@code this}, 方便链接
	 */
	public CacheControl staleIfError(long staleIfError, TimeUnit unit) {
		this.staleIfError = unit.toSeconds(staleIfError);
		return this;
	}


	/**
	 * 返回"Cache-Control" header值.
	 * 
	 * @return {@code null} 如果没有添加指令, 或者header值
	 */
	public String getHeaderValue() {
		StringBuilder ccValue = new StringBuilder();
		if (this.maxAge != -1) {
			appendDirective(ccValue, "max-age=" + Long.toString(this.maxAge));
		}
		if (this.noCache) {
			appendDirective(ccValue, "no-cache");
		}
		if (this.noStore) {
			appendDirective(ccValue, "no-store");
		}
		if (this.mustRevalidate) {
			appendDirective(ccValue, "must-revalidate");
		}
		if (this.noTransform) {
			appendDirective(ccValue, "no-transform");
		}
		if (this.cachePublic) {
			appendDirective(ccValue, "public");
		}
		if (this.cachePrivate) {
			appendDirective(ccValue, "private");
		}
		if (this.proxyRevalidate) {
			appendDirective(ccValue, "proxy-revalidate");
		}
		if (this.sMaxAge != -1) {
			appendDirective(ccValue, "s-maxage=" + Long.toString(this.sMaxAge));
		}
		if (this.staleIfError != -1) {
			appendDirective(ccValue, "stale-if-error=" + Long.toString(this.staleIfError));
		}
		if (this.staleWhileRevalidate != -1) {
			appendDirective(ccValue, "stale-while-revalidate=" + Long.toString(this.staleWhileRevalidate));
		}

		String ccHeaderValue = ccValue.toString();
		return (StringUtils.hasText(ccHeaderValue) ? ccHeaderValue : null);
	}

	private void appendDirective(StringBuilder builder, String value) {
		if (builder.length() > 0) {
			builder.append(", ");
		}
		builder.append(value);
	}

}
