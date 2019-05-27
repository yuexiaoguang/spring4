package org.springframework.jmx;

/**
 * 当找不到{@code MBeanServer}的实例时, 或者找到多个实例时抛出异常.
 */
@SuppressWarnings("serial")
public class MBeanServerNotFoundException extends JmxException {

	public MBeanServerNotFoundException(String msg) {
		super(msg);
	}

	public MBeanServerNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
