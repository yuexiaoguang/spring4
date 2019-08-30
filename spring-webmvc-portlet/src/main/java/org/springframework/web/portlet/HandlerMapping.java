package org.springframework.web.portlet;

import javax.portlet.PortletRequest;

/**
 * 由定义请求和处理器对象之间的映射的对象实现的接口.
 *
 * <p>这个类可以由应用程序开发人员实现, 虽然这不是必需的, 因为
 * {@link org.springframework.web.portlet.handler.PortletModeHandlerMapping},
 * {@link org.springframework.web.portlet.handler.ParameterHandlerMapping}
 * 和{@link org.springframework.web.portlet.handler.PortletModeParameterHandlerMapping} 包含在框架中.
 * 如果在portlet应用程序上下文中没有注册HandlerMapping bean, 则第一个是缺省值.
 *
 * <p>HandlerMapping实现可以支持映射的拦截器, 但不必如此.
 * 处理器将始终包装在{@link HandlerExecutionChain}实例中, 可选地附带一些{@link HandlerInterceptor}实例.
 * DispatcherPortlet将首先按给定的顺序调用每个HandlerInterceptor的{@code preHandle}方法,
 * 如果所有{@code preHandle}方法都返回{@code true}, 最后调用处理器本身.
 *
 * <p>参数化此映射的能力是此Portlet MVC框架的强大且不寻常的功能.
 * 例如, 可以基于会话状态, cookie状态, 或许多其他变量编写自定义映射.
 * 没有其他MVC框架同样灵活.
 *
 * <p>Note: 实现可以实现{@link org.springframework.core.Ordered}接口,
 * 以便能够指定排序顺序, 从而获得DispatcherPortlet应用的优先级.
 * 非有序实例被视为最低优先级.
 */
public interface HandlerMapping {

	/**
	 * 返回此请求的处理器和拦截器.
	 * 可以对portlet模式, 会话状态, 或实现类选择的任何因素进行选择.
	 * <p>返回的HandlerExecutionChain包含一个处理器Object, 而不是一个标记接口, 因此处理器不会受到任何限制.
	 * 例如, 可以编写HandlerAdapter以允许使用另一个框架的处理器对象.
	 * <p>如果未找到匹配项, 则返回{@code null}. 这不是错误.
	 * DispatcherPortlet将查询所有已注册的HandlerMapping bean以查找匹配项, 并且只有在没有找到处理器时才会确定存在错误.
	 * 
	 * @param request 当前的portlet请求
	 * 
	 * @return 包含处理器对象和拦截器的HandlerExecutionChain实例, 或 null
	 * @throws Exception 如果有内部错误
	 */
	HandlerExecutionChain getHandler(PortletRequest request) throws Exception;

}
