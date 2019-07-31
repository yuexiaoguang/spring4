package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.WebRequest;

/**
 * 用于初始化{@link org.springframework.web.bind.WebDataBinder}的回调接口,
 * 用于在特定Web请求的上下文中执行数据绑定.
 */
public interface WebBindingInitializer {

	/**
	 * 为给定的请求初始化给定的DataBinder.
	 * 
	 * @param binder 要初始化的DataBinder
	 * @param request 发生数据绑定的Web请求
	 */
	void initBinder(WebDataBinder binder, WebRequest request);

}
