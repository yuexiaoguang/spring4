package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * Factory for "output" flash attribute assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#flash}.
 */
public class FlashAttributeResultMatchers {

	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#flash()}.
	 */
	protected FlashAttributeResultMatchers() {
	}


	/**
	 * Assert a flash attribute's value with the given Hamcrest {@link Matcher}.
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
	 * Assert a flash attribute's value.
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
	 * Assert the existence of the given flash attributes.
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
	 * Assert the number of flash attributes.
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
