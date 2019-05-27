package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;

/**
 * {@link ParseState}条目, 表示一个增强元素.
 */
public class AdviceEntry implements ParseState.Entry {

	private final String kind;


	/**
	 * @param kind 本条目所代表的增强类型 (before, after, around, etc.)
	 */
	public AdviceEntry(String kind) {
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "Advice (" + this.kind + ")";
	}
}
