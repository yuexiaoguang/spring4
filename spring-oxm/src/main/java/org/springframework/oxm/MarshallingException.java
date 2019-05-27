package org.springframework.oxm;

/**
 * Base class for exception thrown when a marshalling or unmarshalling error occurs.
 */
@SuppressWarnings("serial")
public abstract class MarshallingException extends XmlMappingException {

	/**
	 * Construct a {@code MarshallingException} with the specified detail message.
	 * @param msg the detail message
	 */
	protected MarshallingException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code MarshallingException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	protected MarshallingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
