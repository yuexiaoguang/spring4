package org.springframework.web.portlet.mvc;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventPortlet;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceServingPortlet;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.NoHandlerFoundException;
import org.springframework.web.portlet.context.PortletConfigAware;
import org.springframework.web.portlet.context.PortletContextAware;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * {@link Controller}实现, 它包装了一个内部管理的portlet实例.
 * 这种包装的portlet在该控制器之外是未知的; 它的整个生命周期都在这里.
 *
 * <p>用于通过Spring的调度基础结构调用现有portlet, 例如将 Spring
 * {@link org.springframework.web.portlet.HandlerInterceptor HandlerInterceptors}应用于其请求.
 *
 * <p><b>Example:</b>
 *
 * <pre class="code">&lt;bean id="wrappingController" class="org.springframework.web.portlet.mvc.PortletWrappingController"&gt;
 *   &lt;property name="portletClass"&gt;
 *     &lt;value&gt;org.springframework.web.portlet.sample.HelloWorldPortlet&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="portletName"&gt;
 *     &lt;value&gt;hello-world&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="initParameters"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="config"&gt;/WEB-INF/hello-world-portlet-config.xml&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 */
public class PortletWrappingController extends AbstractController
		implements ResourceAwareController, EventAwareController,
		BeanNameAware, InitializingBean, DisposableBean, PortletContextAware, PortletConfigAware {

	private boolean useSharedPortletConfig = true;

	private PortletContext portletContext;

	private PortletConfig portletConfig;

	private Class<?> portletClass;

	private String portletName;

	private Map<String, String> initParameters = new LinkedHashMap<String, String>();

	private String beanName;

	private Portlet portletInstance;


	/**
	 * 设置是否使用通过{@code setPortletConfig}传入的共享PortletConfig对象.
	 * <p>默认为"true".
	 * 将此设置设置为"false"以传入模拟PortletConfig对象, 其中bean名称为portlet名称, 并保存当前的PortletContext.
	 */
	public void setUseSharedPortletConfig(boolean useSharedPortletConfig) {
		this.useSharedPortletConfig = useSharedPortletConfig;
	}

	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}

	@Override
	public void setPortletConfig(PortletConfig portletConfig) {
		this.portletConfig = portletConfig;
	}

	/**
	 * 设置要包装的Portlet的类.
	 * 需要实现{@code javax.portlet.Portlet}.
	 */
	public void setPortletClass(Class<?> portletClass) {
		this.portletClass = portletClass;
	}

	/**
	 * 设置要包装的Portlet的名称.
	 * 默认是此控制器的bean名称.
	 */
	public void setPortletName(String portletName) {
		this.portletName = portletName;
	}

	/**
	 * 指定要包装的portlet的init参数, 作为name-value对.
	 */
	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.portletClass == null) {
			throw new IllegalArgumentException("portletClass is required");
		}
		if (!Portlet.class.isAssignableFrom(this.portletClass)) {
			throw new IllegalArgumentException("portletClass [" + this.portletClass.getName() +
				"] needs to implement interface [javax.portlet.Portlet]");
		}
		if (this.portletName == null) {
			this.portletName = this.beanName;
		}
		PortletConfig config = this.portletConfig;
		if (config == null || !this.useSharedPortletConfig) {
			config = new DelegatingPortletConfig();
		}
		this.portletInstance = (Portlet) this.portletClass.newInstance();
		this.portletInstance.init(config);
	}


	@Override
	protected void handleActionRequestInternal(
			ActionRequest request, ActionResponse response) throws Exception {

		this.portletInstance.processAction(request, response);
	}

	@Override
	protected ModelAndView handleRenderRequestInternal(
			RenderRequest request, RenderResponse response) throws Exception {

		this.portletInstance.render(request, response);
		return null;
	}

	@Override
	public ModelAndView handleResourceRequest(
			ResourceRequest request, ResourceResponse response) throws Exception {

		if (!(this.portletInstance instanceof ResourceServingPortlet)) {
			throw new NoHandlerFoundException("Cannot handle resource request - target portlet [" +
					this.portletInstance.getClass() + " does not implement ResourceServingPortlet");
		}
		ResourceServingPortlet resourcePortlet = (ResourceServingPortlet) this.portletInstance;

		// 委派给PortletContentGenerator进行检查和准备.
		checkAndPrepare(request, response);

		// 如果需要, 在synchronized块中执行.
		if (isSynchronizeOnSession()) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					resourcePortlet.serveResource(request, response);
					return null;
				}
			}
		}

		resourcePortlet.serveResource(request, response);
		return null;
	}

	@Override
	public void handleEventRequest(
			EventRequest request, EventResponse response) throws Exception {

		if (!(this.portletInstance instanceof EventPortlet)) {
			logger.debug("Ignoring event request for non-event target portlet: " + this.portletInstance.getClass());
			return;
		}
		EventPortlet eventPortlet = (EventPortlet) this.portletInstance;

		// 委派给PortletContentGenerator进行检查和准备.
		check(request, response);

		// 如果需要, 在synchronized块中执行.
		if (isSynchronizeOnSession()) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					eventPortlet.processEvent(request, response);
					return;
				}
			}
		}

		eventPortlet.processEvent(request, response);
	}


	@Override
	public void destroy() {
		this.portletInstance.destroy();
	}


	/**
	 * PortletConfig接口的内部实现, 将传递给包装的portlet.
	 * <p>委托{@link PortletWrappingController}字段和方法提供init参数和其他环境信息.
	 */
	private class DelegatingPortletConfig implements PortletConfig {

		@Override
		public String getPortletName() {
			return portletName;
		}

		@Override
		public PortletContext getPortletContext() {
			return portletContext;
		}

		@Override
		public String getInitParameter(String paramName) {
			return initParameters.get(paramName);
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(initParameters.keySet());
		}

		@Override
		public ResourceBundle getResourceBundle(Locale locale) {
			return (portletConfig != null ? portletConfig.getResourceBundle(locale) : null);
		}

		@Override
		public Enumeration<String> getPublicRenderParameterNames() {
			return Collections.enumeration(Collections.<String>emptySet());
		}

		@Override
		public String getDefaultNamespace() {
			return XMLConstants.NULL_NS_URI;
		}

		@Override
		public Enumeration<QName> getPublishingEventQNames() {
			return Collections.enumeration(Collections.<QName>emptySet());
		}

		@Override
		public Enumeration<QName> getProcessingEventQNames() {
			return Collections.enumeration(Collections.<QName>emptySet());
		}

		@Override
		public Enumeration<Locale> getSupportedLocales() {
			return Collections.enumeration(Collections.<Locale>emptySet());
		}

		@Override
		public Map<String, String[]> getContainerRuntimeOptions() {
			return (portletConfig != null ? portletConfig.getContainerRuntimeOptions() : null);
		}
	}

}
