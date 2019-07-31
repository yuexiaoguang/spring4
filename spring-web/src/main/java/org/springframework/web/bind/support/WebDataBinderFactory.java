package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于为命名目标对象创建{@link WebDataBinder}实例的工厂.
 */
public interface WebDataBinderFactory {

	/**
	 * 为给定对象创建{@link WebDataBinder}.
	 * 
	 * @param webRequest 当前请求
	 * @param target 创建数据绑定器的对象, 或{@code null}, 如果为简单类型创建绑定器
	 * @param objectName 目标对象的名称
	 * 
	 * @return 创建的{@link WebDataBinder}实例, 不会是null
	 * @throws Exception 如果数据绑定器的创建和初始化失败
	 */
	WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception;

}
