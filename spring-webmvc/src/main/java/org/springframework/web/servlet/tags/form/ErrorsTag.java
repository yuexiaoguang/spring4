package org.springframework.web.servlet.tags.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于显示特定字段或对象的错误的表单标记.
 *
 * <p>此标记支持三种主要使用模式:
 *
 * <ol>
 *	<li>仅字段 - 将'{@code path}'设置为字段名称 (或路径)</li>
 *	<li>仅对象错误 - 省略'{@code path}'</li>
 *	<li>所有错误 - 将'{@code path}'设置为'{@code *}'</li>
 * </ol>
 */
@SuppressWarnings("serial")
public class ErrorsTag extends AbstractHtmlElementBodyTag implements BodyTag {

	/**
	 * 此标记在{@link PageContext#PAGE_SCOPE 页面上下文范围}中公开错误消息的键.
	 */
	public static final String MESSAGES_ATTRIBUTE = "messages";

	/**
	 * HTML '{@code span}'标记.
	 */
	public static final String SPAN_TAG = "span";


	private String element = SPAN_TAG;

	private String delimiter = "<br/>";

	/**
	 * 在标记启动之前存储'错误消息'中存在的任何值.
	 */
	private Object oldMessages;

	private boolean errorMessagesWereExposed;


	/**
	 * 设置必须用于呈现错误消息的HTML元素.
	 * <p>默认为HTML '{@code <span/>}'标记.
	 */
	public void setElement(String element) {
		Assert.hasText(element, "'element' cannot be null or blank");
		this.element = element;
	}

	/**
	 * 获取必须用于呈现错误消息的HTML元素.
	 */
	public String getElement() {
		return this.element;
	}

	/**
	 * 设置要在错误消息之间使用的分隔符.
	 * <p>默认为 HTML '{@code <br/>}'标记.
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * 返回错误消息之间使用的分隔符.
	 */
	public String getDelimiter() {
		return this.delimiter;
	}


	/**
	 * 获取HTML '{@code id}'属性的值.
	 * <p>如果忽略{@code <form:errors/>}标记的'{@code path}'属性,
	 * 追加'{@code .errors}'到{@link #getPropertyPath()}返回的值, 或到模型属性名称.
	 * 
	 * @return HTML '{@code id}'属性的值
	 */
	@Override
	protected String autogenerateId() throws JspException {
		String path = getPropertyPath();
		if ("".equals(path) || "*".equals(path)) {
			path = (String) this.pageContext.getAttribute(
					FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
		}
		return StringUtils.deleteAny(path, "[]") + ".errors";
	}

	/**
	 * 获取 HTML '{@code name}'属性的值.
	 * <p>只需返回{@code null}, 因为'{@code name}'属性不是'{@code span}'元素的验证属性.
	 */
	@Override
	protected String getName() throws JspException {
		return null;
	}

	/**
	 * 是否应该继续呈现此标记?
	 * <p>仅在配置的{@link #setPath path}出现错误时才呈现输出.
	 * 
	 * @return {@code true} 仅当配置的{@link #setPath path}出现错误时
	 */
	@Override
	protected boolean shouldRender() throws JspException {
		try {
			return getBindStatus().isError();
		}
		catch (IllegalStateException ex) {
			// Neither BindingResult nor target object available.
			return false;
		}
	}

	@Override
	protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag(getElement());
		writeDefaultAttributes(tagWriter);
		String delimiter = ObjectUtils.getDisplayString(evaluate("delimiter", getDelimiter()));
		String[] errorMessages = getBindStatus().getErrorMessages();
		for (int i = 0; i < errorMessages.length; i++) {
			String errorMessage = errorMessages[i];
			if (i > 0) {
				tagWriter.appendValue(delimiter);
			}
			tagWriter.appendValue(getDisplayString(errorMessage));
		}
		tagWriter.endTag();
	}

	/**
	 * 在{@link PageContext#PAGE_SCOPE}中 {@link #MESSAGES_ATTRIBUTE 此键}下公开任何绑定状态错误消息.
	 * <p>仅在{@link #shouldRender()}返回{@code true}时调用.
	 */
	@Override
	protected void exposeAttributes() throws JspException {
		List<String> errorMessages = new ArrayList<String>();
		errorMessages.addAll(Arrays.asList(getBindStatus().getErrorMessages()));
		this.oldMessages = this.pageContext.getAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
		this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, errorMessages, PageContext.PAGE_SCOPE);
		this.errorMessagesWereExposed = true;
	}

	/**
	 * 删除以前存储在{@link PageContext#PAGE_SCOPE}中{@link #MESSAGES_ATTRIBUTE 此键}下的任何绑定状态错误消息.
	 */
	@Override
	protected void removeAttributes() {
		if (this.errorMessagesWereExposed) {
			if (this.oldMessages != null) {
				this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, this.oldMessages, PageContext.PAGE_SCOPE);
				this.oldMessages = null;
			}
			else {
				this.pageContext.removeAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
			}
		}
	}

}
