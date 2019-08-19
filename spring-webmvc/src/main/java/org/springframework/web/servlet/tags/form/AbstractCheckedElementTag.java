package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 抽象基类, 提供实现数据绑定感知JSP标记的常用方法,
 * 用于呈现'{@code type}'为'{@code checkbox}' 或 '{@code radio}'的 HTML '{@code input}'元素.
 */
@SuppressWarnings("serial")
public abstract class AbstractCheckedElementTag extends AbstractHtmlInputElementTag {

	/**
	 * 使用提供的值渲染'{@code input(checkbox)}', 如果提供的值与绑定值匹配, 则将 '{@code input}'元素标记为'checked'.
	 */
	protected void renderFromValue(Object value, TagWriter tagWriter) throws JspException {
		renderFromValue(value, value, tagWriter);
	}

	/**
	 * 使用提供的值渲染'{@code input(checkbox)}', 如果提供的值与绑定值匹配, 则将 '{@code input}'元素标记为'checked'.
	 */
	protected void renderFromValue(Object item, Object value, TagWriter tagWriter) throws JspException {
		String displayValue = convertToDisplayString(value);
		tagWriter.writeAttribute("value", processFieldValue(getName(), displayValue, getInputType()));
		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * 通过委托给{@link SelectedValueComparator#isSelected}确定提供的值是否与所选值匹配.
	 */
	private boolean isOptionSelected(Object value) throws JspException {
		return SelectedValueComparator.isSelected(getBindStatus(), value);
	}

	/**
	 * 使用提供的值渲染'{@code input(checkbox)}', 如果提供的布尔值为{@code true}, 则将'{@code input}'元素标记为'checked'.
	 */
	protected void renderFromBoolean(Boolean boundValue, TagWriter tagWriter) throws JspException {
		tagWriter.writeAttribute("value", processFieldValue(getName(), "true", getInputType()));
		if (boundValue) {
			tagWriter.writeAttribute("checked", "checked");
		}
	}

	/**
	 * 返回当前PageContext中绑定名称的唯一ID.
	 */
	@Override
	protected String autogenerateId() throws JspException {
		return TagIdGenerator.nextId(super.autogenerateId(), this.pageContext);
	}


	/**
	 * 将'{@code input}'元素写入提供的{@link TagWriter}, 如果合适, 将其标记为'checked'.
	 */
	@Override
	protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

	/**
	 * 标记"type"为非法动态属性.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * 返回HTML输入元素的类型以生成: "checkbox" 或 "radio".
	 */
	protected abstract String getInputType();

}
