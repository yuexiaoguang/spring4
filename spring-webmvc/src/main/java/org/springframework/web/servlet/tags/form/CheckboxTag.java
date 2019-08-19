package org.springframework.web.servlet.tags.form;

import java.util.Collection;
import javax.servlet.jsp.JspException;

import org.springframework.web.bind.WebDataBinder;

/**
 * 用于呈现HTML '{@code input}'元素的数据绑定感知JSP标记, 其中 '{@code type}'为'{@code checkbox}'.
 *
 * <p>根据{@link #getValue 绑定值}的类型, 可以使用三种不同的方法之一.
 *
 * <h3>方法一</h3>
 * 当绑定值的类型为{@link Boolean}时, 如果绑定值为{@code true}, 则'{@code input(checkbox)}'被标记为 'checked'.
 * '{@code value}'属性对应于{@link #setValue(Object) value}属性的解析后的值.
 * <h3>方法二</h3>
 * 当绑定值的类型为{@link Collection}时, 如果配置的{@link #setValue(Object) 值}存在,
 * 则'{@code input(checkbox)}'被标记为'checked'.
 * <h3>方法三</h3>
 * 对于任何其他绑定值类型，如果配置的{@link #setValue(Object) 值}等于绑定值,
 * 则'{@code input(checkbox)}'被标记为'checked'.
 */
@SuppressWarnings("serial")
public class CheckboxTag extends AbstractSingleCheckedElementTag {

	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		super.writeTagContent(tagWriter);

		if (!isDisabled()) {
			// Write out the 'field was present' marker.
			tagWriter.startTag("input");
			tagWriter.writeAttribute("type", "hidden");
			String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
			tagWriter.writeAttribute("name", name);
			tagWriter.writeAttribute("value", processFieldValue(name, "on", "hidden"));
			tagWriter.endTag();
		}

		return SKIP_BODY;
	}

	@Override
	protected void writeTagDetails(TagWriter tagWriter) throws JspException {
		tagWriter.writeAttribute("type", getInputType());

		Object boundValue = getBoundValue();
		Class<?> valueType = getBindStatus().getValueType();

		if (Boolean.class == valueType || boolean.class == valueType) {
			// the concrete type may not be a Boolean - can be String
			if (boundValue instanceof String) {
				boundValue = Boolean.valueOf((String) boundValue);
			}
			Boolean booleanValue = (boundValue != null ? (Boolean) boundValue : Boolean.FALSE);
			renderFromBoolean(booleanValue, tagWriter);
		}

		else {
			Object value = getValue();
			if (value == null) {
				throw new IllegalArgumentException("Attribute 'value' is required when binding to non-boolean values");
			}
			Object resolvedValue = (value instanceof String ? evaluate("value", value) : value);
			renderFromValue(resolvedValue, tagWriter);
		}
	}

	@Override
	protected String getInputType() {
		return "checkbox";
	}

}
