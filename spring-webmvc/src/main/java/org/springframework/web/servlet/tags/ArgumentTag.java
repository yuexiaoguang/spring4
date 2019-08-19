package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * 用于收集参数并将其传递给标记层次结构中的{@link ArgumentAware}祖先的JSP标记.
 *
 * <p>此标记必须嵌套在参数感知标记下.
 */
@SuppressWarnings("serial")
public class ArgumentTag extends BodyTagSupport {

	private Object value;

	private boolean valueSet;


	/**
	 * 设置参数的值 (可选).
	 * <p>如果未设置, 将评估标签的正文内容.
	 * 
	 * @param value 参数值
	 */
	public void setValue(Object value) {
		this.value = value;
		this.valueSet = true;
	}


	@Override
	public int doEndTag() throws JspException {
		Object argument = null;
		if (this.valueSet) {
			argument = this.value;
		}
		else if (getBodyContent() != null) {
			// Get the value from the tag body
			argument = getBodyContent().getString().trim();
		}

		// Find a param-aware ancestor
		ArgumentAware argumentAwareTag = (ArgumentAware) findAncestorWithClass(this, ArgumentAware.class);
		if (argumentAwareTag == null) {
			throw new JspException("The argument tag must be a descendant of a tag that supports arguments");
		}
		argumentAwareTag.addArgument(argument);

		return EVAL_PAGE;
	}

	@Override
	public void release() {
		super.release();
		this.value = null;
		this.valueSet = false;
	}

}
