package org.springframework.test.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 * 替换{@link org.hamcrest.MatcherAssert}, 在使用Hamcrest 1.1时无需依赖"hamcrest-all",
 * 并且还保持与Hamcrest 1.1的向后兼容性 (也嵌入在JUnit 4.4到4.8中).
 *
 * @deprecated as of Spring 4.2, in favor of the original
 * {@link org.hamcrest.MatcherAssert} class with JUnit 4.9 / Hamcrest 1.3
 */
@Deprecated
public abstract class MatcherAssertionErrors {

	/**
	 * 断言给定匹配器匹配实际值.
	 * 
	 * @param <T> 匹配器接受的静态类型
	 * @param actual 要匹配的值
	 * @param matcher 匹配器
	 */
	public static <T> void assertThat(T actual, Matcher<T> matcher) {
		assertThat("", actual, matcher);
	}

	/**
	 * 断言给定匹配器匹配实际值.
	 * 
	 * @param <T> 匹配器接受的静态类型
	 * @param reason 有关错误的其他信息
	 * @param actual 要匹配的值
	 * @param matcher 匹配器
	 */
	public static <T> void assertThat(String reason, T actual, Matcher<T> matcher) {
		if (!matcher.matches(actual)) {
			Description description = new StringDescription();
			description.appendText(reason);
			description.appendText("\nExpected: ");
			description.appendDescriptionOf(matcher);
			description.appendText("\n     but: ");
			matcher.describeMismatch(actual, description);
			throw new AssertionError(description.toString());
		}
	}

}
