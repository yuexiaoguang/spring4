package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 用于呈现'{@code type}'为'{@code radio}'的HTML '{@code input}'元素的数据绑定感知JSP标记.
 *
 * <p>如果配置的{@link #setValue(Object) 值}与{@link #getValue 绑定的值}匹配, 则渲染元素将标记为'checked'.
 *
 * <p>典型的使用模式将涉及绑定到同一属性但具有不同值的多个标记实例.
 */
@SuppressWarnings("serial")
public class RadioButtonTag extends AbstractSingleCheckedElementTag {

	@Override
	protected void writeTagDetails(TagWriter tagWriter) throws JspException {
		tagWriter.writeAttribute("type", getInputType());
		Object resolvedValue = evaluate("value", getValue());
		renderFromValue(resolvedValue, tagWriter);
	}

	@Override
	protected String getInputType() {
		return "radio";
	}

}
