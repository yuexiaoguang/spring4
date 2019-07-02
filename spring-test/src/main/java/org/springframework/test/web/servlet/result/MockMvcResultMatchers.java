package org.springframework.test.web.servlet.result;

import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.AntPathMatcher;

import static org.springframework.test.util.AssertionErrors.*;

/**
 * 基于{@link ResultMatcher}的结果操作的静态工厂方法.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite.
 * To navigate to this setting, open the Preferences and type "favorites".
 */
public abstract class MockMvcResultMatchers {

	private static final AntPathMatcher pathMatcher = new AntPathMatcher();


	/**
	 * 访问与请求相关的断言.
	 */
	public static RequestResultMatchers request() {
		return new RequestResultMatchers();
	}

	/**
	 * 访问处理请求的处理器的断言.
	 */
	public static HandlerResultMatchers handler() {
		return new HandlerResultMatchers();
	}

	/**
	 * 访问与模型相关的断言.
	 */
	public static ModelResultMatchers model() {
		return new ModelResultMatchers();
	}

	/**
	 * 访问所选视图上的断言.
	 */
	public static ViewResultMatchers view() {
		return new ViewResultMatchers();
	}

	/**
	 * 访问flash属性断言.
	 */
	public static FlashAttributeResultMatchers flash() {
		return new FlashAttributeResultMatchers();
	}

	/**
	 * 断言请求已转发到给定的URL.
	 * <p>此方法仅接受精确匹配.
	 * 
	 * @param expectedUrl 预期的精确URL
	 */
	public static ResultMatcher forwardedUrl(final String expectedUrl) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertEquals("Forwarded URL", expectedUrl, result.getResponse().getForwardedUrl());
			}
		};
	}

	/**
	 * 断言请求已转发到给定的URL.
	 * <p>此方法接受{@link org.springframework.util.AntPathMatcher}表达式.
	 * 
	 * @param urlPattern 要匹配的AntPath表达式
	 */
	public static ResultMatcher forwardedUrlPattern(final String urlPattern) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertTrue("AntPath expression", pathMatcher.isPattern(urlPattern));
				assertTrue("Forwarded URL does not match the expected URL pattern",
						pathMatcher.match(urlPattern, result.getResponse().getForwardedUrl()));
			}
		};
	}

	/**
	 * 断言请求被重定向到给定的URL.
	 * <p>此方法仅接受精确匹配.
	 * 
	 * @param expectedUrl 预期的精确URL
	 */
	public static ResultMatcher redirectedUrl(final String expectedUrl) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertEquals("Redirected URL", expectedUrl, result.getResponse().getRedirectedUrl());
			}
		};
	}

	/**
	 * 断言请求被重定向到给定的URL.
	 * <p>此方法接受{@link org.springframework.util.AntPathMatcher}表达式
	 * 
	 * @param expectedUrl 要匹配的AntPath表达式
	 */
	public static ResultMatcher redirectedUrlPattern(final String expectedUrl) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertTrue("AntPath expression",pathMatcher.isPattern(expectedUrl));
				assertTrue("Redirected URL",
						pathMatcher.match(expectedUrl, result.getResponse().getRedirectedUrl()));
			}
		};
	}

	/**
	 * 访问响应状态断言.
	 */
	public static StatusResultMatchers status() {
		return new StatusResultMatchers();
	}

	/**
	 * 访问响应header断言.
	 */
	public static HeaderResultMatchers header() {
		return new HeaderResultMatchers();
	}

	/**
	 * 访问响应正文断言.
	 */
	public static ContentResultMatchers content() {
		return new ContentResultMatchers();
	}

	/**
	 * 使用<a href="https://github.com/jayway/JsonPath">JsonPath</a>表达式访问响应正文断言以检查正文的特定子集.
	 * <p>JSON路径表达式可以是使用{@link String#format(String, Object...)}中定义的格式说明符的参数化字符串.
	 * 
	 * @param expression JSON路径表达式, 可选择使用参数进行参数化
	 * @param args 用于参数化JSON路径表达式的参数
	 */
	public static JsonPathResultMatchers jsonPath(String expression, Object... args) {
		return new JsonPathResultMatchers(expression, args);
	}

	/**
	 * 使用<a href="https://github.com/jayway/JsonPath">JsonPath</a>表达式访问响应正文断言以检查正文的特定子集,
	 * 并使用Hamcrest匹配器断言在JSON路径中找到的值.
	 * 
	 * @param expression JSON路径表达式
	 * @param matcher JSON路径上预期值的匹配器
	 */
	public static <T> ResultMatcher jsonPath(String expression, Matcher<T> matcher) {
		return new JsonPathResultMatchers(expression).value(matcher);
	}

	/**
	 * 使用XPath表达式访问响应正文断言以检查正文的特定子集.
	 * <p>XPath表达式可以是使用{@link String#format(String, Object...)}中定义的格式说明符的参数化字符串.
	 * 
	 * @param expression XPath表达式, 可选择使用参数进行参数化
	 * @param args 用于参数化XPath表达式的参数
	 */
	public static XpathResultMatchers xpath(String expression, Object... args) throws XPathExpressionException {
		return new XpathResultMatchers(expression, null, args);
	}

	/**
	 * 使用XPath表达式访问响应正文断言以检查正文的特定子集.
	 * <p>XPath表达式可以是使用{@link String#format(String, Object...)}中定义的格式说明符的参数化字符串.
	 * 
	 * @param expression XPath表达式, 可选择使用参数进行参数化
	 * @param namespaces XPath表达式中引用的命名空间
	 * @param args 用于参数化XPath表达式的参数
	 */
	public static XpathResultMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathResultMatchers(expression, namespaces, args);
	}

	/**
	 * 访问响应cookie断言.
	 */
	public static CookieResultMatchers cookie() {
		return new CookieResultMatchers();
	}

}
