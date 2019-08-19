package org.springframework.web.servlet.mvc.method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * {@link HandlerAdapter}实现的抽象基类, 支持{@link HandlerMethod}类型的处理器.
 */
public abstract class AbstractHandlerMethodAdapter extends WebContentGenerator implements HandlerAdapter, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;


	public AbstractHandlerMethodAdapter() {
		// 默认不限制HTTP方法
		super(false);
	}


	/**
	 * 指定此HandlerAdapter bean的顺序值.
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}, 表示无序.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 这个实现期望处理器是{@link HandlerMethod}.
	 * 
	 * @param handler 要检查的处理器实例
	 * 
	 * @return 此适配器是否可以适配给定的处理器
	 */
	@Override
	public final boolean supports(Object handler) {
		return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
	}

	/**
	 * 给定一个处理器方法, 返回此适配器是否可以支持它.
	 * 
	 * @param handlerMethod 要检查的处理器方法
	 * 
	 * @return 此适配器是否可以适配给定的方法
	 */
	protected abstract boolean supportsInternal(HandlerMethod handlerMethod);

	/**
	 * 这个实现期望处理器是{@link HandlerMethod}.
	 */
	@Override
	public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return handleInternal(request, response, (HandlerMethod) handler);
	}

	/**
	 * 使用给定的处理器方法来处理请求.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handlerMethod 要使用的处理器方法.
	 * 此对象必须先前已经过{@link #supportsInternal(HandlerMethod)}验证, 且必须返回{@code true}.
	 * 
	 * @return 包含视图名称和所需的模型数据的ModelAndView对象, 如果请求已直接处理, 则为{@code null}
	 * @throws Exception
	 */
	protected abstract ModelAndView handleInternal(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception;

	/**
	 * 这个实现期望处理器是{@link HandlerMethod}.
	 */
	@Override
	public final long getLastModified(HttpServletRequest request, Object handler) {
		return getLastModifiedInternal(request, (HandlerMethod) handler);
	}

	/**
	 * 与{@link javax.servlet.http.HttpServlet#getLastModified(HttpServletRequest)}相同的约定.
	 * 
	 * @param request 当前的HTTP请求
	 * @param handlerMethod 要使用的处理器方法
	 * 
	 * @return 给定处理器的lastModified值
	 */
	protected abstract long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod);

}
