package org.springframework.test.util;

import org.springframework.util.ObjectUtils;

/**
 * JUnit independent assertion class.
 */
public abstract class AssertionErrors {

	/**
	 * Fails a test with the given message.
	 * @param message describes the reason for the failure
	 */
	public static void fail(String message) {
		throw new AssertionError(message);
	}

	/**
	 * Fails a test with the given message passing along expected and actual
	 * values to be added to the message.
	 * <p>For example given:
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", actual, expected);
	 * </pre>
	 * <p>The resulting message is:
	 * <pre class="code">
	 * Response header [Accept] expected:&lt;application/json&gt; but was:&lt;text/plain&gt;
	 * </pre>
	 * @param message describes the value that failed the match
	 * @param expected expected value
	 * @param actual actual value
	 */
	public static void fail(String message, Object expected, Object actual) {
		throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
	}

	/**
	 * Assert the given condition is {@code true} and raise an
	 * {@link AssertionError} if it is not.
	 * @param message the message
	 * @param condition the condition to test for
	 */
	public static void assertTrue(String message, boolean condition) {
		if (!condition) {
			fail(message);
		}
	}

	/**
	 * Assert two objects are equal raise an {@link AssertionError} if not.
	 * <p>For example:
	 * <pre class="code">
	 * assertEquals("Response header [" + name + "]", actual, expected);
	 * </pre>
	 * @param message describes the value being checked
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	public static void assertEquals(String message, Object expected, Object actual) {
		if (!ObjectUtils.nullSafeEquals(expected, actual)) {
			fail(message, ObjectUtils.nullSafeToString(expected), ObjectUtils.nullSafeToString(actual));
		}
	}

}
