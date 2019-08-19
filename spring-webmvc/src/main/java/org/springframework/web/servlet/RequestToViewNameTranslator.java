package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * 策略接口, 用于在未显式提供视图名称时,
 * 将传入的{@link javax.servlet.http.HttpServletRequest}转换为逻辑视图名称.
 */
public interface RequestToViewNameTranslator {

	/**
	 * 将给定的{@link HttpServletRequest}转换为视图名称.
	 * 
	 * @param request 传入的{@link HttpServletRequest}提供要从中解析视图名称的上下文
	 * 
	 * @return 视图名称 (如果未找到默认值, 则为{@code null})
	 * @throws Exception 如果视图名称转换失败
	 */
	String getViewName(HttpServletRequest request) throws Exception;

}
