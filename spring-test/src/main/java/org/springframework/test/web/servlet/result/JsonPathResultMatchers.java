package org.springframework.test.web.servlet.result;

import java.io.UnsupportedEncodingException;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringStartsWith;

import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.StringUtils;

/**
 * 使用<a href="https://github.com/jayway/JsonPath">JsonPath</a>表达式对响应内容进行断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#jsonPath(String, Matcher)}
 * 或{@link MockMvcResultMatchers#jsonPath(String, Object...)}访问此类的实例.
 */
public class JsonPathResultMatchers {

	private final JsonPathExpectationsHelper jsonPathHelper;

	private String prefix;


	/**
	 * <p>使用{@link MockMvcResultMatchers#jsonPath(String, Object...)}
	 * 或{@link MockMvcResultMatchers#jsonPath(String, Matcher)}.
	 * 
	 * @param expression {@link JsonPath}表达式; 不能是{@code null}或空
	 * @param args 参数化{@code JsonPath}表达式的参数, 使用{@link String#format(String, Object...)}中定义的格式说明符
	 */
	protected JsonPathResultMatchers(String expression, Object... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}

	/**
	 * 配置当前{@code JsonPathResultMatchers}实例以验证JSON有效内容是否带有给定前缀.
	 * <p>如果JSON有效内容带有前缀以避免跨站点脚本包含 (XSSI)攻击, 请使用此方法.
	 * 
	 * @param prefix 实际JSON有效负载的字符串前缀
	 */
	public JsonPathResultMatchers prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}


	/**
	 * 根据响应内容评估JSON路径表达式, 并使用给定的Hamcrest {@link Matcher}声明结果值.
	 */
	public <T> ResultMatcher value(final Matcher<T> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValue(content, matcher);
			}
		};
	}

	/**
	 * {@link #value(Matcher)}的重载变体, 它还接受匹配器可以可靠地工作的结果值的目标类型.
	 * <p>这对于可靠地匹配数字非常有用 &mdash; 例如, 将整数强制转换为 double.
	 */
	public <T> ResultMatcher value(final Matcher<T> matcher, final Class<T> targetType) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValue(content, matcher, targetType);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言结果等于提供的值.
	 */
	public ResultMatcher value(final Object expectedValue) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				jsonPathHelper.assertValue(getContent(result), expectedValue);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言给定路径上存在非空值.
	 * <p>如果JSON路径表达式不{@linkplain JsonPath#isDefinite 确定}, 则此方法断言给定路径上的值不<em>为空</em>.
	 */
	public ResultMatcher exists() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.exists(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并声明给定路径中不存在值.
	 * <p>如果JSON路径表达式不{@linkplain JsonPath#isDefinite 确定}, 则此方法断言给定路径上的值<em>为空</em>.
	 */
	public ResultMatcher doesNotExist() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.doesNotExist(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言给定路径中存在空值.
	 * <p>有关<em>empty</em>的语义, 请参阅{@link org.springframework.util.ObjectUtils#isEmpty(Object)}的Javadoc.
	 */
	public ResultMatcher isEmpty() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValueIsEmpty(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言给定路径中存在非空值.
	 * <p>有关<em>empty</em>的语义, 请参阅{@link org.springframework.util.ObjectUtils#isEmpty(Object)}的Javadoc.
	 */
	public ResultMatcher isNotEmpty() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValueIsNotEmpty(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言结果为{@link String}.
	 */
	public ResultMatcher isString() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValueIsString(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言结果为{@link Boolean}.
	 */
	public ResultMatcher isBoolean() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValueIsBoolean(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言结果为{@link Number}.
	 */
	public ResultMatcher isNumber() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValueIsNumber(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言结果为数组.
	 */
	public ResultMatcher isArray() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValueIsArray(content);
			}
		};
	}

	/**
	 * 根据响应内容评估JSON路径表达式, 并断言结果为{@link java.util.Map}.
	 */
	public ResultMatcher isMap() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = getContent(result);
				jsonPathHelper.assertValueIsMap(content);
			}
		};
	}

	private String getContent(MvcResult result) throws UnsupportedEncodingException {
		String content = result.getResponse().getContentAsString();
		if (StringUtils.hasLength(this.prefix)) {
			try {
				String reason = String.format("Expected a JSON payload prefixed with \"%s\" but found: %s",
						this.prefix, StringUtils.quote(content.substring(0, this.prefix.length())));
				MatcherAssert.assertThat(reason, content, StringStartsWith.startsWith(this.prefix));
				return content.substring(this.prefix.length());
			}
			catch (StringIndexOutOfBoundsException ex) {
				throw new AssertionError("JSON prefix \"" + this.prefix + "\" not found: " + ex);
			}
		}
		else {
			return content;
		}
	}
}
