package org.springframework.web.servlet.mvc.condition;

import javax.servlet.http.HttpServletRequest;

/**
 * 请求映射条件的约定.
 *
 * <p>请求条件可以通过{@link #combine(Object)}组合, 通过{@link #getMatchingCondition(HttpServletRequest)}与请求匹配,
 * 并通过{@link #compareTo(Object, HttpServletRequest)}相互比较来确定哪个与给定请求的更匹配.
 *
 * @param <T> 此RequestCondition可以与之组合并进行比较的对象类型
 */
public interface RequestCondition<T> {

	/**
	 * 将此条件与类型级别和方法级别{@code @RequestMapping}注解中的条件相结合.
	 * 
	 * @param other 要结合的条件.
	 * 
	 * @return 请求条件实例, 它是两个条件实例组合的结果.
	 */
	T combine(T other);

	/**
	 * 检查条件是否与请求匹配, 返回可能为当前请求创建的新实例.
	 * 例如, 具有多个URL模式的条件可能仅返回与该请求匹配的那些模式的新实例.
	 * <p>对于CORS pre-flight请求, 条件应与可能的实际请求匹配
	 * (e.g. URL模式, 查询参数, 和来自"Access-Control-Request-Method" header的HTTP方法).
	 * 如果条件无法与pre-flight请求匹配, 则应返回具有空内容的实例, 从而不会导致匹配失败.
	 * 
	 * @return 匹配时的条件实例, 否则为{@code null}.
	 */
	T getMatchingCondition(HttpServletRequest request);

	/**
	 * 将此条件与特定请求的上下文中的另一个条件进行比较.
	 * 此方法假设已通过{@link #getMatchingCondition(HttpServletRequest)}获取这两个实例,
	 * 以确保它们仅具有与当前请求相关的内容.
	 */
	int compareTo(T other, HttpServletRequest request);

}
