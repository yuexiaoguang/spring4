package org.springframework.beans.factory.parsing;

import org.springframework.util.StringUtils;

/**
 * 表示JavaBean属性的{@link ParseState}条目.
 */
public class PropertyEntry implements ParseState.Entry {

	private final String name;


	/**
	 * @param name 此实例表示的JavaBean属性的名称
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code name}是{@code null}或完全由空格组成
	 */
	public PropertyEntry(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Invalid property name '" + name + "'.");
		}
		this.name = name;
	}


	@Override
	public String toString() {
		return "Property '" + this.name + "'";
	}

}
