package org.springframework.test.web.client.match;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * {@link RequestMatcher}类的静态工厂方法.
 * 通常用于为 {@link MockRestServiceServer#expect(RequestMatcher)}提供输入.
 *
 * <h3>Eclipse Users</h3>
 * <p>考虑将此类添加为Java编辑器的最爱. 要导航到此设置, 请打开“首选项”并键入“favorites”.
 */
public abstract class MockRestRequestMatchers {

	/**
	 * 匹配任何请求.
	 */
	public static RequestMatcher anything() {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws AssertionError {
			}
		};
	}

	/**
	 * 断言请求的{@link HttpMethod}.
	 * 
	 * @param method HTTP方法
	 * 
	 * @return 请求匹配器
	 */
	public static RequestMatcher method(final HttpMethod method) {
		Assert.notNull(method, "'method' must not be null");
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws AssertionError {
				AssertionErrors.assertEquals("Unexpected HttpMethod", method, request.getMethod());
			}
		};
	}

	/**
	 * 使用给定的匹配器断言请求URI字符串.
	 * 
	 * @param matcher 预期URI的字符串匹配器
	 * 
	 * @return 请求匹配器
	 */
	public static RequestMatcher requestTo(final Matcher<String> matcher) {
		Assert.notNull(matcher, "'matcher' must not be null");
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				assertThat("Request URI", request.getURI().toString(), matcher);
			}
		};
	}

	/**
	 * 断言请求URI字符串.
	 * 
	 * @param expectedUri 预期URI
	 * 
	 * @return 请求匹配器
	 */
	public static RequestMatcher requestTo(final String expectedUri) {
		Assert.notNull(expectedUri, "'uri' must not be null");
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				assertEquals("Request URI", expectedUri, request.getURI().toString());
			}
		};
	}

	/**
	 * 期望对给定URI的请求.
	 * 
	 * @param uri 预期URI
	 * 
	 * @return 请求匹配器
	 */
	public static RequestMatcher requestTo(final URI uri) {
		Assert.notNull(uri, "'uri' must not be null");
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				AssertionErrors.assertEquals("Unexpected request", uri, request.getURI());
			}
		};
	}

	/**
	 * 使用给定的Hamcrest匹配器断言请求查询参数值.
	 */
	@SafeVarargs
	public static RequestMatcher queryParam(final String name, final Matcher<? super String>... matchers) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) {
				MultiValueMap<String, String> params = getQueryParams(request);
				assertValueCount("query param", name, params, matchers.length);
				for (int i = 0 ; i < matchers.length; i++) {
					assertThat("Query param", params.get(name).get(i), matchers[i]);
				}
			}
		};
	}

	/**
	 * 断言请求查询参数值.
	 */
	public static RequestMatcher queryParam(final String name, final String... expectedValues) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) {
				MultiValueMap<String, String> params = getQueryParams(request);
				assertValueCount("query param", name, params, expectedValues.length);
				for (int i = 0 ; i < expectedValues.length; i++) {
					assertEquals("Query param + [" + name + "]", expectedValues[i], params.get(name).get(i));
				}
			}
		};
	}

	private static MultiValueMap<String, String> getQueryParams(ClientHttpRequest request) {
		return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
	}

	private static void assertValueCount(String valueType, final String name,
			MultiValueMap<String, String> map, int count) {

		List<String> values = map.get(name);

		String message = "Expected " + valueType + " <" + name + ">";
		assertTrue(message + " to exist but was null", values != null);

		assertTrue(message + " to have at least <" + count + "> values but found " + values,
				count <= values.size());
	}

	/**
	 * 使用给定的Hamcrest匹配器断言请求header值.
	 */
	@SafeVarargs
	public static RequestMatcher header(final String name, final Matcher<? super String>... matchers) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) {
				assertValueCount("header", name, request.getHeaders(), matchers.length);
				for (int i = 0 ; i < matchers.length; i++) {
					assertThat("Request header", request.getHeaders().get(name).get(i), matchers[i]);
				}
			}
		};
	}

	/**
	 * 断言请求header值.
	 */
	public static RequestMatcher header(final String name, final String... expectedValues) {
		return new RequestMatcher() {
			@Override
			public void match(ClientHttpRequest request) {
				assertValueCount("header", name, request.getHeaders(), expectedValues.length);
				for (int i = 0 ; i < expectedValues.length; i++) {
					assertEquals("Request header + [" + name + "]",
							expectedValues[i], request.getHeaders().get(name).get(i));
				}
			}
		};
	}

	/**
	 * 访问请求正文匹配器.
	 */
	public static ContentRequestMatchers content() {
		return new ContentRequestMatchers();
	}

	/**
	 * 使用<a href="https://github.com/jayway/JsonPath">JsonPath</a>表达式访问请求正文匹配器, 以检查正文的特定子集.
	 * JSON路径表达式可以是使用{@link String#format(String, Object...)}中定义的格式说明符的参数化字符串.
	 * 
	 * @param expression 可选地使用参数进行参数化的JSON路径
	 * @param args 参数化JSON路径表达式的参数
	 */
	public static JsonPathRequestMatchers jsonPath(String expression, Object... args) {
		return new JsonPathRequestMatchers(expression, args);
	}

	/**
	 * 使用<a href="https://github.com/jayway/JsonPath">JsonPath</a>表达式
	 * 访问请求正文匹配器以检查正文的特定子集, 并使用Hamcrest匹配来断言在JSON路径中找到的值.
	 * 
	 * @param expression JSON路径表达式
	 * @param matcher JSON路径上预期值的匹配器
	 */
	public static <T> RequestMatcher jsonPath(String expression, Matcher<T> matcher) {
		return new JsonPathRequestMatchers(expression).value(matcher);
	}

	/**
	 * 使用XPath访问请求正文匹配器以检查正文的特定子集.
	 * XPath表达式可以是使用{@link String#format(String, Object...)}中定义的格式说明符的参数化字符串.
	 * 
	 * @param expression 可选的参数化参数的XPath
	 * @param args 用于参数化XPath表达式的参数
	 */
	public static XpathRequestMatchers xpath(String expression, Object... args) throws XPathExpressionException {
		return new XpathRequestMatchers(expression, null, args);
	}

	/**
	 * 使用XPath访问响应正文匹配器以检查正文的特定子集.
	 * XPath表达式可以是使用{@link String#format(String, Object...)}中定义的格式说明符的参数化字符串.
	 * 
	 * @param expression 可选的参数化参数的XPath
	 * @param namespaces XPath表达式中引用的命名空间
	 * @param args 用于参数化XPath表达式的参数
	 */
	public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathRequestMatchers(expression, namespaces, args);
	}

}
