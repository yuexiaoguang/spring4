package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 在选定视图上进行断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#view}访问此类的实例.
 */
public class ViewResultMatchers {

	/**
	 * Use {@link MockMvcResultMatchers#view()}.
	 */
	protected ViewResultMatchers() {
	}


	/**
	 * 使用给定的Hamcrest {@link Matcher}断言所选视图名称.
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
	 * 断言选定的视图名称.
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
