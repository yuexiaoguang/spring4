package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 用于"output" flash属性断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#flash}访问此类的实例.
 */
public class FlashAttributeResultMatchers {

	/**
	 * Use {@link MockMvcResultMatchers#flash()}.
	 */
	protected FlashAttributeResultMatchers() {
	}


	/**
	 * 使用给定的Hamcrest {@link Matcher}断言flash属性的值.
	 */
	public <T> ResultMatcher attribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@Override
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) throws Exception {
				assertThat("Flash attribute", (T) result.getFlashMap().get(name), matcher);
			}
		};
	}

	/**
	 * 断言flash属性的值.
	 */
	public <T> ResultMatcher attribute(final String name, final Object value) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertEquals("Flash attribute", value, result.getFlashMap().get(name));
			}
		};
	}

	/**
	 * 断言给定的flash属性的存在.
	 */
	public <T> ResultMatcher attributeExists(final String... names) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				for (String name : names) {
					assertTrue("Flash attribute [" + name + "] does not exist", result.getFlashMap().get(name) != null);
				}
			}
		};
	}

	/**
	 * 断言flash属性的数量.
	 */
	public <T> ResultMatcher attributeCount(final int count) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertEquals("FlashMap size", count, result.getFlashMap().size());
			}
		};
	}

}
