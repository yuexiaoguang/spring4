package org.springframework.web;

import java.util.List;

import org.springframework.http.MediaType;

/**
 * Exception thrown when a client POSTs, PUTs, or PATCHes content of a type
 * not supported by request handler.
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotSupportedException extends HttpMediaTypeException {

	private final MediaType contentType;


	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param message the exception message
	 */
	public HttpMediaTypeNotSupportedException(String message) {
		super(message);
		this.contentType = null;
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param supportedMediaTypes the list of supported media types
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes) {
		this(contentType, supportedMediaTypes, "Content type '" + contentType + "' not supported");
	}

	/**
	 * Create a new HttpMediaTypeNotSupportedException.
	 * @param contentType the unsupported content type
	 * @param supportedMediaTypes the list of supported media types
	 * @param msg the detail message
	 */
	public HttpMediaTypeNotSupportedException(MediaType contentType, List<MediaType> supportedMediaTypes, String msg) {
		super(msg, supportedMediaTypes);
		this.contentType = contentType;
	}


	/**
	 * Return the HTTP request content type method that caused the failure.
	 */
	public MediaType getContentType() {
		return this.contentType;
	}

}
