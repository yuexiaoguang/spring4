package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.UUID;

import org.springframework.util.StringUtils;

/**
 * {@code java.util.UUID}的编辑器, 将UUID字符串转换为UUID对象并返回.
 */
public class UUIDEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			setValue(UUID.fromString(text));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		UUID value = (UUID) getValue();
		return (value != null ? value.toString() : "");
	}

}
