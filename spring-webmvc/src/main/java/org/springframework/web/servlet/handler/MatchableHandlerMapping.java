package org.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link HandlerMapping}可以实现的其他接口, 用于公开与其内部请求匹配配置和实现对齐的请求匹配API.
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * 确定给定请求是否与请求条件匹配.
	 * 
	 * @param request 当前的请求
	 * @param pattern 要匹配的模式
	 * 
	 * @return 请求匹配的结果, 或{@code null}
	 */
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
