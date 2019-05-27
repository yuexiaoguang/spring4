package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.time.ZoneId;

import org.springframework.lang.UsesJava8;

/**
 * {@code java.time.ZoneId}的编辑器, 将区域ID字符串转换为{@code ZoneId}对象.
 * 公开{@code TimeZone} ID作为文本.
 */
@UsesJava8
public class ZoneIdEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(ZoneId.of(text));
	}

	@Override
	public String getAsText() {
		ZoneId value = (ZoneId) getValue();
		return (value != null ? value.getId() : "");
	}

}
