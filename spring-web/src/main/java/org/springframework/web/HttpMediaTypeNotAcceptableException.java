package org.springframework.web;

import java.util.List;

import org.springframework.http.MediaType;

/**
 * 当请求处理器无法生成客户端可接受的响应时抛出的异常.
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotAcceptableException extends HttpMediaTypeException {

	public HttpMediaTypeNotAcceptableException(String message) {
		super(message);
	}

	/**
	 * @param supportedMediaTypes 支持的媒体类型列表
	 */
	public HttpMediaTypeNotAcceptableException(List<MediaType> supportedMediaTypes) {
		super("Could not find acceptable representation", supportedMediaTypes);
	}

}
