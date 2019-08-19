package org.springframework.web.servlet.tags;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.web.util.JavaScriptUtils;

/**
 * 自定义JSP标记, 以转义其封闭的正文内容, 应用HTML转义和/或JavaScript转义.
 *
 * <p>提供"htmlEscape"属性, 用于显式指定是否应用HTML转义.
 * 如果未设置, 则使用页面级默认值 (e.g. 来自 HtmlEscapeTag) 或应用程序范围的默认值 (web.xml中的"defaultHtmlEscape" context-param).
 *
 * <p>提供"javaScriptEscape"属性, 用于指定是否应用JavaScript转义. 可以与HTML转义或独立使用相结合.
 */
@SuppressWarnings("serial")
public class EscapeBodyTag extends HtmlEscapingAwareTag implements BodyTag {

	private boolean javaScriptEscape = false;

	private BodyContent bodyContent;


	/**
	 * 为此标记设置JavaScript转义.
	 * 默认为"false".
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	protected int doStartTagInternal() {
		// do nothing
		return EVAL_BODY_BUFFERED;
	}

	@Override
	public void doInitBody() {
		// do nothing
	}

	@Override
	public void setBodyContent(BodyContent bodyContent) {
		this.bodyContent = bodyContent;
	}

	@Override
	public int doAfterBody() throws JspException {
		try {
			String content = readBodyContent();
			// HTML and/or JavaScript escape, if demanded
			content = htmlEscape(content);
			content = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(content) : content);
			writeBodyContent(content);
		}
		catch (IOException ex) {
			throw new JspException("Could not write escaped body", ex);
		}
		return (SKIP_BODY);
	}

	/**
	 * 从页面中读取未转义的正文内容.
	 * 
	 * @return 原始内容
	 * 
	 * @throws IOException 如果读取失败
	 */
	protected String readBodyContent() throws IOException {
		return this.bodyContent.getString();
	}

	/**
	 * 将转义的正文内容写入页面.
	 * <p>可以在子类中重写, e.g. 用于测试.
	 * 
	 * @param content 要写入的内容
	 * 
	 * @throws IOException 如果写入失败
	 */
	protected void writeBodyContent(String content) throws IOException {
		this.bodyContent.getEnclosingWriter().print(content);
	}

}
