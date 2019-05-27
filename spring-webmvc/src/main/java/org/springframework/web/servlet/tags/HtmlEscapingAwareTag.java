package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;

import org.springframework.web.util.HtmlUtils;

/**
 * Superclass for tags that output content that might get HTML-escaped.
 *
 * <p>Provides a "htmlEscape" property for explicitly specifying whether to
 * apply HTML escaping. If not set, a page-level default (e.g. from the
 * HtmlEscapeTag) or an application-wide default (the "defaultHtmlEscape"
 * context-param in {@code web.xml}) is used.
 */
@SuppressWarnings("serial")
public abstract class HtmlEscapingAwareTag extends RequestContextAwareTag {

	private Boolean htmlEscape;


	/**
	 * Set HTML escaping for this tag, as boolean value.
	 * Overrides the default HTML escaping setting for the current page.
	 */
	public void setHtmlEscape(boolean htmlEscape) throws JspException {
		this.htmlEscape = htmlEscape;
	}

	/**
	 * Return the HTML escaping setting for this tag,
	 * or the default setting if not overridden.
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
	 * Return the applicable default HTML escape setting for this tag.
	 * <p>The default implementation checks the RequestContext's setting,
	 * falling back to {@code false} in case of no explicit default given.
	 */
	protected boolean isDefaultHtmlEscape() {
		return getRequestContext().isDefaultHtmlEscape();
	}

	/**
	 * Return the applicable default for the use of response encoding with
	 * HTML escaping for this tag.
	 * <p>The default implementation checks the RequestContext's setting,
	 * falling back to {@code false} in case of no explicit default given.
	 * @since 4.1.2
	 */
	protected boolean isResponseEncodedHtmlEscape() {
		return getRequestContext().isResponseEncodedHtmlEscape();
	}

	/**
	 * HTML-encodes the given String, only if the "htmlEscape" setting is enabled.
	 * <p>The response encoding will be taken into account if the
	 * "responseEncodedHtmlEscape" setting is enabled as well.
	 * @param content the String to escape
	 * @return the escaped String
	 * @since 4.1.2
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
