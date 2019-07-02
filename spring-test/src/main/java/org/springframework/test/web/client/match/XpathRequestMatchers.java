package org.springframework.test.web.client.match;

import java.io.IOException;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;

/**
 * 请求内容{@code RequestMatcher}的工厂方法, 使用XPath表达式.
 * 通常通过{@code RequestMatchers.xpath(..)}访问此类的实例.
 */
public class XpathRequestMatchers {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * 类构造函数, 不用于直接实例化.
	 * 使用{@link MockRestRequestMatchers#xpath(String, Object...)}
	 * 或{@link MockRestRequestMatchers#xpath(String, Map, Object...)}.
	 * 
	 * @param expression XPath表达式
	 * @param namespaces XPath表达式中引用的XML名称空间, 或{@code null}
	 * @param args 使用{@link String#format(String, Object...)}中定义的格式说明符参数化XPath表达式的参数
	 * 
	 * @throws XPathExpressionException 如果表达式编译失败
	 */
	protected XpathRequestMatchers(String expression, Map<String, String> namespaces, Object ... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}


	/**
	 * 应用XPath并使用给定的{@code Matcher<Node>}断言它.
	 */
	public <T> RequestMatcher node(final Matcher<? super Node> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNode(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * 断言内容存在于给定的XPath中.
	 */
	public <T> RequestMatcher exists() {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.exists(request.getBodyAsBytes(), DEFAULT_ENCODING);
			}
		};
	}

	/**
	 * 断言在给定的XPath中不存在该内容.
	 */
	public <T> RequestMatcher doesNotExist() {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.doesNotExist(request.getBodyAsBytes(), DEFAULT_ENCODING);
			}
		};
	}

	/**
	 * 应用XPath并断言使用给定{@code Matcher<Integer>}找到的节点数.
	 */
	public <T> RequestMatcher nodeCount(final Matcher<Integer> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * 应用XPath并断言找到的节点数.
	 */
	public <T> RequestMatcher nodeCount(final int expectedCount) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, expectedCount);
			}
		};
	}

	/**
	 * 应用XPath并断言使用给定匹配器找到的String内容.
	 */
	public <T> RequestMatcher string(final Matcher<? super String> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * 应用XPath并断言找到的String内容.
	 */
	public RequestMatcher string(final String value) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
			}
		};
	}

	/**
	 * 应用XPath并断言使用给定匹配器找到的数字.
	 */
	public <T> RequestMatcher number(final Matcher<? super Double> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * 应用XPath并断言找到的节点数.
	 */
	public RequestMatcher number(final Double value) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
			}
		};
	}

	/**
	 * 应用XPath并断言找到的布尔值.
	 */
	public <T> RequestMatcher booleanValue(final Boolean value) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertBoolean(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
			}
		};
	}


	/**
	 * XPath {@link RequestMatcher}的抽象基类.
	 */
	private abstract static class AbstractXpathRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse XML request content: " + ex.getMessage());
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
	}

}
