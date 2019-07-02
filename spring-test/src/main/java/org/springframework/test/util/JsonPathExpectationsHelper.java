package org.springframework.test.util;

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 通过JSON路径表达式应用断言的辅助类.
 *
 * <p>基于<a href="https://github.com/jayway/JsonPath">JsonPath</a>项目: 要求版本0.9+, 强烈推荐1.1+.
 */
public class JsonPathExpectationsHelper {

	private final String expression;

	private final JsonPath jsonPath;


	/**
	 * @param expression {@link JsonPath}表达式; 不能是{@code null}或空
	 * @param args 参数化{@code JsonPath}表达式的参数, 使用{@link String#format(String, Object...)}中定义的格式说明符
	 */
	public JsonPathExpectationsHelper(String expression, Object... args) {
		Assert.hasText(expression, "expression must not be null or empty");
		this.expression = String.format(expression, args);
		this.jsonPath = JsonPath.compile(this.expression);
	}


	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并使用给定的{@code Matcher}断言结果值.
	 * 
	 * @param content JSON内容
	 * @param matcher 用于断言结果的匹配器
	 */
	@SuppressWarnings("unchecked")
	public <T> void assertValue(String content, Matcher<T> matcher) {
		T value = (T) evaluateJsonPath(content);
		assertThat("JSON path \"" + this.expression + "\"", value, matcher);
	}

	/**
	 * {@link #assertValue(String, Matcher)}的重载变体, 它也接受结果值的目标类型.
	 * 这对于可靠地匹配数字(例如将整数强制转换为double) 非常有用.
	 * 
	 * @param content JSON内容
	 * @param matcher 用于断言结果的匹配器
	 * @param targetType 结果值的预期类型
	 */
	@SuppressWarnings("unchecked")
	public <T> void assertValue(String content, Matcher<T> matcher, Class<T> targetType) {
		T value = (T) evaluateJsonPath(content, targetType);
		assertThat("JSON path \"" + this.expression + "\"", value, matcher);
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言结果等于期望值.
	 * 
	 * @param content JSON内容
	 * @param expectedValue 期望值
	 */
	public void assertValue(String content, Object expectedValue) {
		Object actualValue = evaluateJsonPath(content);
		if ((actualValue instanceof List) && !(expectedValue instanceof List)) {
			@SuppressWarnings("rawtypes")
			List actualValueList = (List) actualValue;
			if (actualValueList.isEmpty()) {
				fail("No matching value at JSON path \"" + this.expression + "\"");
			}
			if (actualValueList.size() != 1) {
				fail("Got a list of values " + actualValue + " instead of the expected single value " + expectedValue);
			}
			actualValue = actualValueList.get(0);
		}
		else if (actualValue != null && expectedValue != null) {
			if (!actualValue.getClass().equals(expectedValue.getClass())) {
				actualValue = evaluateJsonPath(content, expectedValue.getClass());
			}
		}
		assertEquals("JSON path \"" + this.expression + "\"", expectedValue, actualValue);
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言结果值为{@link String}.
	 * 
	 * @param content JSON内容
	 */
	public void assertValueIsString(String content) {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a string", value), value, instanceOf(String.class));
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言结果值为{@link Boolean}.
	 * 
	 * @param content JSON内容
	 */
	public void assertValueIsBoolean(String content) {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a boolean", value), value, instanceOf(Boolean.class));
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言结果值为{@link Number}.
	 * 
	 * @param content JSON内容
	 */
	public void assertValueIsNumber(String content) {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a number", value), value, instanceOf(Number.class));
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言结果值为数组.
	 * 
	 * @param content JSON内容
	 */
	public void assertValueIsArray(String content) {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("an array", value), value, instanceOf(List.class));
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言结果值为{@link Map}.
	 * 
	 * @param content JSON内容
	 */
	public void assertValueIsMap(String content) {
		Object value = assertExistsAndReturn(content);
		assertThat(failureReason("a map", value), value, instanceOf(Map.class));
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言给定路径上存在非空值.
	 * <p>如果JSON路径表达式不是{@linkplain JsonPath#isDefinite() definite}, 则此方法断言给定路径上的值不<em>为空</em>.
	 * 
	 * @param content JSON内容
	 */
	public void exists(String content) {
		assertExistsAndReturn(content);
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言给定路径中不存在值.
	 * <p>如果JSON路径表达式不是{@linkplain JsonPath#isDefinite() definite}, 则此方法断言给定路径上的值<em>为空</em>.
	 * 
	 * @param content JSON内容
	 */
	public void doesNotExist(String content) {
		Object value;
		try {
			value = evaluateJsonPath(content);
		}
		catch (AssertionError ex) {
			return;
		}
		String reason = failureReason("no value", value);
		if (pathIsIndefinite() && value instanceof List) {
			assertTrue(reason, ((List<?>) value).isEmpty());
		}
		else {
			assertTrue(reason, (value == null));
		}
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言给定路径中存在空值.
	 * <p>有关<em>empty</em>的语义, 参阅{@link ObjectUtils#isEmpty(Object)}的Javadoc.
	 * 
	 * @param content JSON内容
	 */
	public void assertValueIsEmpty(String content) {
		Object value = evaluateJsonPath(content);
		assertTrue(failureReason("an empty value", value), ObjectUtils.isEmpty(value));
	}

	/**
	 * 根据提供的{@code content}评估JSON路径表达式, 并断言给定路径中存在非空值.
	 * <p>有关<em>empty</em>的语义, 参阅{@link ObjectUtils#isEmpty(Object)}的Javadoc.
	 * 
	 * @param content JSON内容
	 */
	public void assertValueIsNotEmpty(String content) {
		Object value = evaluateJsonPath(content);
		assertTrue(failureReason("a non-empty value", value), !ObjectUtils.isEmpty(value));
	}

	private String failureReason(String expectedDescription, Object value) {
		return String.format("Expected %s at JSON path \"%s\" but found: %s", expectedDescription, this.expression,
				ObjectUtils.nullSafeToString(StringUtils.quoteIfString(value)));
	}

	private Object evaluateJsonPath(String content) {
		try {
			return this.jsonPath.read(content);
		}
		catch (Throwable ex) {
			throw new AssertionError("No value at JSON path \"" + this.expression + "\": " + ex);
		}
	}

	private Object evaluateJsonPath(String content, Class<?> targetType) {
		try {
			return JsonPath.parse(content).read(this.expression, targetType);
		}
		catch (Throwable ex) {
			throw new AssertionError("No value at JSON path \"" + this.expression + "\": " + ex);
		}
	}

	private Object assertExistsAndReturn(String content) {
		Object value = evaluateJsonPath(content);
		String reason = "No value at JSON path \"" + this.expression + "\"";
		assertTrue(reason, value != null);
		if (pathIsIndefinite() && value instanceof List) {
			assertTrue(reason, !((List<?>) value).isEmpty());
		}
		return value;
	}

	private boolean pathIsIndefinite() {
		return !this.jsonPath.isDefinite();
	}
}
