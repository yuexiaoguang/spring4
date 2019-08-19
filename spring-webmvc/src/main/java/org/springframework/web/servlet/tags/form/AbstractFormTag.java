package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import javax.servlet.jsp.JspException;

import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;

/**
 * 所有JSP表单标记的基类.
 * 提供用于null安全EL评估以及访问和使用{@link TagWriter}的工具方法.
 *
 * <p>子类应实现{@link #writeTagContent(TagWriter)}来执行实际的标记呈现.
 *
 * <p>子类 (或测试类) 可以覆盖{@link #createTagWriter()}方法,
 * 将输出重定向到与当前{@link javax.servlet.jsp.PageContext}关联的{@link javax.servlet.jsp.JspWriter}之外的{@link java.io.Writer}.
 */
@SuppressWarnings("serial")
public abstract class AbstractFormTag extends HtmlEscapingAwareTag {

	/**
	 * 评估提供的属性名称的提供值.
	 * <p>默认实现只是按原样返回给定值.
	 */
	protected Object evaluate(String attributeName, Object value) throws JspException {
		return value;
	}

	/**
	 * 将提供的属性名称下的提供值写入提供的{@link TagWriter} (可选).
	 * 在这种情况下, 首先提供的值被{@link #evaluate 评估}, 然后将{@link ObjectUtils#getDisplayString String表示}写为属性值.
	 * 如果生成的{@code String}表示为{@code null}或为空, 则不写入任何属性.
	 */
	protected final void writeOptionalAttribute(TagWriter tagWriter, String attributeName, String value)
			throws JspException {

		if (value != null) {
			tagWriter.writeOptionalAttributeValue(attributeName, getDisplayString(evaluate(attributeName, value)));
		}
	}

	/**
	 * 创建将写入所有输出的{@link TagWriter}.
	 * 默认情况下, {@link TagWriter}将其输出写入{@link javax.servlet.jsp.JspWriter},
	 * 用于当前{@link javax.servlet.jsp.PageContext}.
	 * 子类可以选择更改实际写入输出的{@link java.io.Writer}.
	 */
	protected TagWriter createTagWriter() {
		return new TagWriter(this.pageContext);
	}

	/**
	 * 提供一个简单的模板方法, 调用{@link #createTagWriter()},
	 * 并将创建的{@link TagWriter}传递给{@link #writeTagContent(TagWriter)}方法.
	 * 
	 * @return {@link #writeTagContent(TagWriter)}返回的值
	 */
	@Override
	protected final int doStartTagInternal() throws Exception {
		return writeTagContent(createTagWriter());
	}

	/**
	 * 获取所提供的{@code Object}的显示值, 根据需要进行HTML转义.
	 * 此版本<strong>不是</strong> {@link PropertyEditor}感知的.
	 */
	protected String getDisplayString(Object value) {
		return ValueFormatter.getDisplayString(value, isHtmlEscape());
	}

	/**
	 * 获取所提供的{@code Object}的显示值, 根据需要进行HTML转义.
	 * 如果提供的值不是{@link String}且提供的{@link PropertyEditor}不为null, 则使用{@link PropertyEditor}获取显示值.
	 */
	protected String getDisplayString(Object value, PropertyEditor propertyEditor) {
		return ValueFormatter.getDisplayString(value, propertyEditor, isHtmlEscape());
	}

	/**
	 * 如果没有给出明确的默认值, 则默认为{@code true}.
	 */
	@Override
	protected boolean isDefaultHtmlEscape() {
		Boolean defaultHtmlEscape = getRequestContext().getDefaultHtmlEscape();
		return (defaultHtmlEscape == null || defaultHtmlEscape.booleanValue());
	}


	/**
	 * 子类应实现此方法以执行标记内容呈现.
	 * 
	 * @return {@link javax.servlet.jsp.tagext.Tag#doStartTag()}的有效标记呈现指令.
	 */
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

}
