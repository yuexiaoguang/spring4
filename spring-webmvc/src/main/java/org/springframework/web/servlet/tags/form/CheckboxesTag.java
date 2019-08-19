package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

import org.springframework.web.bind.WebDataBinder;

/**
 * 用于呈现多个HTML '{@code input}'元素的数据绑定感知JSP标记, 其中'{@code type}'为 '{@code checkbox}'.
 *
 * <p>打算与Collection一起用作 {@link #getItems()} 绑定值}.
 */
@SuppressWarnings("serial")
public class CheckboxesTag extends AbstractMultiCheckedElementTag {

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
	protected String getInputType() {
		return "checkbox";
	}

}
