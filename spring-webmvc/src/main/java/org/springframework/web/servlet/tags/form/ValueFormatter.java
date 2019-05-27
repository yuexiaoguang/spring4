package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;

import org.springframework.util.ObjectUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Package-visible helper class for formatting values for rendering via a form tag.
 * Supports two styles of formatting: plain and {@link PropertyEditor}-aware.
 *
 * <p>Plain formatting simply prevents the string '{@code null}' from appearing,
 * replacing it with an empty String, and adds HTML escaping as required.
 *
 * <p>{@link PropertyEditor}-aware formatting will attempt to use the supplied
 * {@link PropertyEditor} to render any non-String value before applying the
 * default rules of plain formatting.
 */
abstract class ValueFormatter {

	/**
	 * Build the display value of the supplied {@code Object}, HTML escaped
	 * as required. This version is <strong>not</strong> {@link PropertyEditor}-aware.
	 */
	public static String getDisplayString(Object value, boolean htmlEscape) {
		String displayValue = ObjectUtils.getDisplayString(value);
		return (htmlEscape ? HtmlUtils.htmlEscape(displayValue) : displayValue);
	}

	/**
	 * Build the display value of the supplied {@code Object}, HTML escaped
	 * as required. If the supplied value is not a {@link String} and the supplied
	 * {@link PropertyEditor} is not null then the {@link PropertyEditor} is used
	 * to obtain the display value.
	 */
	public static String getDisplayString(Object value, PropertyEditor propertyEditor, boolean htmlEscape) {
		if (propertyEditor != null && !(value instanceof String)) {
			try {
				propertyEditor.setValue(value);
				String text = propertyEditor.getAsText();
				if (text != null) {
					return getDisplayString(text, htmlEscape);
				}
			}
			catch (Throwable ex) {
				// The PropertyEditor might not support this value... pass through.
			}
		}
		return getDisplayString(value, htmlEscape);
	}

}
