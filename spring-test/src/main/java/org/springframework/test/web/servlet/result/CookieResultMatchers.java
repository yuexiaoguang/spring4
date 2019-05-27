package org.springframework.test.web.servlet.result;

import javax.servlet.http.Cookie;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * Factory for response cookie assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#cookie}.
 */
public class CookieResultMatchers {

	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#cookie()}.
	 */
	protected CookieResultMatchers() {
	}


	/**
	 * Assert a cookie value with the given Hamcrest {@link Matcher}.
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
	 * Assert a cookie value.
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
	 * Assert a cookie exists. The existence check is irrespective of whether
	 * max age is 0 (i.e. expired).
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
	 * Assert a cookie does not exist. Note that the existence check is
	 * irrespective of whether max age is 0, i.e. expired.
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
	 * Assert a cookie's maxAge with a Hamcrest {@link Matcher}.
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
	 * Assert a cookie's maxAge value.
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
	 * Assert a cookie path with a Hamcrest {@link Matcher}.
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
	 * Assert a cookie's domain with a Hamcrest {@link Matcher}.
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
	 * Assert a cookie's domain value.
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
	 * Assert a cookie's comment with a Hamcrest {@link Matcher}.
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
	 * Assert a cookie's comment value.
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
	 * Assert a cookie's version with a Hamcrest {@link Matcher}
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
	 * Assert a cookie's version value.
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
	 * Assert whether the cookie must be sent over a secure protocol or not.
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
	 * Assert whether the cookie must be HTTP only.
	 * @since 4.3.9
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