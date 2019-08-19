package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用于检索和保存FlashMap实例的策略接口.
 * 有关Flash属性的一般概述, 请参阅{@link FlashMap}.
 */
public interface FlashMapManager {

	/**
	 * 查找由与当前请求匹配的先前请求保存的FlashMap, 将其从底层存储中删除, 还删除其他过期的FlashMap实例.
	 * <p>与{@link #saveOutputFlashMap}相反, 在每个请求的开头调用此方法,
	 * 只有在要保存Flash属性时(即重定向之前)才会调用此方法.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * 
	 * @return 与当前请求匹配的FlashMap或{@code null}
	 */
	FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response);

	/**
	 * 将给定的FlashMap保存在某个底层存储中, 并设置其有效期的开始.
	 * <p><strong>NOTE:</strong> 在重定向之前调用此方法,
	 * 以便在提交响应之前允许将FlashMap保存在HTTP会话或响应cookie中.
	 * 
	 * @param flashMap 要保存的FlashMap
	 * @param request 当前的请求
	 * @param response 当前的响应
	 */
	void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response);

}
