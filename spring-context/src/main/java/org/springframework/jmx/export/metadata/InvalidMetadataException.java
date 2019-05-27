package org.springframework.jmx.export.metadata;

import org.springframework.jmx.JmxException;

/**
 * {@code JmxAttributeSource}在遇到托管资源或其某个方法上的错误元数据时抛出.
 */
@SuppressWarnings("serial")
public class InvalidMetadataException extends JmxException {

	public InvalidMetadataException(String msg) {
		super(msg);
	}

}
