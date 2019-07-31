package org.springframework.http;

import org.springframework.util.InvalidMimeTypeException;

/**
 * 遇到无效媒体类型规范字符串时, {@link MediaType#parseMediaType(String)}抛出的异常.
 */
@SuppressWarnings("serial")
public class InvalidMediaTypeException extends IllegalArgumentException {

	private String mediaType;


	/**
	 * @param mediaType 有问题的媒体类型
	 * @param message 详细信息
	 */
	public InvalidMediaTypeException(String mediaType, String message) {
		super("Invalid media type \"" + mediaType + "\": " + message);
		this.mediaType = mediaType;
	}

	/**
	 * 包装{@link InvalidMimeTypeException}.
	 */
	InvalidMediaTypeException(InvalidMimeTypeException ex) {
		super(ex.getMessage(), ex);
		this.mediaType = ex.getMimeType();
	}


	/**
	 * 返回有问题的媒体类型.
	 */
	public String getMediaType() {
		return this.mediaType;
	}

}
