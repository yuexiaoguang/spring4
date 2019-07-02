package org.springframework.test.web.servlet;

import org.springframework.web.servlet.DispatcherServlet;

/**
 * 用于自定义由{@link MockMvc}管理的{@link DispatcherServlet}实例的策略接口.
 */
public interface DispatcherServletCustomizer {

	/**
	 * 在初始化之前自定义提供的{@link DispatcherServlet}.
	 * 
	 * @param dispatcherServlet 要自定义的调度servlet
	 */
	void customize(DispatcherServlet dispatcherServlet);

}
