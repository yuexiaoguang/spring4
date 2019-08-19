package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * {@link HandlerExceptionResolver}, 委托给其他{@link HandlerExceptionResolver}的列表.
 */
public class HandlerExceptionResolverComposite implements HandlerExceptionResolver, Ordered {

	private List<HandlerExceptionResolver> resolvers;

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 设置要委托给的异常解析器列表.
	 */
	public void setExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.resolvers = exceptionResolvers;
	}

	/**
	 * 返回要委托给的异常解析器列表.
	 */
	public List<HandlerExceptionResolver> getExceptionResolvers() {
		return (this.resolvers != null ? Collections.unmodifiableList(this.resolvers) :
				Collections.<HandlerExceptionResolver>emptyList());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 通过迭代配置的异常解析器列表来解析异常.
	 * 返回ModelAndView实例的第一个获胜. 否则返回{@code null}.
	 */
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
			Object handler,Exception ex) {

		if (this.resolvers != null) {
			for (HandlerExceptionResolver handlerExceptionResolver : this.resolvers) {
				ModelAndView mav = handlerExceptionResolver.resolveException(request, response, handler, ex);
				if (mav != null) {
					return mav;
				}
			}
		}
		return null;
	}

}
