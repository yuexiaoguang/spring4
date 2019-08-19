package org.springframework.web.servlet.tags;

/**
 * 允许实现标记使用嵌套的{@code spring:param}标记.
 */
public interface ParamAware {

	/**
	 * 嵌套的spring:param 标记的回调钩子, 将它们的值传递给父标签.
	 * 
	 * @param param 嵌套的{@code spring:param}标记的结果
	 */
	void addParam(Param param);

}
