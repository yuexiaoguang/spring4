package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;

/**
 * {@link ParseState}条目, 表示一个切点.
 */
public class PointcutEntry implements ParseState.Entry {

	private final String name;

	/**
	 * @param name 切点的bean名称
	 */
	public PointcutEntry(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Pointcut '" + this.name + "'";
	}
}
