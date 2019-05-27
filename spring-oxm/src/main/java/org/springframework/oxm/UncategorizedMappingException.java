package org.springframework.oxm;

/**
 * Exception that indicates that the cause cannot be distinguished further.
 */
@SuppressWarnings("serial")
public class UncategorizedMappingException extends XmlMappingException {

	/**
	 * Construct an {@code UncategorizedMappingException} with the specified detail message
	 * and nested exception.
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public UncategorizedMappingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
