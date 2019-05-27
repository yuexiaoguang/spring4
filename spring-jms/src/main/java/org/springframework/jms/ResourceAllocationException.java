package org.springframework.jms;

/**
 * 镜像JMS ResourceAllocationException的运行时异常.
 */
@SuppressWarnings("serial")
public class ResourceAllocationException extends JmsException {

	public ResourceAllocationException(javax.jms.ResourceAllocationException cause) {
		super(cause);
	}

}
