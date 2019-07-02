package org.springframework.mock.web;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;

/**
 * JSP 2.0 {@link javax.servlet.jsp.el.ExpressionEvaluator}接口的模拟实现,
 * 委托给Apache JSTL ExpressionEvaluatorManager.
 *
 * <p>用于测试Web框架; 仅在测试自定义JSP标记时测试应用程序所必需的.
 *
 * <p>请注意, 必须在类路径上使用Apache JSTL实现 (jstl.jar, standard.jar)才能使用此表达式求值器.
 */
@SuppressWarnings("deprecation")
public class MockExpressionEvaluator extends javax.servlet.jsp.el.ExpressionEvaluator {

	private final PageContext pageContext;


	/**
	 * @param pageContext 要运行的JSP PageContext
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
