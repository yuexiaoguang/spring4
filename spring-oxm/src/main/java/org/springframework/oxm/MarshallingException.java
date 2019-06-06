package org.springframework.oxm;

/**
 * 发生编组或解组错误时抛出异常的基类.
 */
@SuppressWarnings("serial")
public abstract class MarshallingException extends XmlMappingException {

	protected MarshallingException(String msg) {
		super(msg);
	}

	protected MarshallingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
