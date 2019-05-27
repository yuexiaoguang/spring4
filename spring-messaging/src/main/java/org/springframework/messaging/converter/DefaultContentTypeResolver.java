package org.springframework.messaging.converter;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * 默认{@link ContentTypeResolver}, 用于检查{@link MessageHeaders#CONTENT_TYPE} header或回退到默认值.
 *
 * <p>header值应为{@link org.springframework.util.MimeType}或{@code String}, 可以解析为{@code MimeType}.
 */
public class DefaultContentTypeResolver implements ContentTypeResolver {

	private MimeType defaultMimeType;


	/**
	 * 设置当没有{@link MessageHeaders#CONTENT_TYPE} header时使用的默认MIME类型.
	 * <p>此属性没有默认值.
	 */
	public void setDefaultMimeType(MimeType defaultMimeType) {
		this.defaultMimeType = defaultMimeType;
	}

	/**
	 * 如果不存在{@link MessageHeaders#CONTENT_TYPE} header, 返回要使用的默认MIME类型.
	 */
	public MimeType getDefaultMimeType() {
		return this.defaultMimeType;
	}


	@Override
	public MimeType resolve(MessageHeaders headers) {
		if (headers == null || headers.get(MessageHeaders.CONTENT_TYPE) == null) {
			return this.defaultMimeType;
		}
		Object value = headers.get(MessageHeaders.CONTENT_TYPE);
		if (value instanceof MimeType) {
			return (MimeType) value;
		}
		else if (value instanceof String) {
			return MimeType.valueOf((String) value);
		}
		else {
			throw new IllegalArgumentException(
					"Unknown type for contentType header value: " + value.getClass());
		}
	}

	@Override
	public String toString() {
		return "DefaultContentTypeResolver[" + "defaultMimeType=" + this.defaultMimeType + "]";
	}

}
