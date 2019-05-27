package org.springframework.mock.web;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;

/**
 * Mock implementation of the JSP 2.0 {@link javax.servlet.jsp.el.ExpressionEvaluator}
 * interface, delegating to the Apache JSTL ExpressionEvaluatorManager.
 *
 * <p>Used for testing the web framework; only necessary for testing applications
 * when testing custom JSP tags.
 *
 * <p>Note that the Apache JSTL implementation (jstl.jar, standard.jar) has to be
 * available on the class path to use this expression evaluator.
 */
@SuppressWarnings("deprecation")
public class MockExpressionEvaluator extends javax.servlet.jsp.el.ExpressionEvaluator {

	private final PageContext pageContext;


	/**
	 * Create a new MockExpressionEvaluator for the given PageContext.
	 * @param pageContext the JSP PageContext to run in
	 */
	public MockExpressionEvaluator(PageContext pageContext) {
		this.pageContext = pageContext;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public javax.servlet.jsp.el.Expression parseExpression(final String expression, final Class expectedType,
			final javax.servlet.jsp.el.FunctionMapper functionMapper) throws javax.servlet.jsp.el.ELException {

		return new javax.servlet.jsp.el.Expression() {
			@Override
			public Object evaluate(javax.servlet.jsp.el.VariableResolver variableResolver) throws javax.servlet.jsp.el.ELException {
				return doEvaluate(expression, expectedType, functionMapper);
			}
		};
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(String expression, Class expectedType, javax.servlet.jsp.el.VariableResolver variableResolver,
			javax.servlet.jsp.el.FunctionMapper functionMapper) throws javax.servlet.jsp.el.ELException {

		if (variableResolver != null) {
			throw new IllegalArgumentException("Custom VariableResolver not supported");
		}
		return doEvaluate(expression, expectedType, functionMapper);
	}

	@SuppressWarnings("rawtypes")
	protected Object doEvaluate(String expression, Class expectedType, javax.servlet.jsp.el.FunctionMapper functionMapper)
			throws javax.servlet.jsp.el.ELException {

		if (functionMapper != null) {
			throw new IllegalArgumentException("Custom FunctionMapper not supported");
		}
		try {
			return ExpressionEvaluatorManager.evaluate("JSP EL expression", expression, expectedType, this.pageContext);
		}
		catch (JspException ex) {
			throw new javax.servlet.jsp.el.ELException("Parsing of JSP EL expression \"" + expression + "\" failed", ex);
		}
	}

}
