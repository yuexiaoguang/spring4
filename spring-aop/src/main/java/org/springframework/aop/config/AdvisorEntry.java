package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;

/**
 * {@link ParseState}条目，表示一个 advisor.
 */
public class AdvisorEntry implements ParseState.Entry {

	private final String name;


	/**
	 * @param name 切面的bean名称
	 */
	public AdvisorEntry(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Advisor '" + this.name + "'";
	}
}
