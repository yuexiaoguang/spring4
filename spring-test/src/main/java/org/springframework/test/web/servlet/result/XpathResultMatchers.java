package org.springframework.test.web.servlet.result;

import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * 使用XPath表达式对响应内容进行断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#xpath}访问此类的实例.
 */
public class XpathResultMatchers {

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * 使用{@link MockMvcResultMatchers#xpath(String, Object...)}
	 * 或{@link MockMvcResultMatchers#xpath(String, Map, Object...)}.
	 * 
	 * @param expression XPath表达式
	 * @param namespaces XPath表达式中引用的XML名称空间, 或{@code null}
	 * @param args 用于{@link String#format(String, Object...)}中定义的格式说明符参数化XPath表达式的参数
	 */
	protected XpathResultMatchers(String expression, Map<String, String> namespaces, Object ... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}


	/**
	 * 评估XPath并断言使用给定的Hamcrest {@link Matcher}找到的{@link Node}内容.
	 */
	public ResultMatcher node(final Matcher<? super Node> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertNode(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
			}
		};
	}

	/**
	 * 如果在响应中明确定义, 则获取响应编码, 否则为{code null}.
	 */
	private String getDefinedEncoding(MockHttpServletResponse response) {
		return response.isCharset() ? response.getCharacterEncoding() : null;
	}

	/**
	 * 评估XPath并断言内容存在.
	 */
	public ResultMatcher exists() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.exists(response.getContentAsByteArray(), getDefinedEncoding(response));
			}
		};
	}

	/**
	 * 评估XPath并断言内容不存在.
	 */
	public ResultMatcher doesNotExist() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.doesNotExist(response.getContentAsByteArray(), getDefinedEncoding(response));
			}
		};
	}

	/**
	 * 评估XPath并断言使用给定Hamcrest {@link Matcher}找到的节点数.
	 */
	public ResultMatcher nodeCount(final Matcher<Integer> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
			}
		};
	}

	/**
	 * 评估XPath并断言找到的节点数.
	 */
	public ResultMatcher nodeCount(final int expectedCount) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), expectedCount);
			}
		};
	}

	/**
	 * 应用XPath并断言使用给定的Hamcrest {@link Matcher}找到的{@link String}值.
	 */
	public ResultMatcher string(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
			}
		};
	}

	/**
	 * 应用XPath并断言找到的{@link String}值.
	 */
	public ResultMatcher string(final String expectedValue) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
			}
		};
	}

	/**
	 * 评估XPath并断言使用给定的Hamcrest {@link Matcher}找到的{@link Double}值.
	 */
	public ResultMatcher number(final Matcher<? super Double> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
			}
		};
	}

	/**
	 * 评估XPath并断言找到的{@link Double}值.
	 */
	public ResultMatcher number(final Double expectedValue) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
			}
		};
	}

	/**
	 * 评估XPath并断言找到的{@link Boolean}值.
	 */
	public ResultMatcher booleanValue(final Boolean value) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				MockHttpServletResponse response = result.getResponse();
				xpathHelper.assertBoolean(response.getContentAsByteArray(), getDefinedEncoding(response), value);
			}
		};
	}

}
