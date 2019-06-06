package org.springframework.oxm;

/**
 * 编组验证失败时抛出异常.
 */
@SuppressWarnings("serial")
public class ValidationFailureException extends XmlMappingException {

	public ValidationFailureException(String msg) {
		super(msg);
	}

	public ValidationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
