package org.springframework.web.portlet.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.portlet.PortletRequest;

import org.springframework.beans.BeansException;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.portlet.HandlerExecutionChain;
import org.springframework.web.portlet.HandlerInterceptor;
import org.springframework.web.portlet.HandlerMapping;

/**
 * {@link org.springframework.web.portlet.HandlerMapping}实现的抽象基类.
 * 支持排序, 默认处理器和处理器拦截器.
 */
public abstract class AbstractHandlerMapping extends ApplicationObjectSupport implements HandlerMapping, Ordered {

	private int order = Integer.MAX_VALUE;  // default: same as non-Ordered

	private Object defaultHandler;

	private final List<Object> interceptors = new ArrayList<Object>();

	private boolean applyWebRequestInterceptorsToRenderPhaseOnly = true;

	private HandlerInterceptor[] adaptedInterceptors;


	/**
	 * 指定此HandlerMapping bean的顺序值.
	 * <p>默认值为{@code Integer.MAX_VALUE}, 表示它是非有序的.
	 */
	public final void setOrder(int order) {
	  this.order = order;
	}

	@Override
	public final int getOrder() {
	  return this.order;
	}

	/**
	 * 设置此处理器映射的默认处理器.
	 * 如果未找到特定映射, 则将返回此处理器.
	 * <p>默认为{@code null}, 表示没有默认处理器.
	 */
	public void setDefaultHandler(Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * 返回此处理器映射的默认处理器, 或{@code null}.
	 */
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * 设置拦截器, 应用于此处理器映射映射的所有处理器.
	 * <p>支持的拦截器类型是HandlerInterceptor和WebRequestInterceptor.
	 * 每个给定的WebRequestInterceptor都将包装在WebRequestHandlerInterceptorAdapter中.
	 * 
	 * @param interceptors 处理器拦截器数组, 或{@code null}
	 */
	public void setInterceptors(Object[] interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * 指定是否仅将WebRequestInterceptors应用于Portlet渲染阶段 ("true"), 或者是否也将它们应用于Portlet操作阶段 ("false").
	 * <p>默认为"true", 因为WebRequestInterceptors通常是为MVC样式的处理器执行和渲染过程而构建的
	 * (例如, "Open Session in View"拦截器的主要目标场景, 在视图渲染期间提供持久对象的延迟加载).
	 * 将此设置为"false"以使WebRequestInterceptors也适用于操作阶段
	 * (例如, 在"Open Session in View"拦截器的情况下, 允许在操作阶段期间在事务外部进行延迟加载).
	 */
	public void setApplyWebRequestInterceptorsToRenderPhaseOnly(boolean applyWebRequestInterceptorsToRenderPhaseOnly) {
		this.applyWebRequestInterceptorsToRenderPhaseOnly = applyWebRequestInterceptorsToRenderPhaseOnly;
	}


	/**
	 * 初始化拦截器.
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		initInterceptors();
	}

	/**
	 * 在给定配置的拦截器的情况下 (see {@link #setInterceptors}), 子类可以重写以扩展其他拦截器的扩展钩子.
	 * <p>将在{@link #initInterceptors()}将指定的拦截器适配为{@link HandlerInterceptor}实例之前调用.
	 * <p>默认实现为空.
	 * 
	 * @param interceptors 配置的拦截器 (never {@code null}), 允许在现有拦截器之前和之后添加更多拦截器
	 */
	protected void extendInterceptors(List<?> interceptors) {
	}

	/**
	 * 初始化指定的拦截器, 在必要时适配它们.
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			this.adaptedInterceptors = new HandlerInterceptor[this.interceptors.size()];
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors[i] = adaptInterceptor(interceptor);
			}
		}
	}

	/**
	 * 将给定的拦截器对象适配为HandlerInterceptor接口.
	 * <p>支持的拦截器类型是HandlerInterceptor和WebRequestInterceptor.
	 * 每个给定的WebRequestInterceptor都将包装在WebRequestHandlerInterceptorAdapter中.
	 * 可以在子类中重写.
	 * 
	 * @param interceptor 指定的拦截器对象
	 * 
	 * @return 包装为HandlerInterceptor的拦截器
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter(
					(WebRequestInterceptor) interceptor, this.applyWebRequestInterceptorsToRenderPhaseOnly);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * 返回适配的拦截器.
	 * 
	 * @return HandlerInterceptor数组, 或{@code null}
	 */
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return this.adaptedInterceptors;
	}


	/**
	 * 查找给定请求的处理器, 如果找不到特定的请求, 则返回默认处理器.
	 * 
	 * @param request 当前的portlet请求
	 * 
	 * @return 相应的处理器实例或默认处理器
	 */
	@Override
	public final HandlerExecutionChain getHandler(PortletRequest request) throws Exception {
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = getApplicationContext().getBean(handlerName);
		}
		return getHandlerExecutionChain(handler, request);
	}

	/**
	 * 查找给定请求的处理器, 如果找不到特定的处理器, 则返回{@code null}.
	 * 这个方法由{@link #getHandler}调用; {@code null}返回值将导致默认处理器.
	 * <p>Note: 此方法还可以返回预构建的{@link HandlerExecutionChain}, 将处理器对象与动态确定的拦截器组合在一起.
	 * 静态指定的拦截器将合并到这样的现有链中.
	 * 
	 * @param request 当前的portlet请求
	 * 
	 * @return 相应的处理器实例, 或{@code null}
	 * @throws Exception 如果有内部错误
	 */
	protected abstract Object getHandlerInternal(PortletRequest request) throws Exception;

	/**
	 * 为给定的处理器构建HandlerExecutionChain, 包括适用的拦截器.
	 * <p>默认实现只是使用给定的处理器和此处理器映射的常见拦截器构建标准的HandlerExecutionChain.
	 * 子类可以覆盖它以扩展/重新排列拦截器列表.
	 * <p><b>NOTE:</b> 传入的处理器对象可以是原始处理器或预构建的HandlerExecutionChain.
	 * 此方法应明确处理这两种情况, 即构建新的HandlerExecutionChain或扩展现有链.
	 * <p>要简单地添加拦截器, 请考虑调用{@code super.getHandlerExecutionChain},
	 * 并在返回的链对象上调用{@link HandlerExecutionChain#addInterceptor}.
	 * 
	 * @param handler 已解析的处理器实例 (never {@code null})
	 * @param request 当前的portlet请求
	 * 
	 * @return the HandlerExecutionChain (never {@code null})
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, PortletRequest request) {
		if (handler instanceof HandlerExecutionChain) {
			HandlerExecutionChain chain = (HandlerExecutionChain) handler;
			chain.addInterceptors(getAdaptedInterceptors());
			return chain;
		}
		else {
			return new HandlerExecutionChain(handler, getAdaptedInterceptors());
		}
	}

}
