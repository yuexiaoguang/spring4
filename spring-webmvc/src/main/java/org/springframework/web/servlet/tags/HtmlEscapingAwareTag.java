package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;

import org.springframework.web.util.HtmlUtils;

/**
 * 用于输出可能被HTML转义的内容的标签的超类.
 *
 * <p>提供"htmlEscape"属性, 用于显式指定是否应用HTML转义.
 * 如果未设置, 则使用页面级默认值 (e.g. 来自HtmlEscapeTag)
 * 或应用程序范围的默认值 ({@code web.xml}中的"defaultHtmlEscape" context-param).
 */
@SuppressWarnings("serial")
public abstract class HtmlEscapingAwareTag extends RequestContextAwareTag {

	private Boolean htmlEscape;


	/**
	 * 设置此标记的HTML转义.
	 * 覆盖当前页面的默认HTML转义设置.
	 */
	public void setHtmlEscape(boolean htmlEscape) throws JspException {
		this.htmlEscape = htmlEscape;
	}

	/**
	 * 返回此标记的HTML转义设置, 如果未覆盖, 则返回默认设置.
	 */
	protected boolean isHtmlEscape() {
		if (this.htmlEscape != null) {
			return this.htmlEscape.booleanValue();
		}
		else {
			return isDefaultHtmlEscape();
		}
	}

	/**
	 * 返回此标记适用的默认HTML转义设置.
	 * <p>默认实现检查RequestContext的设置, 如果没有给出明确的默认值, 则返回{@code false}.
	 */
	protected boolean isDefaultHtmlEscape() {
		return getRequestContext().isDefaultHtmlEscape();
	}

	/**
	 * 返回适用的默认值, 以便在此标记的HTML转义中使用响应编码.
	 * <p>默认实现检查RequestContext的设置, 如果没有给出明确的默认值, 则返回{@code false}.
	 */
	protected boolean isResponseEncodedHtmlEscape() {
		return getRequestContext().isResponseEncodedHtmlEscape();
	}

	/**
	 * 仅在启用"htmlEscape"设置时对给定的String进行HTML编码.
	 * <p>如果同时启用"responseEncodedHtmlEscape"设置, 则会考虑响应编码.
	 * 
	 * @param content 要转义的字符串
	 * 
	 * @return 已转义的字符串
	 */
	protected String htmlEscape(String content) {
		String out = content;
		if (isHtmlEscape()) {
			if (isResponseEncodedHtmlEscape()) {
				out = HtmlUtils.htmlEscape(content, this.pageContext.getResponse().getCharacterEncoding());
			}
			else {
				out = HtmlUtils.htmlEscape(content);
			}
		}
		return out;
	}
}
