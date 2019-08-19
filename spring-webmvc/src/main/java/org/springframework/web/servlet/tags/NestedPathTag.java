package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.springframework.beans.PropertyAccessor;

/**
 * <p>嵌套路径标记, 用于支持和协助模型中的嵌套bean或bean属性.
 * 在请求范围中导出String类型的"nestedPath"变量, 对当前页面可见, 并且还包括页面.
 *
 * <p>BindTag将自动检测当前的嵌套路径, 并自动将其添加到自己的路径, 以形成bean或bean属性的完整路径.
 *
 * <p>此标记还将添加当前设置的任何现有嵌套路径. 因此, 可以嵌套多个nested-path标记.
 *
 * <p>Thanks to Seth Ladd for the suggestion and the original implementation!
 */
@SuppressWarnings("serial")
public class NestedPathTag extends TagSupport implements TryCatchFinally {

	/**
	 * 此标记范围内的公开变量的名称: "nestedPath".
	 */
	public static final String NESTED_PATH_VARIABLE_NAME = "nestedPath";


	private String path;

	/** 缓存先前的嵌套路径, 以便可以重置它 */
	private String previousNestedPath;


	/**
	 * 设置此标记应应用的路径.
	 * <p>E.g. "customer"允许绑定路径, 如"address.street"而不是"customer.address.street".
	 */
	public void setPath(String path) {
		if (path == null) {
			path = "";
		}
		if (path.length() > 0 && !path.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
			path += PropertyAccessor.NESTED_PROPERTY_SEPARATOR;
		}
		this.path = path;
	}

	/**
	 * 返回此标记适用的路径.
	 */
	public String getPath() {
		return this.path;
	}


	@Override
	public int doStartTag() throws JspException {
		// 保存以前的nestedPath值, 构建并公开当前的nestedPath值.
		// 使用请求范围也可以将nestedPath公开给包含的页面.
		this.previousNestedPath =
				(String) pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		String nestedPath =
				(this.previousNestedPath != null ? this.previousNestedPath + getPath() : getPath());
		pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, nestedPath, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 重置以前的任何nestedPath值.
	 */
	@Override
	public int doEndTag() {
		if (this.previousNestedPath != null) {
			// 公开先前的nestedPath值.
			pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
		}
		else {
			// 删除公开的nestedPath值.
			pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}

		return EVAL_PAGE;
	}

	@Override
	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	@Override
	public void doFinally() {
		this.previousNestedPath = null;
	}

}
