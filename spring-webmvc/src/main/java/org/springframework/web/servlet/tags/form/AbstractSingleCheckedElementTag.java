package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 抽象基类, 提供实现数据绑定感知JSP标记的常用方法,
 * 用于呈现'{@code type}'为'{@code checkbox}' 或 '{@code radio}'的 <i>单个</i> HTML '{@code input}'元素.
 */
@SuppressWarnings("serial")
public abstract class AbstractSingleCheckedElementTag extends AbstractCheckedElementTag {

	/**
	 * '{@code value}'属性的值.
	 */
	private Object value;

	/**
	 * '{@code label}'属性的值.
	 */
	private Object label;


	/**
	 * 设置'{@code value}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 获取'{@code value}'属性的值.
	 */
	protected Object getValue() {
		return this.value;
	}

	/**
	 * 设置'{@code label}'属性的值.
	 * 可能是运行时表达式.
	 */
	public void setLabel(Object label) {
		this.label = label;
	}

	/**
	 * 获取'{@code label}'属性的值.
	 */
	protected Object getLabel() {
		return this.label;
	}


	/**
	 * 使用配置的{@link #setValue(Object) 值}渲染'{@code input(radio)}'元素.
	 * 如果值与{@link #getValue 绑定的值}匹配, 则将元素标记为已选中.
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");
		String id = resolveId();
		writeOptionalAttribute(tagWriter, "id", id);
		writeOptionalAttribute(tagWriter, "name", getName());
		writeOptionalAttributes(tagWriter);
		writeTagDetails(tagWriter);
		tagWriter.endTag();

		Object resolvedLabel = evaluate("label", getLabel());
		if (resolvedLabel != null) {
			tagWriter.startTag("label");
			tagWriter.writeAttribute("for", id);
			tagWriter.appendValue(convertToDisplayString(resolvedLabel));
			tagWriter.endTag();
		}

		return SKIP_BODY;
	}

	/**
	 * 写入给定主标记的详细信息: i.e. 特殊属性和标记的值.
	 */
	protected abstract void writeTagDetails(TagWriter tagWriter) throws JspException;

}
