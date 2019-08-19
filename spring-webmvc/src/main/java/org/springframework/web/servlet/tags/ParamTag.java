package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * 用于收集 name-value参数并将其传递到标记层次结构中的{@link ParamAware}祖先的JSP标记.
 *
 * <p>此标记必须嵌套在param aware标记下.
 */
@SuppressWarnings("serial")
public class ParamTag extends BodyTagSupport {

	private String name;

	private String value;

	private boolean valueSet;


	/**
	 * 设置参数的名称 (必需).
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置参数的值 (可选).
	 */
	public void setValue(String value) {
		this.value = value;
		this.valueSet = true;
	}


	@Override
	public int doEndTag() throws JspException {
		Param param = new Param();
		param.setName(this.name);
		if (this.valueSet) {
			param.setValue(this.value);
		}
		else if (getBodyContent() != null) {
			// Get the value from the tag body
			param.setValue(getBodyContent().getString().trim());
		}

		// Find a param aware ancestor
		ParamAware paramAwareTag = (ParamAware) findAncestorWithClass(this, ParamAware.class);
		if (paramAwareTag == null) {
			throw new JspException("The param tag must be a descendant of a tag that supports parameters");
		}

		paramAwareTag.addParam(param);

		return EVAL_PAGE;
	}

	@Override
	public void release() {
		super.release();
		this.name = null;
		this.value = null;
		this.valueSet = false;
	}

}
