package org.springframework.util;

/**
 * 遇到无效内容类型规范String时, {@link MimeTypeUtils#parseMimeType(String)}抛出异常.
 */
@SuppressWarnings("serial")
public class InvalidMimeTypeException extends IllegalArgumentException {

	private final String mimeType;


	/**
	 * @param mimeType 违规的媒体类型
	 * @param message 指示无效部分的详细消息
	 */
	public InvalidMimeTypeException(String mimeType, String message) {
		super("Invalid mime type \"" + mimeType + "\": " + message);
		this.mimeType = mimeType;
	}


	/**
	 * 返回违规的内容类型.
	 */
	public String getMimeType() {
		return this.mimeType;
	}
}
