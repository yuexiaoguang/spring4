package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 在模型上断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#model}访问此类的实例.
 */
public class ModelResultMatchers {

	/**
	 * Use {@link MockMvcResultMatchers#model()}.
	 */
	protected ModelResultMatchers() {
	}


	/**
	 * 使用给定的Hamcrest {@link Matcher}断言模型属性值.
	 */
	public <T> ResultMatcher attribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@Override
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				assertThat("Model attribute '" + name + "'", (T) mav.getModel().get(name), matcher);
			}
		};
	}

	/**
	 * 断言模型属性值.
	 */
	public ResultMatcher attribute(final String name, final Object value) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				assertEquals("Model attribute '" + name + "'", value, mav.getModel().get(name));
			}
		};
	}

	/**
	 * 断言存在给定的模型属性.
	 */
	public ResultMatcher attributeExists(final String... names) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				for (String name : names) {
					assertTrue("Model attribute '" + name + "' does not exist", mav.getModel().get(name) != null);
				}
			}
		};
	}

    /**
     * 断言给定的模型属性不存在
     */
    public ResultMatcher attributeDoesNotExist(final String... names) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                ModelAndView mav = getModelAndView(result);
                for (String name : names) {
                    assertTrue("Model attribute '" + name + "' exists", mav.getModel().get(name) == null);
                }
            }
        };
    }

	/**
	 * 断言给定的模型属性有错误.
	 */
	public ResultMatcher attributeErrorCount(final String name, final int expectedCount) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				Errors errors = getBindingResult(mav, name);
				assertEquals("Binding/validation error count for attribute '" + name + "', ",
						expectedCount, errors.getErrorCount());
			}
		};
	}

	/**
	 * 断言给定的模型属性有错误.
	 */
	public ResultMatcher attributeHasErrors(final String... names) {
		return new ResultMatcher() {
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				for (String name : names) {
					BindingResult result = getBindingResult(mav, name);
					assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
				}
			}
		};
	}

	/**
	 * 断言给定的模型属性没有错误.
	 */
	public ResultMatcher attributeHasNoErrors(final String... names) {
		return new ResultMatcher() {
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				for (String name : names) {
					BindingResult result = getBindingResult(mav, name);
					assertTrue("Unexpected errors for attribute '" + name + "': " + result.getAllErrors(),
							!result.hasErrors());
				}
			}
		};
	}

	/**
	 * 断言给定的模型属性字段有错误.
	 */
	public ResultMatcher attributeHasFieldErrors(final String name, final String... fieldNames) {
		return new ResultMatcher() {
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				BindingResult result = getBindingResult(mav, name);
				assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
				for (final String fieldName : fieldNames) {
					boolean hasFieldErrors = result.hasFieldErrors(fieldName);
					assertTrue("No errors for field '" + fieldName + "' of attribute '" + name + "'", hasFieldErrors);
				}
			}
		};
	}

	/**
	 * 使用精确字符串匹配, 断言模型属性的字段错误代码.
	 */
	public ResultMatcher attributeHasFieldErrorCode(final String name, final String fieldName, final String error) {
		return new ResultMatcher() {
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				BindingResult result = getBindingResult(mav, name);
				assertTrue("No errors for attribute '" + name + "'", result.hasErrors());
				boolean hasFieldErrors = result.hasFieldErrors(fieldName);
				assertTrue("No errors for field '" + fieldName + "' of attribute '" + name + "'", hasFieldErrors);
				String code = result.getFieldError(fieldName).getCode();
				assertTrue("Expected error code '" + error + "' but got '" + code + "'", code.equals(error));
			}
		};
	}

	/**
	 * 使用{@link org.hamcrest.Matcher}断言模型属性的字段错误代码.
	 */
	public <T> ResultMatcher attributeHasFieldErrorCode(final String name, final String fieldName,
			final Matcher<? super String> matcher) {

		return new ResultMatcher() {
			@Override
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				BindingResult result = getBindingResult(mav, name);
				assertTrue("No errors for attribute: [" + name + "]", result.hasErrors());
				boolean hasFieldErrors = result.hasFieldErrors(fieldName);
				assertTrue("No errors for field '" + fieldName + "' of attribute '" + name + "'", hasFieldErrors);
				String code = result.getFieldError(fieldName).getCode();
				assertThat("Field name '" + fieldName + "' of attribute '" + name + "'", code, matcher);
			}
		};
	}

	/**
	 * 断言模型中的错误总数.
	 */
	public <T> ResultMatcher errorCount(final int expectedCount) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				int actualCount = getErrorCount(getModelAndView(result).getModelMap());
				assertEquals("Binding/validation error count", expectedCount, actualCount);
			}
		};
	}

	/**
	 * 断言模型有错误.
	 */
	public <T> ResultMatcher hasErrors() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				int count = getErrorCount(getModelAndView(result).getModelMap());
				assertTrue("Expected binding/validation errors", count != 0);
			}
		};
	}

	/**
	 * 断言模型没有错误.
	 */
	public <T> ResultMatcher hasNoErrors() {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				for (Object value : mav.getModel().values()) {
					if (value instanceof Errors) {
						assertTrue("Unexpected binding/validation errors: " + value, !((Errors) value).hasErrors());
					}
				}
			}
		};
	}

	/**
	 * 断言模型属性的数量.
	 */
	public <T> ResultMatcher size(final int size) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				int actual = 0;
				for (String key : mav.getModel().keySet()) {
					if (!key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
						actual++;
					}
				}
				assertEquals("Model size", size, actual);
			}
		};
	}

	private ModelAndView getModelAndView(MvcResult mvcResult) {
		ModelAndView mav = mvcResult.getModelAndView();
		assertTrue("No ModelAndView found", mav != null);
		return mav;
	}

	private BindingResult getBindingResult(ModelAndView mav, String name) {
		BindingResult result = (BindingResult) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
		assertTrue("No BindingResult for attribute: " + name, result != null);
		return result;
	}

	private int getErrorCount(ModelMap model) {
		int count = 0;
		for (Object value : model.values()) {
			if (value instanceof Errors) {
				count += ((Errors) value).getErrorCount();
			}
		}
		return count;
	}

}
