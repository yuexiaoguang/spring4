package org.springframework.web.multipart.support;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

/**
 * 自定义{@link java.beans.PropertyEditor}, 用于将{@link MultipartFile MultipartFiles}转换为字符串.
 *
 * <p>允许用户指定要使用的字符集.
 */
public class StringMultipartFileEditor extends PropertyEditorSupport {

	private final String charsetName;


	/**
	 * 使用默认的字符集.
	 */
	public StringMultipartFileEditor() {
		this.charsetName = null;
	}

	/**
	 * @param charsetName 有效的charset名称
	 */
	public StringMultipartFileEditor(String charsetName) {
		this.charsetName = charsetName;
	}


	@Override
	public void setAsText(String text) {
		setValue(text);
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof MultipartFile) {
			MultipartFile multipartFile = (MultipartFile) value;
			try {
				super.setValue(this.charsetName != null ?
						new String(multipartFile.getBytes(), this.charsetName) :
						new String(multipartFile.getBytes()));
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
			}
		}
		else {
			super.setValue(value);
		}
	}

}
