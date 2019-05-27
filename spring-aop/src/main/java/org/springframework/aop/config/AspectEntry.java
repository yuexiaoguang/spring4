package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;
import org.springframework.util.StringUtils;

/**
 * {@link ParseState}条目, 表示一个切面.
 */
public class AspectEntry implements ParseState.Entry {

	private final String id;

	private final String ref;


	/**
	 * @param id 切面元素的id
	 * @param ref 此切面元素引用的bean名称
	 */
	public AspectEntry(String id, String ref) {
		this.id = id;
		this.ref = ref;
	}

	@Override
	public String toString() {
		return "Aspect: " + (StringUtils.hasLength(this.id) ? "id='" + this.id + "'" : "ref='" + this.ref + "'");
	}
}
