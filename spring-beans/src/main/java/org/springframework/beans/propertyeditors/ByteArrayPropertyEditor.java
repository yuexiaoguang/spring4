package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

/**
 * byte数组的编辑器. 字符串将简单地转换为其对应的byte.
 */
public class ByteArrayPropertyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		setValue(text != null ? text.getBytes() : null);
	}

	@Override
	public String getAsText() {
		byte[] value = (byte[]) getValue();
		return (value != null ? new String(value) : "");
	}

}
