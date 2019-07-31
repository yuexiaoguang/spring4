package org.springframework.web;

import java.util.List;

import org.springframework.http.MediaType;

/**
 * 当请求处理器不支持客户端POST, PUT或PATCH内容的类型时抛出的异常.
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotSupportedException extends HttpMediaTypeException {

	private final MediaType contentType;


	public HttpMediaTypeNotSupportedException(String message) {
		super(message);
		this.contentType = null;
	}

	/**
	 * @param contentType 不支持的内容类型
	 * @param supportedMediaTypes 支持的媒体类型列表
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes) {
		this(contentType, supportedMediaTypes, "Content type '" + contentType + "' not supported");
	}

	/**
	 * @param contentType 不支持的内容类型
	 * @param supportedMediaTypes 支持的媒体类型列表
	 * @param msg 详细信息
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes, String msg) {
		super(msg, supportedMediaTypes);
		this.contentType = contentType;
	}


	/**
	 * 返回导致失败的HTTP请求内容类型方法.
	 */
	public MediaType getContentType() {
		return this.contentType;
	}
}
