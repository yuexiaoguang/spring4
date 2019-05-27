package org.springframework.beans.factory.parsing;

import org.springframework.util.StringUtils;

/**
 * 表示autowire候选限定符的{@link ParseState}条目.
 */
public class QualifierEntry implements ParseState.Entry {

	private String typeName;


	public QualifierEntry(String typeName) {
		if (!StringUtils.hasText(typeName)) {
			throw new IllegalArgumentException("Invalid qualifier type '" + typeName + "'.");
		}
		this.typeName = typeName;
	}

	@Override
	public String toString() {
		return "Qualifier '" + this.typeName + "'";
	}

}
