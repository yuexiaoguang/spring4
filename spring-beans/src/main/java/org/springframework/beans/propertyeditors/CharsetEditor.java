package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.nio.charset.Charset;

import org.springframework.util.StringUtils;

/**
 * {@code java.nio.charset.Charset}的编辑器, 将charset字符串转换为Charset对象并返回.
 *
 * <p>期待与Charset的 {@link java.nio.charset.Charset#name()}相同的语法,
 * e.g. {@code UTF-8}, {@code ISO-8859-16}, etc.
 */
public class CharsetEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			setValue(Charset.forName(text));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Charset value = (Charset) getValue();
		return (value != null ? value.name() : "");
	}

}
