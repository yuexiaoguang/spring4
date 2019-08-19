package org.springframework.web.servlet;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 处理器执行链, 由处理器对象和任何处理器拦截器组成.
 * 由HandlerMapping的{@link HandlerMapping#getHandler}方法返回.
 */
public class HandlerExecutionChain {

	private static final Log logger = LogFactory.getLog(HandlerExecutionChain.class);

	private final Object handler;

	private HandlerInterceptor[] interceptors;

	private List<HandlerInterceptor> interceptorList;

	private int interceptorIndex = -1;


	/**
	 * @param handler 要执行的处理器对象
	 */
	public HandlerExecutionChain(Object handler) {
		this(handler, (HandlerInterceptor[]) null);
	}

	/**
	 * @param handler 要执行的处理器对象
	 * @param interceptors 要在处理器本身执行之前, (按给定顺序)应用的拦截器数组
	 */
	public HandlerExecutionChain(Object handler, HandlerInterceptor... interceptors) {
		if (handler instanceof HandlerExecutionChain) {
			HandlerExecutionChain originalChain = (HandlerExecutionChain) handler;
			this.handler = originalChain.getHandler();
			this.interceptorList = new ArrayList<HandlerInterceptor>();
			CollectionUtils.mergeArrayIntoCollection(originalChain.getInterceptors(), this.interceptorList);
			CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
		}
		else {
			this.handler = handler;
			this.interceptors = interceptors;
		}
	}


	/**
	 * 返回要执行的处理器对象.
	 * 
	 * @return 处理器对象 (may be {@code null})
	 */
	public Object getHandler() {
		return this.handler;
	}

	public void addInterceptor(HandlerInterceptor interceptor) {
		initInterceptorList().add(interceptor);
	}

	public void addInterceptors(HandlerInterceptor... interceptors) {
		if (!ObjectUtils.isEmpty(interceptors)) {
			CollectionUtils.mergeArrayIntoCollection(interceptors, initInterceptorList());
		}
	}

	private List<HandlerInterceptor> initInterceptorList() {
		if (this.interceptorList == null) {
			this.interceptorList = new ArrayList<HandlerInterceptor>();
			if (this.interceptors != null) {
				// 通过构造函数指定的拦截器数组
				CollectionUtils.mergeArrayIntoCollection(this.interceptors, this.interceptorList);
			}
		}
		this.interceptors = null;
		return this.interceptorList;
	}

	/**
	 * 返回要应用的拦截器数组 (按给定顺序).
	 * 
	 * @return HandlerInterceptor实例数组 (may be {@code null})
	 */
	public HandlerInterceptor[] getInterceptors() {
		if (this.interceptors == null && this.interceptorList != null) {
			this.interceptors = this.interceptorList.toArray(new HandlerInterceptor[this.interceptorList.size()]);
		}
		return this.interceptors;
	}


	/**
	 * 要应用的注册拦截器的preHandle方法.
	 * 
	 * @return {@code true} 如果执行链应该继续下一个拦截器或处理器本身.
	 * 否则, DispatcherServlet假定这个拦截器已经处理了响应本身.
	 */
	boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			for (int i = 0; i < interceptors.length; i++) {
				HandlerInterceptor interceptor = interceptors[i];
				if (!interceptor.preHandle(request, response, this.handler)) {
					triggerAfterCompletion(request, response, null);
					return false;
				}
				this.interceptorIndex = i;
			}
		}
		return true;
	}

	/**
	 * 应用注册的拦截器的postHandle方法.
	 */
	void applyPostHandle(HttpServletRequest request, HttpServletResponse response, ModelAndView mv) throws Exception {
		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			for (int i = interceptors.length - 1; i >= 0; i--) {
				HandlerInterceptor interceptor = interceptors[i];
				interceptor.postHandle(request, response, this.handler, mv);
			}
		}
	}

	/**
	 * 在映射的HandlerInterceptors上触发afterCompletion回调.
	 * 只需为preHandle调用成功完成并返回true的所有拦截器调用afterCompletion.
	 */
	void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex)
			throws Exception {

		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			for (int i = this.interceptorIndex; i >= 0; i--) {
				HandlerInterceptor interceptor = interceptors[i];
				try {
					interceptor.afterCompletion(request, response, this.handler, ex);
				}
				catch (Throwable ex2) {
					logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
				}
			}
		}
	}

	/**
	 * 在映射的AsyncHandlerInterceptors上应用afterConcurrentHandlerStarted回调.
	 */
	void applyAfterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response) {
		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			for (int i = interceptors.length - 1; i >= 0; i--) {
				if (interceptors[i] instanceof AsyncHandlerInterceptor) {
					try {
						AsyncHandlerInterceptor asyncInterceptor = (AsyncHandlerInterceptor) interceptors[i];
						asyncInterceptor.afterConcurrentHandlingStarted(request, response, this.handler);
					}
					catch (Throwable ex) {
						logger.error("Interceptor [" + interceptors[i] + "] failed in afterConcurrentHandlingStarted", ex);
					}
				}
			}
		}
	}


	/**
	 * 委托给处理器的{@code toString()}.
	 */
	@Override
	public String toString() {
		Object handler = getHandler();
		if (handler == null) {
			return "HandlerExecutionChain with no handler";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("HandlerExecutionChain with handler [").append(handler).append("]");
		HandlerInterceptor[] interceptors = getInterceptors();
		if (!ObjectUtils.isEmpty(interceptors)) {
			sb.append(" and ").append(interceptors.length).append(" interceptor");
			if (interceptors.length > 1) {
				sb.append("s");
			}
		}
		return sb.toString();
	}

}
