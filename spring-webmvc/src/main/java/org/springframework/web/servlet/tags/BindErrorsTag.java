package org.springframework.web.servlet.tags;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.validation.Errors;

/**
 * JSP标记, 用于在某个bean存在绑定错误时评估内容.
 * 为给定的bean导出{@link org.springframework.validation.Errors}类型的"errors"变量.
 */
@SuppressWarnings("serial")
public class BindErrorsTag extends HtmlEscapingAwareTag {

	public static final String ERRORS_VARIABLE_NAME = "errors";


	private String name;

	private Errors errors;


	/**
	 * 设置此标记应检查的bean的名称.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回此标记检查的bean的名称.
	 */
	public String getName() {
		return this.name;
	}


	@Override
	protected final int doStartTagInternal() throws ServletException, JspException {
		this.errors = getRequestContext().getErrors(this.name, isHtmlEscape());
		if (this.errors != null && this.errors.hasErrors()) {
			this.pageContext.setAttribute(ERRORS_VARIABLE_NAME, this.errors, PageContext.REQUEST_SCOPE);
			return EVAL_BODY_INCLUDE;
		}
		else {
			return SKIP_BODY;
		}
	}

	@Override
	public int doEndTag() {
		this.pageContext.removeAttribute(ERRORS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		return EVAL_PAGE;
	}

	/**
	 * 检索此标记当前绑定的Errors实例.
	 * <p>用于协作嵌套标签.
	 */
	public final Errors getErrors() {
		return this.errors;
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.errors = null;
	}
}
