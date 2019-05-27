package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.TimeZone;

import org.springframework.util.StringUtils;

/**
 * {@code java.util.TimeZone}的编辑器, 将时区ID转换为{@code TimeZone}对象.
 * 公开{@code TimeZone} ID作为文本表示.
 */
public class TimeZoneEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(StringUtils.parseTimeZoneString(text));
	}

	@Override
	public String getAsText() {
		TimeZone value = (TimeZone) getValue();
		return (value != null ? value.getID() : "");
	}

}
