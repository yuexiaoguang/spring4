package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspTagException;

/**
 * Allows implementing tag to utilize nested {@code spring:argument} tags.
 */
public interface ArgumentAware {

	/**
	 * Callback hook for nested spring:argument tags to pass their value
	 * to the parent tag.
	 * @param argument the result of the nested {@code spring:argument} tag
	 */
	void addArgument(Object argument) throws JspTagException;

}
