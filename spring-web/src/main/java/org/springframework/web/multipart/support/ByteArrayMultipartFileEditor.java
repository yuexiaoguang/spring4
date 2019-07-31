package org.springframework.web.multipart.support;

import java.io.IOException;

import org.springframework.beans.propertyeditors.ByteArrayPropertyEditor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 自定义{@link java.beans.PropertyEditor}, 用于将{@link MultipartFile MultipartFiles}转换为字节数组.
 */
public class ByteArrayMultipartFileEditor extends ByteArrayPropertyEditor {

	@Override
	public void setValue(Object value) {
		if (value instanceof MultipartFile) {
			MultipartFile multipartFile = (MultipartFile) value;
			try {
				super.setValue(multipartFile.getBytes());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
			}
		}
		else if (value instanceof byte[]) {
			super.setValue(value);
		}
		else {
			super.setValue(value != null ? value.toString().getBytes() : null);
		}
	}

	@Override
	public String getAsText() {
		byte[] value = (byte[]) getValue();
		return (value != null ? new String(value) : "");
	}

}
