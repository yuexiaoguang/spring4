package org.springframework.test.web.client.match;

import java.io.IOException;
import java.text.ParseException;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;

/**
 * 使用<a href="https://github.com/jayway/JsonPath">JsonPath</a>表达式断言请求内容的工厂.
 *
 * <p>通常通过
 * {@link MockRestRequestMatchers#jsonPath(String, Matcher)}
 * 或{@link MockRestRequestMatchers#jsonPath(String, Object...)}访问此类的实例.
 */
public class JsonPathRequestMatchers {

	private final JsonPathExpectationsHelper jsonPathHelper;


	/**
	 * <p>使用{@link MockRestRequestMatchers#jsonPath(String, Matcher)}
	 * 或{@link MockRestRequestMatchers#jsonPath(String, Object...)}.
	 * 
	 * @param expression {@link JsonPath}表达式; 从不{@code null}或空
	 * @param args 参数化{@code JsonPath}表达式的参数, 使用{@link String#format(String, Object...)}中定义的格式说明符
	 */
	protected JsonPathRequestMatchers(String expression, Object ... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}


	/**
	 * 根据请求内容评估JSON路径表达式, 并使用给定的Hamcrest {@link Matcher}断言结果值.
	 */
	public <T> RequestMatcher value(final Matcher<T> matcher) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * {@link #value(Matcher)}的重载变体, 它还接受匹配器可以可靠地工作的结果值的目标类型.
	 * <p>这对于可靠地匹配数字非常有用 &mdash; 例如, 将整数强制转换为 double.
	 */
	public <T> RequestMatcher value(final Matcher<T> matcher, final Class<T> targetType) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				String body = request.getBodyAsString();
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(body, matcher, targetType);
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言结果等于提供的值.
	 */
	public RequestMatcher value(final Object expectedValue) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), expectedValue);
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言给定路径中存在非空值.
	 * <p>如果JSON路径表达式不{@linkplain JsonPath#isDefinite 确定}, 则此方法断言给定路径上的值<em>不为空</em>.
	 */
	public RequestMatcher exists() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.exists(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言在给定路径中不存在值.
	 * <p>如果JSON路径表达式不{@linkplain JsonPath#isDefinite 确定}, 则此方法断言给定路径上的值<em>为空</em>.
	 */
	public RequestMatcher doesNotExist() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.doesNotExist(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言给定路径中存在空值.
	 * <p>有关<<em>empty</em>的语义, 请参阅{@link org.springframework.util.ObjectUtils#isEmpty(Object)}的Javadoc.
	 */
	public RequestMatcher isEmpty() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsEmpty(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言给定路径中存在非空值.
	 * <p>有关<<em>empty</em>的语义, 请参阅{@link org.springframework.util.ObjectUtils#isEmpty(Object)}的Javadoc.
	 */
	public RequestMatcher isNotEmpty() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNotEmpty(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言结果为{@link String}.
	 */
	public RequestMatcher isString() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsString(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言结果为{@link Boolean}.
	 */
	public RequestMatcher isBoolean() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsBoolean(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言结果为{@link Number}.
	 */
	public RequestMatcher isNumber() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNumber(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言结果是数组.
	 */
	public RequestMatcher isArray() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsArray(request.getBodyAsString());
			}
		};
	}

	/**
	 * 根据请求内容评估JSON路径表达式, 并断言结果为{@link java.util.Map}.
	 */
	public RequestMatcher isMap() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsMap(request.getBodyAsString());
			}
		};
	}


	/**
	 * 基于{@code JsonPath}的{@link RequestMatcher}的抽象基类.
	 */
	private abstract static class AbstractJsonPathRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (ParseException ex) {
				throw new AssertionError("Failed to parse JSON request content: " + ex.getMessage());
			}
		}

		abstract void matchInternal(MockClientHttpRequest request) throws IOException, ParseException;
	}
}
