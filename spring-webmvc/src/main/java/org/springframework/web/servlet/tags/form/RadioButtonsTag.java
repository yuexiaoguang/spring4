package org.springframework.web.servlet.tags.form;

/**
 * Databinding-aware JSP tag for rendering multiple HTML '{@code input}'
 * elements with a '{@code type}' of '{@code radio}'.
 *
 * <p>Rendered elements are marked as 'checked' if the configured
 * {@link #setItems(Object) value} matches the bound value.
 */
@SuppressWarnings("serial")
public class RadioButtonsTag extends AbstractMultiCheckedElementTag {

	@Override
	protected String getInputType() {
		return "radio";
	}

}
