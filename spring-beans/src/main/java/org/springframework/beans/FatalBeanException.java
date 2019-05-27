package org.springframework.beans;

/**
 * 抛出bean包或子包中遇到的不可恢复的问题, e.g. 无效的类或字段.
 */
@SuppressWarnings("serial")
public class FatalBeanException extends BeansException {

	public FatalBeanException(String msg) {
		super(msg);
	}

	public FatalBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
