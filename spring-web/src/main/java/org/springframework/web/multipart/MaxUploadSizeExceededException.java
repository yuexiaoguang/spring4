package org.springframework.web.multipart;

/**
 * 当上传超过允许的最大上传大小时, 抛出的MultipartException子类.
 */
@SuppressWarnings("serial")
public class MaxUploadSizeExceededException extends MultipartException {

	private final long maxUploadSize;


	/**
	 * @param maxUploadSize 允许的最大上传大小
	 */
	public MaxUploadSizeExceededException(long maxUploadSize) {
		this(maxUploadSize, null);
	}

	/**
	 * @param maxUploadSize 允许的最大上传大小
	 * @param ex 使用multipart解析API的根本原因
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
