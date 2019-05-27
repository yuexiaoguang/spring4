package org.springframework.web.multipart;

/**
 * MultipartException subclass thrown when an upload exceeds the
 * maximum upload size allowed.
 */
@SuppressWarnings("serial")
public class MaxUploadSizeExceededException extends MultipartException {

	private final long maxUploadSize;


	/**
	 * Constructor for MaxUploadSizeExceededException.
	 * @param maxUploadSize the maximum upload size allowed
	 */
	public MaxUploadSizeExceededException(long maxUploadSize) {
		this(maxUploadSize, null);
	}

	/**
	 * Constructor for MaxUploadSizeExceededException.
	 * @param maxUploadSize the maximum upload size allowed
	 * @param ex root cause from multipart parsing API in use
	 */
	public MaxUploadSizeExceededException(long maxUploadSize, Throwable ex) {
		super("Maximum upload size of " + maxUploadSize + " bytes exceeded", ex);
		this.maxUploadSize = maxUploadSize;
	}


	/**
	 * Return the maximum upload size allowed.
	 */
	public long getMaxUploadSize() {
		return this.maxUploadSize;
	}

}
