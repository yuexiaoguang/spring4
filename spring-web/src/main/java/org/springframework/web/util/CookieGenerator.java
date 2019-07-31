package org.springframework.web.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * 用于生成cookie的Helper类, 将cookie描述符设置作为bean属性, 并能够在给定响应中添加和删除cookie.
 *
 * <p>可以作为生成特定cookie的组件的基类, 例如 CookieLocaleResolver 和 CookieThemeResolver.
 */
public class CookieGenerator {

	/**
	 * Cookie可见的默认路径: "/", i.e. 整个服务器.
	 */
	public static final String DEFAULT_COOKIE_PATH = "/";


	protected final Log logger = LogFactory.getLog(getClass());

	private String cookieName;

	private String cookieDomain;

	private String cookiePath = DEFAULT_COOKIE_PATH;

	private Integer cookieMaxAge;

	private boolean cookieSecure = false;

	private boolean cookieHttpOnly = false;


	/**
	 * 使用此生成器创建的cookie的给定名称.
	 */
	public void setCookieName(String cookieName) {
		this.cookieName = cookieName;
	}

	/**
	 * 返回此生成器创建的cookie的给定名称.
	 */
	public String getCookieName() {
		return this.cookieName;
	}

	/**
	 * 将给定域用于此生成器创建的cookie.
	 * 该cookie仅对此域中的服务器可见.
	 */
	public void setCookieDomain(String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	/**
	 * 返回此生成器创建的cookie的域.
	 */
	public String getCookieDomain() {
		return this.cookieDomain;
	}

	/**
	 * 使用此生成器创建的cookie的给定路径.
	 * 该Cookie仅对此路径及以下的URL可见.
	 */
	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}

	/**
	 * 返回此生成器创建的cookie的路径.
	 */
	public String getCookiePath() {
		return this.cookiePath;
	}

	/**
	 * 使用此生成器创建的cookie的给定最大有效期 (以秒为单位).
	 * 有用的特殊值: -1 ... 不持久化, 在客户端关闭时删除.
	 * <p>默认情况下, 使用Servlet容器的默认值, 根本没有特定的最大有效期.
	 */
	public void setCookieMaxAge(Integer cookieMaxAge) {
		this.cookieMaxAge = cookieMaxAge;
	}

	/**
	 * 返回此生成器创建的cookie的最大有效期.
	 */
	public Integer getCookieMaxAge() {
		return this.cookieMaxAge;
	}

	/**
	 * 设置是否应仅使用安全协议发送cookie, 例如HTTPS (SSL).
	 * 这是对接收浏览器的指示, 不是由HTTP服务器本身处理的.
	 * <p>默认 "false".
	 */
	public void setCookieSecure(boolean cookieSecure) {
		this.cookieSecure = cookieSecure;
	}

	/**
	 * 返回是否应仅使用安全协议, 例如 HTTPS (SSL)发送cookie.
	 */
	public boolean isCookieSecure() {
		return this.cookieSecure;
	}

	/**
	 * 设置是否应该使用"HttpOnly"属性标记cookie.
	 * <p>默认 "false".
	 */
	public void setCookieHttpOnly(boolean cookieHttpOnly) {
		this.cookieHttpOnly = cookieHttpOnly;
	}

	/**
	 * 返回cookie是否应标记为 "HttpOnly"属性.
	 */
	public boolean isCookieHttpOnly() {
		return this.cookieHttpOnly;
	}


	/**
	 * 用此生成器的cookie描述符设置, 将具有给定值的cookie添加到响应中.
	 * <p>委托给{@link #createCookie}进行cookie创建.
	 * 
	 * @param response 用于添加cookie的HTTP响应
	 * @param cookieValue 要添加的cookie的值
	 */
	public void addCookie(HttpServletResponse response, String cookieValue) {
		Assert.notNull(response, "HttpServletResponse must not be null");
		Cookie cookie = createCookie(cookieValue);
		Integer maxAge = getCookieMaxAge();
		if (maxAge != null) {
			cookie.setMaxAge(maxAge);
		}
		if (isCookieSecure()) {
			cookie.setSecure(true);
		}
		if (isCookieHttpOnly()) {
			cookie.setHttpOnly(true);
		}
		response.addCookie(cookie);
		if (logger.isDebugEnabled()) {
			logger.debug("Added cookie with name [" + getCookieName() + "] and value [" + cookieValue + "]");
		}
	}

	/**
	 * 从响应中删除此生成器描述的cookie.
	 * 将生成一个空值和最大有效期为0的cookie.
	 * <p>委托给{@link #createCookie}创建 cookie.
	 * 
	 * @param response 从中删除cookie的HTTP响应
	 */
	public void removeCookie(HttpServletResponse response) {
		Assert.notNull(response, "HttpServletResponse must not be null");
		Cookie cookie = createCookie("");
		cookie.setMaxAge(0);
		if (isCookieSecure()) {
			cookie.setSecure(true);
		}
		if (isCookieHttpOnly()) {
			cookie.setHttpOnly(true);
		}
		response.addCookie(cookie);
		if (logger.isDebugEnabled()) {
			logger.debug("Removed cookie with name [" + getCookieName() + "]");
		}
	}

	/**
	 * 使用此生成器的cookie描述符设置创建具有给定值的cookie (除了"cookieMaxAge").
	 * 
	 * @param cookieValue 要创建的cookie的值
	 * 
	 * @return cookie
	 */
	protected Cookie createCookie(String cookieValue) {
		Cookie cookie = new Cookie(getCookieName(), cookieValue);
		if (getCookieDomain() != null) {
			cookie.setDomain(getCookieDomain());
		}
		cookie.setPath(getCookiePath());
		return cookie;
	}

}
