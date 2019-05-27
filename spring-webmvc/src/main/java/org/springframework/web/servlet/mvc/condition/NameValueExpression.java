package org.springframework.web.servlet.mvc.condition;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A contract for {@code "name!=value"} style expression used to specify request
 * parameters and request header conditions in {@code @RequestMapping}.
 */
public interface NameValueExpression<T> {

	String getName();

	T getValue();

	boolean isNegated();

}
