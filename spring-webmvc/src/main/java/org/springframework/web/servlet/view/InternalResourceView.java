package org.springframework.web.servlet.view;

import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * 同一Web应用程序中的JSP或其他资源的包装器.
 * 将模型对象公开为请求属性, 并使用{@link javax.servlet.RequestDispatcher}将请求转发到指定的资源URL.
 *
 * <p>此视图的URL应该指定Web应用程序中的资源, 适用于RequestDispatcher的{@code forward}或{@code include}方法.
 *
 * <p>如果在已包含的请求中或在已提交的响应内操作, 则此视图将回退到包含而不是转发.
 * 这可以通过在呈现视图之前调用{@code response.flushBuffer()} (将提交响应)来强制执行.
 *
 * <p>从DispatcherServlet上下文定义的角度看, {@link InternalResourceViewResolver}的典型用法如下所示:
 *
 * <pre class="code">&lt;bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 从处理器返回的每个视图名称都将转换为JSP资源 (例如: "myView" -> "/WEB-INF/jsp/myView.jsp"), 默认情况下使用此视图类.
 */
public class InternalResourceView extends AbstractUrlBasedView {

	private boolean alwaysInclude = false;

	private boolean preventDispatchLoop = false;


	public InternalResourceView() {
	}

	/**
	 * @param url 要转发到的URL
	 */
	public InternalResourceView(String url) {
		super(url);
	}

	/**
	 * @param url 要转发到的URL
	 * @param alwaysInclude 是否始终包含视图, 而不是转发到视图
	 */
	public InternalResourceView(String url, boolean alwaysInclude) {
		super(url);
		this.alwaysInclude = alwaysInclude;
	}


	/**
	 * 指定是始终包含视图而不是转发到它.
	 * <p>默认为"false". 切换此标志以强制使用Servlet包含, 即使可以转发.
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}

	/**
	 * 设置是否显式阻止调度回当前处理器路径.
	 * <p>默认为"false". 对于基于约定的视图, 将其切换为"true", 其中发送回当前处理器路径是确定性错误.
	 */
	public void setPreventDispatchLoop(boolean preventDispatchLoop) {
		this.preventDispatchLoop = preventDispatchLoop;
	}

	/**
	 * InternalResourceView并不严格要求ApplicationContext.
	 */
	@Override
	protected boolean isContextRequired() {
		return false;
	}


	/**
	 * 渲染指定模型的内部资源.
	 * 这包括将模型设置为请求属性.
	 */
	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 将模型对象公开为请求属性.
		exposeModelAsRequestAttributes(model, request);

		// 将助手公开为请求属性.
		exposeHelpers(request);

		// 确定请求调度器的路径.
		String dispatcherPath = prepareForRendering(request, response);

		// 获取目标资源的RequestDispatcher (通常是JSP).
		RequestDispatcher rd = getRequestDispatcher(request, dispatcherPath);
		if (rd == null) {
			throw new ServletException("Could not get RequestDispatcher for [" + getUrl() +
					"]: Check that the corresponding file exists within your web application archive!");
		}

		// 如果已经包含或响应已经提交, 执行 include, 否则forward.
		if (useInclude(request, response)) {
			response.setContentType(getContentType());
			if (logger.isDebugEnabled()) {
				logger.debug("Including resource [" + getUrl() + "] in InternalResourceView '" + getBeanName() + "'");
			}
			rd.include(request, response);
		}

		else {
			// Note: 转发的资源应该确定内容类型本身.
			if (logger.isDebugEnabled()) {
				logger.debug("Forwarding to resource [" + getUrl() + "] in InternalResourceView '" + getBeanName() + "'");
			}
			rd.forward(request, response);
		}
	}

	/**
	 * 公开每个渲染操作独有的助手.
	 * 这是必要的, 以便不同的渲染操作不会覆盖彼此的上下文等.
	 * <p>由{@link #renderMergedOutputModel(Map, HttpServletRequest, HttpServletResponse)}调用.
	 * 默认实现为空. 可以重写此方法以将自定义助手添加为请求属性.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @throws Exception 如果在添加属性时出现致命错误
	 */
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
	}

	/**
	 * 准备渲染, 并确定转发到 (或包括)的请求调度器路径.
	 * <p>此实现只返回配置的URL.
	 * 子类可以覆盖它以确定要呈现的资源, 通常以不同的方式解释URL.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @return 要使用的请求调度器路径
	 * @throws Exception 如果准备失败了
	 */
	protected String prepareForRendering(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String path = getUrl();
		if (this.preventDispatchLoop) {
			String uri = request.getRequestURI();
			if (path.startsWith("/") ? uri.equals(path) : uri.equals(StringUtils.applyRelativePath(uri, path))) {
				throw new ServletException("Circular view path [" + path + "]: would dispatch back " +
						"to the current handler URL [" + uri + "] again. Check your ViewResolver setup! " +
						"(Hint: This may be the result of an unspecified view, due to default view name generation.)");
			}
		}
		return path;
	}

	/**
	 * 获取用于 forward/include 的RequestDispatcher.
	 * <p>默认实现简单调用{@link HttpServletRequest#getRequestDispatcher(String)}.
	 * 可以在子类中重写.
	 * 
	 * @param request 当前的HTTP请求
	 * @param path 目标URL (由{@link #prepareForRendering}返回)
	 * 
	 * @return 相应的RequestDispatcher
	 */
	protected RequestDispatcher getRequestDispatcher(HttpServletRequest request, String path) {
		return request.getRequestDispatcher(path);
	}

	/**
	 * 确定是否使用RequestDispatcher的{@code include} 或 {@code forward}方法.
	 * <p>执行检查是否在请求中找到包含URI属性, 指示包含请求以及响应是否已提交.
	 * 在这两种情况下, 都将执行包含, 因为不再可能进行转发.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @return {@code true}用于include, {@code false}用于forward
	 */
	protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
		return (this.alwaysInclude || WebUtils.isIncludeRequest(request) || response.isCommitted());
	}
}
