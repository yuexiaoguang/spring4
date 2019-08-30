package org.springframework.web.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * Portlet MVC框架SPI接口, 允许核心MVC工作流的参数化.
 *
 * <p>必须为每个处理器类型实现的接口, 以处理请求.
 * 此接口用于允许DispatcherPortlet无限扩展.
 * DispatcherPortlet通过此接口访问所有已安装的处理器, 这意味着它不包含特定于任何处理器类型的代码.
 *
 * <p>请注意, 处理器可以是Object类型. 这是为了使其他框架的处理器能够与此框架集成, 而无需自定义编码.
 *
 * <p>此接口不适用于应用程序开发人员. 它适用于想要开发自己的Web工作流程的处理器.
 *
 * <p>Note: 实现可以实现Ordered接口, 以便能够指定排序顺序, 从而获得DispatcherPortlet应用的优先级.
 * 非有序实例被视为最低优先级.
 */
public interface HandlerAdapter {

	/**
	 * 给定一个处理器实例, 返回此HandlerAdapter是否可以支持它.
	 * 典型的HandlerAdapters将根据处理器类型做出决定.
	 * HandlerAdapters通常每个只支持一种处理器类型.
	 * <p>典型的实现:
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 * 
	 * @param handler 要检查的处理器对象
	 * 
	 * @return 此对象是否可以使用给定的处理器
	 */
	boolean supports(Object handler);

	/**
	 * 使用给定的处理器来处理此操作请求.
	 * 所需的工作流程可能差异很大.
	 * 
	 * @param request 当前的操作请求
	 * @param response 当前的操作响应
	 * @param handler 要使用的处理器.
	 * 此对象必须先前已传递到此接口的{@code supports}方法, 该方法必须返回true.
	 * 
	 * @throws Exception
	 */
	void handleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception;

	/**
	 * 使用给定的处理器来处理此渲染请求.
	 * 所需的工作流程可能差异很大.
	 * 
	 * @param request 当前的渲染请求
	 * @param response 当前的渲染响应
	 * @param handler 要使用的处理器.
	 * 此对象必须先前已传递到此接口的{@code supports}方法, 该方法必须返回true.
	 * 
	 * @return 包含视图名称和所需的模型数据的 ModelAndView 对象, 或{@code null} 如果请求已直接处理
	 * @throws Exception
	 */
	ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception;

	/**
	 * 使用给定的处理器来处理此资源请求.
	 * 所需的工作流程可能差异很大.
	 * 
	 * @param request 当前的渲染请求
	 * @param response 当前的渲染响应
	 * @param handler 要使用的处理器.
	 * 此对象必须先前已传递到此接口的{@code supports}方法, 该方法必须返回true.
	 * 
	 * @return 包含视图名称和所需的模型数据的 ModelAndView 对象, 或{@code null} 如果请求已直接处理
	 * @throws Exception
	 */
	ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler) throws Exception;

	/**
	 * 使用给定的处理器来处理此事件请求.
	 * 所需的工作流程可能差异很大.
	 * 
	 * @param request 当前的操作请求
	 * @param response 当前的操作响应
	 * @param handler 要使用的处理器.
	 * 此对象必须先前已传递到此接口的{@code supports}方法, 该方法必须返回true.
	 * 
	 * @throws Exception
	 */
	void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception;

}
