package org.springframework.scheduling;

import org.springframework.core.NestedRuntimeException;

/**
 * 调度失败的常规异常, 例如调度程序已经关闭.
 * 由于调度失败通常是致命的, 因此是未受检的异常.
 */
@SuppressWarnings("serial")
public class SchedulingException extends NestedRuntimeException {

	public SchedulingException(String msg) {
		super(msg);
	}

	/**
	 * @param msg the detail message
	 * @param cause the root cause (通常来自使用底层调度API, 如Quartz)
	 */
	public SchedulingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
