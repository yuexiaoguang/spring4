package org.springframework.oxm;

/**
 * 编组失败引发的异常.
 */
@SuppressWarnings("serial")
public class MarshallingFailureException extends MarshallingException {

	public MarshallingFailureException(String msg) {
		super(msg);
	}

	public MarshallingFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
