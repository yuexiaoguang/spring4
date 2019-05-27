package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * {@code java.util.Locale}的编辑器, 直接填充Locale属性.
 *
 * <p>期望与Locale的{@code toString}语法相同, i.e. language + optionally country + optionally variant, 由 "_"分隔 (e.g. "en", "en_US").
 * 还接受空格作为分隔符, 作为下划线的替代.
 */
public class LocaleEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		setValue(StringUtils.parseLocaleString(text));
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}

}
