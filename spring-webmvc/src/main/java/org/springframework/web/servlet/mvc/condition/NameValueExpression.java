package org.springframework.web.servlet.mvc.condition;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * {@code "name!=value"}样式表达式的约定, 用于在{@code @RequestMapping}中指定请求参数和请求header条件.
 */
public interface NameValueExpression<T> {

	String getName();

	T getValue();

	boolean isNegated();

}
