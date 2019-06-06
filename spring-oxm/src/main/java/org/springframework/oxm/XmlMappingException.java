package org.springframework.oxm;

import org.springframework.core.NestedRuntimeException;

/**
 * 对象XML映射异常的层次结构的根.
 */
@SuppressWarnings("serial")
public abstract class XmlMappingException extends NestedRuntimeException {

	public XmlMappingException(String msg) {
		super(msg);
	}

	public XmlMappingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
