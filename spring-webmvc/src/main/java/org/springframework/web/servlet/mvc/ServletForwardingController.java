package org.springframework.web.servlet.mvc;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * Spring Controller实现, 它转发到命名的servlet, i.e. web.xml中的"servlet-name", 而不是URL路径映射.
 * 目标servlet首先在web.xml中甚至不需要"servlet-mapping": "servlet"声明就足够了.
 *
 * <p>用于通过Spring的调度基础结构调用现有的servlet, 例如将Spring HandlerInterceptors应用于其请求.
 * 这甚至可以在不支持Servlet过滤器的最小Servlet容器中工作.
 *
 * <p><b>Example:</b> web.xml, 映射所有"/myservlet"请求到Spring 分派器.
 * 还定义了一个自定义的"myServlet", 但<i>没有</i> servlet映射.
 *
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myServlet&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;mypackage.TestServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;org.springframework.web.servlet.DispatcherServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/myservlet&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <b>Example:</b> myDispatcher-servlet.xml, 反过来将"/myservlet"转发到servlet (由servlet名称标识).
 * 所有这些请求都将通过配置的HandlerInterceptor链 (e.g. OpenSessionInViewInterceptor).
 * 从servlet的角度来看, 一切都会像往常一样工作.
 *
 * <pre class="code">
 * &lt;bean id="urlMapping" class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"&gt;
 *   &lt;property name="interceptors"&gt;
 *     &lt;list&gt;
 *       &lt;ref bean="openSessionInViewInterceptor"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 *   &lt;property name="mappings"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="/myservlet"&gt;myServletForwardingController&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myServletForwardingController" class="org.springframework.web.servlet.mvc.ServletForwardingController"&gt;
 *   &lt;property name="servletName"&gt;&lt;value&gt;myServlet&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 */
public class ServletForwardingController extends AbstractController implements BeanNameAware {

	private String servletName;

	private String beanName;


	public ServletForwardingController() {
		super(false);
	}


	/**
	 * 设置要转发到的servlet的名称, i.e. web.xml中目标servlet的"servlet-name".
	 * <p>默认是此控制器的bean名称.
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
		if (this.servletName == null) {
			this.servletName = name;
		}
	}


	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		RequestDispatcher rd = getServletContext().getNamedDispatcher(this.servletName);
		if (rd == null) {
			throw new ServletException("No servlet with name '" + this.servletName + "' defined in web.xml");
		}
		// If already included, include again, else forward.
		if (useInclude(request, response)) {
			rd.include(request, response);
			if (logger.isDebugEnabled()) {
				logger.debug("Included servlet [" + this.servletName +
						"] in ServletForwardingController '" + this.beanName + "'");
			}
		}
		else {
			rd.forward(request, response);
			if (logger.isDebugEnabled()) {
				logger.debug("Forwarded to servlet [" + this.servletName +
						"] in ServletForwardingController '" + this.beanName + "'");
			}
		}
		return null;
	}

	/**
	 * 确定是否使用RequestDispatcher的{@code include}或{@code forward}方法.
	 * <p>执行检查是否在请求中找到include URI属性, 指示include请求以及响应是否已提交.
	 * 在这两种情况下, 都将执行include, 因为不再可能进行转发.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @return {@code true}用于include, {@code false}用于forward
	 */
	protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
		return (WebUtils.isIncludeRequest(request) || response.isCommitted());
	}

}
