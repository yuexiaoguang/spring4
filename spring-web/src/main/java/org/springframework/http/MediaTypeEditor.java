package org.springframework.http;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * {@link MediaType}描述符的{@link java.beans.PropertyEditor Editor},
 * 自动将{@code String}规范 (e.g. {@code "text/html"})转换为{@code MediaType}属性.
 */
public class MediaTypeEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			setValue(MediaType.parseMediaType(text));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		MediaType mediaType = (MediaType) getValue();
		return (mediaType != null ? mediaType.toString() : "");
	}

}
