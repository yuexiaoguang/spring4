package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

/**
 * char数组的编辑器. 字符串将简单地转换为其对应的char表示形式.
 */
public class CharArrayPropertyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		setValue(text != null ? text.toCharArray() : null);
	}

	@Override
	public String getAsText() {
		char[] value = (char[]) getValue();
		return (value != null ? new String(value) : "");
	}

}
