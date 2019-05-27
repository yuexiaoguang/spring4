package org.springframework.context;

import java.util.EventObject;

/**
 * 所有应用程序事件都要扩展的类. 抽象, 因为直接发布通用事件没有意义.
 */
public abstract class ApplicationEvent extends EventObject {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = 7099057708183571937L;

	/** 事件发生时的系统时间 */
	private final long timestamp;


	/**
	 * @param source 事件最初发生的对象 (never {@code null})
	 */
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}


	/**
	 * 返回发生事件时的系统时间(以毫秒为单位).
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

}
