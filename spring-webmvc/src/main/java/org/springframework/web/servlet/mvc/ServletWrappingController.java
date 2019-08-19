package org.springframework.web.servlet.mvc;

import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;

/**
 * Spring Controller实现, 它包装在内部管理的servlet实例.
 * 这种包装的servlet在该控制器之外是未知的; 它的整个生命周期都在这里 (与{@link ServletForwardingController}相反).
 *
 * <p>用于通过Spring的调度基础结构调用现有的servlet, 例如将Spring HandlerInterceptors应用于其请求.
 *
 * <p>请注意, Struts有一个特殊要求, 它解析{@code web.xml}以查找其servlet映射.
 * 因此, 需要在此控制器上将DispatcherServlet的servlet名称指定为"servletName",
 * 以便Struts找到DispatcherServlet的映射 (认为它引用了ActionServlet).
 *
 * <p><b>Example:</b> 一个DispatcherServlet XML上下文,
 * 将"*.do"转发到由ServletWrappingController包装的Struts ActionServlet.
 * 所有这些请求都将通过配置的HandlerInterceptor链 (e.g. OpenSessionInViewInterceptor).
 * 从Struts的角度来看, 一切都会像往常一样工作.
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
 *       &lt;prop key="*.do"&gt;strutsWrappingController&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="strutsWrappingController" class="org.springframework.web.servlet.mvc.ServletWrappingController"&gt;
 *   &lt;property name="servletClass"&gt;
 *     &lt;value&gt;org.apache.struts.action.ActionServlet&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="servletName"&gt;
 *     &lt;value&gt;action&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="initParameters"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="config"&gt;/WEB-INF/struts-config.xml&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 */
public class ServletWrappingController extends AbstractController
		implements BeanNameAware, InitializingBean, DisposableBean {

	private Class<? extends Servlet> servletClass;

	private String servletName;

	private Properties initParameters = new Properties();

	private String beanName;

	private Servlet servletInstance;


	public ServletWrappingController() {
		super(false);
	}


	/**
	 * 设置要包装的servlet的类.
	 * 需要实现{@code javax.servlet.Servlet}.
	 */
	public void setServletClass(Class<? extends Servlet> servletClass) {
		this.servletClass = servletClass;
	}

	/**
	 * 设置要包装的servlet的名称.
	 * 默认是此控制器的bean名称.
	 */
	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	/**
	 * 指定要包装的servlet的init参数, name-value对.
	 */
	public void setInitParameters(Properties initParameters) {
		this.initParameters = initParameters;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * 初始化包装的Servlet实例.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.servletClass == null) {
			throw new IllegalArgumentException("'servletClass' is required");
		}
		if (this.servletName == null) {
			this.servletName = this.beanName;
		}
		this.servletInstance = this.servletClass.newInstance();
		this.servletInstance.init(new DelegatingServletConfig());
	}


	/**
	 * 调用包装的Servlet实例.
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		this.servletInstance.service(request, response);
		return null;
	}


	/**
	 * 销毁包装的Servlet实例.
	 */
	@Override
	public void destroy() {
		this.servletInstance.destroy();
	}


	/**
	 * ServletConfig接口的内部实现, 传递给包装的servlet.
	 * 委托给ServletWrappingController字段和方法以提供init参数和其他环境信息.
	 */
	private class DelegatingServletConfig implements ServletConfig {

		@Override
		public String getServletName() {
			return servletName;
		}

		@Override
		public ServletContext getServletContext() {
			return ServletWrappingController.this.getServletContext();
		}

		@Override
		public String getInitParameter(String paramName) {
			return initParameters.getProperty(paramName);
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Enumeration<String> getInitParameterNames() {
			return (Enumeration) initParameters.keys();
		}
	}

}
