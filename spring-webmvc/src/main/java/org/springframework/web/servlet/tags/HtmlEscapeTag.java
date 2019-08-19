package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;

/**
 * 设置当前页面的默认HTML转义值. 转义识别标记可以覆盖实际值. 默认为"false".
 *
 * <p>Note: 还可以设置"defaultHtmlEscape" web.xml context-param. 页面级设置会覆盖context-param.
 */
@SuppressWarnings("serial")
public class HtmlEscapeTag extends RequestContextAwareTag {

	private boolean defaultHtmlEscape;


	/**
	 * 设置HTML转义的默认值, 将其放入当前的PageContext中.
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}


	@Override
	protected int doStartTagInternal() throws JspException {
		getRequestContext().setDefaultHtmlEscape(this.defaultHtmlEscape);
		return EVAL_BODY_INCLUDE;
	}

}
