package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

/**
 * 基础Controller接口, 表示接收{@code HttpServletRequest}和{@code HttpServletResponse}实例的组件,
 * 就像{@code HttpServlet}一样, 但能够参与MVC工作流程.
 * 控制器可与Struts {@code Action}的概念相媲美.
 *
 * <p>Controller接口的任何实现都应该是一个<i>可重用, 线程安全的</i>类, 能够在应用程序的整个生命周期中处理多个HTTP请求.
 * 为了能够轻松配置Controller, 鼓励Controller实现 (通常是) JavaBeans.
 *
 * <h3><a name="workflow">Workflow</a></h3>
 *
 * <p>在{@code DispatcherServlet}收到请求并完成其解析区域设置, 主题等工作之后, 它会尝试使用
 * {@link org.springframework.web.servlet.HandlerMapping HandlerMapping}解析Controller.
 * 当发现Controller处理请求时, 将调用定位的Controller的
 * {@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest}方法;
 * 然后, 定位的Controller负责处理实际的请求并且 &mdash;
 * 如果适用, 返回一个合适的{@link org.springframework.web.servlet.ModelAndView ModelAndView}.
 * 实际上, 这个方法是{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}的主要入口点,
 * 它将请求委托给控制器.
 *
 * <p>所以基本上{@code Controller}接口的任何<i>直接</i>实现都只处理HttpServletRequests,
 * 并且应该返回一个ModelAndView, 由DispatcherServlet进一步解释.
 * 任何其他功能, 如可选验证, 表单处理等, 都应该通过扩展
 * {@link org.springframework.web.servlet.mvc.AbstractController AbstractController}或其子类之一来获得.
 *
 * <h3>设计和测试说明</h3>
 *
 * <p>Controller接口明确设计为在HttpServletRequest和HttpServletResponse对象上运行, 就像HttpServlet一样.
 * 它的目的不是将自己与Servlet API分离, 与WebWork, JSF或Tapestry形成对比.
 * 相反, Servlet API的全部功能可用, 允许Controller具有通用性:
 * Controller不仅能够处理Web用户接口请求, 还能处理远程协议或按需生成报告.
 *
 * <p>通过将HttpServletRequest和HttpServletResponse对象的模拟对象作为参数传递给
 * {@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest}方法, 可以轻松地测试Controller.
 * 为方便起见, Spring附带了一组Servlet API模拟, 适用于测试任何类型的Web组件, 但特别适合测试Spring Web控制器.
 * 与Struts Action相比, 不需要模拟ActionServlet或任何其他基础结构; 模拟HttpServletRequest和HttpServletResponse就足够了.
 *
 * <p>如果控制器需要知道特定的环境引用, 他们可以选择实现特定的感知接口, 就像Spring (web) 应用程序上下文中的任何其他bean一样, 例如:
 * <ul>
 * <li>{@code org.springframework.context.ApplicationContextAware}</li>
 * <li>{@code org.springframework.context.ResourceLoaderAware}</li>
 * <li>{@code org.springframework.web.context.ServletContextAware}</li>
 * </ul>
 *
 * <p>通过相应感知接口中定义的相应setter, 可以在测试环境中容易地传递这样的环境引用.
 * 通常, 建议尽可能减少依赖关系:
 * 例如, 如果只需要资源加载, 仅需要实现ResourceLoaderAware.
 * 或者, 从WebApplicationObjectSupport基类派生, 它通过方便的访问器提供所有这些引用, 但在初始化时需要ApplicationContext引用.
 *
 * <p>控制器可以选择实现{@link LastModified}接口.
 */
public interface Controller {

	/**
	 * 处理请求, 并返回DispatcherServlet将呈现的ModelAndView对象.
	 * {@code null}返回值不是错误:
	 * 它表示此对象已完成请求处理本身, 因此没有要呈现的ModelAndView.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @return 要呈现的ModelAndView, 如果直接处理, 则为{@code null}
	 * @throws Exception
	 */
	ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
