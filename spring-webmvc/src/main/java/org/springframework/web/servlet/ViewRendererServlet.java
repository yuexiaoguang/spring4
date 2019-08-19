package org.springframework.web.servlet;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.util.NestedServletException;

/**
 * ViewRendererServlet是一个桥接servlet, 主要用于Portlet MVC支持.
 *
 * <p>对于Portlet的使用, 此Servlet是强制portlet容器将PortletRequest转换为ServletRequest所必需的,
 * 当通过PortletRequestDispatcher包含资源时, 它必须执行此操作.
 * 这样即使在Portlet环境中也可以重用整个基于Servlet的View支持.
 *
 * <p>桥接servlet的实际映射可在DispatcherPortlet中通过"viewRendererUrl"属性进行配置.
 * 默认为"/WEB-INF/servlet/view", 它仅适用于内部资源调度.
 */
@SuppressWarnings("serial")
public class ViewRendererServlet extends HttpServlet {

	/**
	 * 保存当前Web应用程序上下文的请求属性.
	 * 否则, 只有标签等可以获得全局Web应用程序上下文.
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;

	/** 包含View对象的请求属性的名称 */
	public static final String VIEW_ATTRIBUTE = ViewRendererServlet.class.getName() + ".VIEW";

	/** 包含模型Map的请求属性的名称 */
	public static final String MODEL_ATTRIBUTE = ViewRendererServlet.class.getName() + ".MODEL";


	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 处理此请求, 处理异常.
	 * 实际的事件处理由抽象的{@code renderView()}模板方法执行.
	 */
	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		try {
			renderView(request, response);
		}
		catch (ServletException ex) {
			throw ex;
		}
		catch (IOException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new NestedServletException("View rendering failed", ex);
		}
	}

	/**
	 * 检索View实例和模型Map以渲染并触发实际的渲染.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @throws Exception 处理失败
	 */
	@SuppressWarnings("unchecked")
	protected void renderView(HttpServletRequest request, HttpServletResponse response) throws Exception {
		View view = (View) request.getAttribute(VIEW_ATTRIBUTE);
		if (view == null) {
			throw new ServletException("Could not complete render request: View is null");
		}
		Map<String, Object> model = (Map<String, Object>) request.getAttribute(MODEL_ATTRIBUTE);
		view.render(model, request, response);
	}

}
