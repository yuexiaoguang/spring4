package org.springframework.test.util;

import org.springframework.util.ObjectUtils;

/**
 * JUnit独立断言类.
 */
public abstract class AssertionErrors {

	/**
	 * 给定消息的失败测试.
	 * 
	 * @param message 描述了失败的原因
	 */
	public static void fail(String message) {
		throw new AssertionError(message);
	}

	/**
	 * 给定消息的测试失败, 将预期值和实际值传递给消息.
	 * <p>示例:
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", actual, expected);
	 * </pre>
	 * <p>结果消息:
	 * <pre class="code">
	 * Response header [Accept] expected:&lt;application/json&gt; but was:&lt;text/plain&gt;
	 * </pre>
	 * 
	 * @param message 描述匹配失败的值
	 * @param expected 预期值
	 * @param actual 实际值
	 */
	public static void fail(String message, Object expected, Object actual) {
		throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
	}

	/**
	 * 断言给定的条件是{@code true}, 如果不是, 则引发{@link AssertionError}.
	 * 
	 * @param message 消息
	 * @param condition 要测试的条件
	 */
	public static void assertTrue(String message, boolean condition) {
		if (!condition) {
			fail(message);
		}
	}

	/**
	 * 断言两个对象相等, 如果不相等, 引发{@link AssertionError}.
	 * <p>示例:
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", actual, expected);
	 * </pre>
	 * 
	 * @param message 描述要检查的值
	 * @param expected 预期值
	 * @param actual 实际值
	 */
	public static void assertEquals(String message, Object expected, Object actual) {
		if (!ObjectUtils.nullSafeEquals(expected, actual)) {
			fail(message, ObjectUtils.nullSafeToString(expected), ObjectUtils.nullSafeToString(actual));
		}
	}
}
