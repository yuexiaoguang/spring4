package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 媒体类型表达式的约定 (e.g. "text/plain", "!text/plain"),
 * 如{@code @RequestMapping}注解中所定义的"consumes" 和 "produces"条件.
 */
public interface MediaTypeExpression {

	MediaType getMediaType();

	boolean isNegated();

}
