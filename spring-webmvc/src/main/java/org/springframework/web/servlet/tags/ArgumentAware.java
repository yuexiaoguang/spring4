package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspTagException;

/**
 * 允许实现标记使用嵌套的{@code spring:argument}标记.
 */
public interface ArgumentAware {

	/**
	 * 嵌套的 spring:argument 标记的回调挂钩, 将其值传递给父标记.
	 * 
	 * @param argument 嵌套{@code spring:argument}标记的结果
	 */
	void addArgument(Object argument) throws JspTagException;

}
