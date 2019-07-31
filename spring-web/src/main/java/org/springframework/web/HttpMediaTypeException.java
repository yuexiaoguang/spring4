package org.springframework.web;

import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;

import org.springframework.http.MediaType;

/**
 * 与媒体类型相关的异常的抽象. 添加支持的{@link MediaType MediaTypes}列表.
 */
@SuppressWarnings("serial")
public abstract class HttpMediaTypeException extends ServletException {

	private final List<MediaType> supportedMediaTypes;


	protected HttpMediaTypeException(String message) {
		super(message);
		this.supportedMediaTypes = Collections.emptyList();
	}

	/**
	 * @param supportedMediaTypes 支持的媒体类型列表
	 */
	protected HttpMediaTypeException(String message, List<MediaType> supportedMediaTypes) {
		super(message);
		this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
	}


	/**
	 * 返回支持的媒体类型列表.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.supportedMediaTypes;
	}

}
