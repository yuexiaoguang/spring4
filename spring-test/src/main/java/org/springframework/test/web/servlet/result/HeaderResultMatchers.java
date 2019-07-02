package org.springframework.test.web.servlet.result;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hamcrest.Matcher;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 响应header断言的工厂.
 *
 * <p>可以通过{@link MockMvcResultMatchers#header}获得此类的实例.
 */
public class HeaderResultMatchers {

	/**
	 * See {@link MockMvcResultMatchers#header()}.
	 */
	protected HeaderResultMatchers() {
	}


	/**
	 * 使用给定的Hamcrest字符串{@code Matcher}断言响应header的主要值.
	 */
	public ResultMatcher string(final String name, final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertThat("Response header '" + name + "'", result.getResponse().getHeader(name), matcher);
			}
		};
	}

	/**
	 * 使用给定的Hamcrest Iterable {@link Matcher}断言响应header的值.
	 */
	public <T> ResultMatcher stringValues(final String name, final Matcher<Iterable<String>> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				List<String> values = result.getResponse().getHeaders(name);
				assertThat("Response header '" + name + "'", values, matcher);
			}
		};
	}

	/**
	 * 断言响应头的主值为String值.
	 */
	public ResultMatcher string(final String name, final String value) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertEquals("Response header '" + name + "'", value, result.getResponse().getHeader(name));
			}
		};
	}

	/**
	 * 断言响应header的值为String值.
	 */
	public ResultMatcher stringValues(final String name, final String... values) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				List<Object> actual = result.getResponse().getHeaderValues(name);
				assertEquals("Response header '" + name + "'", Arrays.asList(values), actual);
			}
		};
	}

	/**
	 * 断言命名的响应header不存在.
	 */
	public ResultMatcher doesNotExist(final String name) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertTrue("Response should not contain header '" + name + "'",
						!result.getResponse().containsHeader(name));
			}
		};
	}

	/**
	 * 断言命名响应header的主要值为{@code long}.
	 * <p>如果响应不包含指定的header, 或者提供的{@code value}与主值不匹配,
	 * 则此方法返回的{@link ResultMatcher}会抛出{@link AssertionError}.
	 */
	public ResultMatcher longValue(final String name, final long value) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				MockHttpServletResponse response = result.getResponse();
				assertTrue("Response does not contain header '" + name + "'", response.containsHeader(name));
				assertEquals("Response header '" + name + "'", value, Long.parseLong(response.getHeader(name)));
			}
		};
	}

	/**
	 * 使用RFC 7231中描述的首选日期格式, 断言命名响应header的主值为日期字符串.
	 * <p>如果响应不包含指定的header, 或者提供的{@code value}与主值不匹配,
	 * 则此方法返回的{@link ResultMatcher}会抛出{@link AssertionError}.
	 */
	public ResultMatcher dateValue(final String name, final long value) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				String formatted = format.format(new Date(value));
				MockHttpServletResponse response = result.getResponse();
				assertTrue("Response does not contain header '" + name + "'", response.containsHeader(name));
				assertEquals("Response header '" + name + "'", formatted, response.getHeader(name));
			}
		};
	}

}
