package org.springframework.web.servlet.tags;

import java.beans.PropertyEditor;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.BindStatus;

/**
 * 绑定标记, 支持评估某个bean或bean属性的绑定错误.
 * 将{@link org.springframework.web.servlet.support.BindStatus}类型的"status"变量暴露给Java表达式和JSP EL表达式.
 *
 * <p>可用于绑定到模型中的任何bean或bean属性.
 * 指定的路径确定标记是否公开bean本身的状态 (显示对象级错误), 特定的bean属性 (显示字段错误),
 * 或匹配的bean属性集 (显示所有相应的字段错误).
 *
 * <p>使用此标记绑定的{@link org.springframework.validation.Errors}对象暴露给协作标记,
 * 以及此errors对象应用于的bean属性.
 * 嵌套标记(例如 {@link TransformTag}) 可以访问这些公开的属性.
 */
@SuppressWarnings("serial")
public class BindTag extends HtmlEscapingAwareTag implements EditorAwareTag {

	/**
	 * 此标记范围内公开的变量的名称: "status".
	 */
	public static final String STATUS_VARIABLE_NAME = "status";


	private String path;

	private boolean ignoreNestedPath = false;

	private BindStatus status;

	private Object previousPageStatus;

	private Object previousRequestStatus;


	/**
	 * 设置此标记应应用的路径.
	 * 可以是获取全局错误的bean (e.g. "person"), 或者是获取字段错误的bean属性 (e.g. "person.name")
	 *  (也支持嵌套字段和"person.na*"映射).
	 * "person.*" 将返回指定bean的所有错误, 包括全局错误和字段错误.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * 返回此标记适用的路径.
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * 设置是否忽略嵌套路径.
	 * 默认不忽略.
	 */
	public void setIgnoreNestedPath(boolean ignoreNestedPath) {
	  this.ignoreNestedPath = ignoreNestedPath;
	}

	/**
	 * 返回是否忽略嵌套路径.
	 */
	public boolean isIgnoreNestedPath() {
	  return this.ignoreNestedPath;
	}


	@Override
	protected final int doStartTagInternal() throws Exception {
		String resolvedPath = getPath();
		if (!isIgnoreNestedPath()) {
			String nestedPath = (String) pageContext.getAttribute(
					NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
			// 只有前置, 如果不是绝对路径
			if (nestedPath != null && !resolvedPath.startsWith(nestedPath) &&
					!resolvedPath.equals(nestedPath.substring(0, nestedPath.length() - 1))) {
				resolvedPath = nestedPath + resolvedPath;
			}
		}

		try {
			this.status = new BindStatus(getRequestContext(), resolvedPath, isHtmlEscape());
		}
		catch (IllegalStateException ex) {
			throw new JspTagException(ex.getMessage());
		}

		// 保存以前的状态值, 以便在此标记的末尾重新公开.
		this.previousPageStatus = pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		this.previousRequestStatus = pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);

		// 将此标记的状态对象公开为PageContext属性, 使其可用于JSP EL.
		pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
		pageContext.setAttribute(STATUS_VARIABLE_NAME, this.status, PageContext.REQUEST_SCOPE);

		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() {
		// Reset previous status values.
		if (this.previousPageStatus != null) {
			pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousPageStatus, PageContext.PAGE_SCOPE);
		}
		if (this.previousRequestStatus != null) {
			pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousRequestStatus, PageContext.REQUEST_SCOPE);
		}
		else {
			pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		return EVAL_PAGE;
	}


	/**
	 * 检索此标记当前绑定的属性, 或{@code null} (如果绑定到对象而不是特定属性).
	 * 用于协作嵌套标签.
	 * 
	 * @return 此标记当前绑定的属性, 或{@code null}
	 */
	public final String getProperty() {
		return this.status.getExpression();
	}

	/**
	 * 检索此标记当前绑定的Errors实例.
	 * 用于协作嵌套标签.
	 * 
	 * @return 当前的Errors实例, 或{@code null}
	 */
	public final Errors getErrors() {
		return this.status.getErrors();
	}

	@Override
	public final PropertyEditor getEditor() {
		return this.status.getEditor();
	}


	@Override
	public void doFinally() {
		super.doFinally();
		this.status = null;
		this.previousPageStatus = null;
		this.previousRequestStatus = null;
	}

}
