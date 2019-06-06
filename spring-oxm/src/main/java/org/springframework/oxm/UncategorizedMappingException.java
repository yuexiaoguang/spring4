package org.springframework.oxm;

/**
 * 表示无法进一步区分原因的异常.
 */
@SuppressWarnings("serial")
public class UncategorizedMappingException extends XmlMappingException {

	public UncategorizedMappingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
