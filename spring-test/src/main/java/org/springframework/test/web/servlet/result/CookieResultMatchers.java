package org.springframework.test.web.servlet.result;

import javax.servlet.http.Cookie;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 用于响应cookie断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#cookie}访问此类的实例.
 */
public class CookieResultMatchers {

	/**
	 * Use {@link MockMvcResultMatchers#cookie()}.
	 */
	protected CookieResultMatchers() {
	}


	/**
	 * 使用给定的Hamcrest {@link Matcher}断言cookie值.
	 */
	public ResultMatcher value(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				Cookie cookie = getCookie(result, name);
				assertThat("Response cookie '" + name + "'", cookie.getValue(), matcher);
			}
		};
	}

	/**
	 * 断言cookie值.
	 */
	public ResultMatcher value(final String name, final String expectedValue) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie", expectedValue, cookie.getValue());
			}
		};
	}

	/**
	 * 断言存在cookie.
	 * 存在性检查与最大年龄是否为0无关 (i.e. 过期).
	 */
	public ResultMatcher exists(final String name) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				getCookie(result, name);
			}
		};
	}

	/**
	 * 断言cookie不存在.
	 * 注意, 存在性检查与最大年龄是否为0无关, 即是否过期.
	 */
	public ResultMatcher doesNotExist(final String name) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				Cookie cookie = result.getResponse().getCookie(name);
				assertTrue("Unexpected cookie with name '" + name + "'", cookie == null);
			}
		};
	}

	/**
	 * 使用Hamcrest {@link Matcher}断言cookie的maxAge.
	 */
	public ResultMatcher maxAge(final String name, final Matcher<? super Integer> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				Cookie cookie = getCookie(result, name);
				assertThat("Response cookie '" + name + "' maxAge", cookie.getMaxAge(), matcher);
			}
		};
	}

	/**
	 * 断言cookie的maxAge值.
	 */
	public ResultMatcher maxAge(final String name, final int maxAge) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie '" + name + "' maxAge", maxAge, cookie.getMaxAge());
			}
		};
	}

	/**
	 * 使用Hamcrest {@link Matcher}断言cookie路径.
	 */
	public ResultMatcher path(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertThat("Response cookie '" + name + "' path", cookie.getPath(), matcher);
			}
		};
	}

	public ResultMatcher path(final String name, final String path) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie '" + name + "' path", path, cookie.getPath());
			}
		};
	}

	/**
	 * 使用Hamcrest {@link Matcher}断言cookie的域名.
	 */
	public ResultMatcher domain(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertThat("Response cookie '" + name + "' domain", cookie.getDomain(), matcher);
			}
		};
	}

	/**
	 * 断言cookie的域值.
	 */
	public ResultMatcher domain(final String name, final String domain) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie '" + name + "' domain", domain, cookie.getDomain());
			}
		};
	}

	/**
	 * 使用Hamcrest {@link Matcher}断言cookie的注释.
	 */
	public ResultMatcher comment(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertThat("Response cookie '" + name + "' comment", cookie.getComment(), matcher);
			}
		};
	}

	/**
	 * 断言cookie的注释值.
	 */
	public ResultMatcher comment(final String name, final String comment) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie '" + name + "' comment", comment, cookie.getComment());
			}
		};
	}

	/**
	 * 使用Hamcrest {@link Matcher}断言cookie的版本
	 */
	public ResultMatcher version(final String name, final Matcher<? super Integer> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertThat("Response cookie '" + name + "' version", cookie.getVersion(), matcher);
			}
		};
	}

	/**
	 * 断言cookie的版本值.
	 */
	public ResultMatcher version(final String name, final int version) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie '" + name + "' version", version, cookie.getVersion());
			}
		};
	}

	/**
	 * 断言cookie是否必须通过安全协议发送.
	 */
	public ResultMatcher secure(final String name, final boolean secure) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie '" + name + "' secure", secure, cookie.getSecure());
			}
		};
	}

	/**
	 * 断言cookie是否必须仅为HTTP.
	 */
	public ResultMatcher httpOnly(final String name, final boolean httpOnly) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Cookie cookie = getCookie(result, name);
				assertEquals("Response cookie '" + name + "' httpOnly", httpOnly, cookie.isHttpOnly());
			}
		};
	}


	private static Cookie getCookie(MvcResult result, String name) {
		Cookie cookie = result.getResponse().getCookie(name);
		assertTrue("No cookie with name '" + name + "'", cookie != null);
		return cookie;
	}

}
