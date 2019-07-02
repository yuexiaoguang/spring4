package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 提供对已执行请求结果的访问.
 */
public interface MvcResult {

	/**
	 * 返回执行的请求.
	 * 
	 * @return 请求, never {@code null}
	 */
	MockHttpServletRequest getRequest();

	/**
	 * 返回生成的响应.
	 * 
	 * @return 响应, never {@code null}
	 */
	MockHttpServletResponse getResponse();

	/**
	 * 返回执行的处理器.
	 * 
	 * @return 处理器, 或{@code null}
	 */
	Object getHandler();

	/**
	 * 返回处理器周围的拦截器.
	 * 
	 * @return 拦截器, 或{@code null}
	 */
	HandlerInterceptor[] getInterceptors();

	/**
	 * 返回由处理器准备的{@code ModelAndView}.
	 * 
	 * @return a {@code ModelAndView}, or {@code null}
	 */
	ModelAndView getModelAndView();

	/**
	 * 返回由处理器引发的并通过{@link HandlerExceptionResolver}成功解决的任何异常.
	 * 
	 * @return 异常, 可能为{@code null}
	 */
 	Exception getResolvedException();

	/**
	 * 返回请求处理期间保存的"output"闪存属性.
	 * 
	 * @return {@code FlashMap}, 可能为空
	 */
	FlashMap getFlashMap();

	/**
	 * 获取异步执行的结果.
	 * 此方法将等待异步结果最长为配置的时间,
	 * i.e. {@link org.springframework.mock.web.MockAsyncContext#getTimeout()}.
	 * 
	 * @throws IllegalStateException 如果未设置异步结果
	 */
	Object getAsyncResult();

	/**
	 * 获取异步执行的结果.
	 * 此方法将等待异步结果最长为指定的时间.
	 * 
	 * @param timeToWait 等待异步结果的时间, 以毫秒为单位; 如果为-1, 则使用异步请求超时值,
	 *  i.e.{@link org.springframework.mock.web.MockAsyncContext#getTimeout()}.
	 * 
	 * @throws IllegalStateException 如果未设置异步结果
	 */
	Object getAsyncResult(long timeToWait);

}
