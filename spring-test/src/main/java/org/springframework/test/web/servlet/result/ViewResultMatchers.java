package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * Factory for assertions on the selected view.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#view}.
 */
public class ViewResultMatchers {

	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#view()}.
	 */
	protected ViewResultMatchers() {
	}


	/**
	 * Assert the selected view name with the given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher name(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = result.getModelAndView();
				assertTrue("No ModelAndView found", mav != null);
				assertThat("View name", mav.getViewName(), matcher);
			}
		};
	}

	/**
	 * Assert the selected view name.
	 */
	public ResultMatcher name(final String expectedViewName) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = result.getModelAndView();
				assertTrue("No ModelAndView found", mav != null);
				assertEquals("View name", expectedViewName, mav.getViewName());
			}
		};
	}

}
