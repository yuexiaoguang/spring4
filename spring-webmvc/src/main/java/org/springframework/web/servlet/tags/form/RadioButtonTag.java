package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * Databinding-aware JSP tag for rendering an HTML '{@code input}'
 * element with a '{@code type}' of '{@code radio}'.
 *
 * <p>Rendered elements are marked as 'checked' if the configured
 * {@link #setValue(Object) value} matches the {@link #getValue bound value}.
 *
 * <p>A typical usage pattern will involved multiple tag instances bound
 * to the same property but with different values.
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
