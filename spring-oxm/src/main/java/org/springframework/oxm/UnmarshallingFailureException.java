package org.springframework.oxm;

/**
 * 解组失败时抛出异常.
 */
@SuppressWarnings("serial")
public class UnmarshallingFailureException extends MarshallingException {

	public UnmarshallingFailureException(String msg) {
		super(msg);
	}

	public UnmarshallingFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
