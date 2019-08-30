package org.springframework.web.portlet;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;
import org.springframework.web.portlet.context.PortletApplicationContextUtils;
import org.springframework.web.portlet.context.PortletRequestAttributes;
import org.springframework.web.portlet.context.PortletRequestHandledEvent;
import org.springframework.web.portlet.context.StandardPortletEnvironment;
import org.springframework.web.portlet.context.XmlPortletApplicationContext;

/**
 * Spring的portlet框架的基本portlet.
 * 在基于JavaBean的整体解决方案中提供与Spring应用程序上下文的集成.
 *
 * <p>该类提供以下功能:
 * <ul>
 * <li>每个portlet管理一个Portlet {@link org.springframework.context.ApplicationContext}实例.
 * portlet的配置由portlet命名空间中的bean确定.
 * <li>无论请求是否成功处理, 都会根据请求处理发布事件.
 * </ul>
 *
 * <p>子类必须实现{@link #doActionService}和{@link #doRenderService}来处理操作和渲染请求.
 * 因为这会直接扩展{@link GenericPortletBean}而不是Portlet, 所以bean属性会映射到它上面.
 * 子类可以覆盖{@link #initFrameworkPortlet()}以进行自定义初始化.
 *
 * <p>在portlet init-param级别关注"contextClass"参数, 如果找不到则回退到默认的上下文类
 * ({@link org.springframework.web.portlet.context.XmlPortletApplicationContext}).
 * 请注意, 使用默认的FrameworkPortlet, 上下文类需要实现
 * {@link org.springframework.web.portlet.context.ConfigurablePortletApplicationContext} SPI.
 *
 * <p>将"contextConfigLocation" portlet init-param传递给上下文实例,
 * 将其解析为可能由多个逗号和空格分隔的多个文件路径, 例如"test-portlet.xml, myPortlet.xml".
 * 如果未明确指定, 则上下文实现应该从portlet的命名空间构建默认位置.
 *
 * <p>Note: 在多个配置位置的情况下, 以后的bean定义将覆盖在先前加载的文件中定义的那些,
 * 至少在使用Spring的默认ApplicationContext实现之一时.
 * 这可以用来通过额外的XML文件故意覆盖某些bean定义.
 *
 * <p>默认命名空间是"'portlet-name'-portlet", e.g. portlet-name "test"的"test-portlet"
 * (通过XmlPortletApplicationContext导致"/WEB-INF/test-portlet.xml"默认位置).
 * 也可以通过"namespace" portlet init-param显式设置命名空间.
 */
public abstract class FrameworkPortlet extends GenericPortletBean
		implements ApplicationListener<ContextRefreshedEvent> {

	/**
	 * FrameworkPortlet的默认上下文类.
	 */
	public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlPortletApplicationContext.class;

	/**
	 * Portlet ApplicationContext命名空间的后缀.
	 * 如果此类的portlet在上下文中被赋予名称 "test", 则portlet使用的命名空间将解析为"test-portlet".
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-portlet";

	/**
	 * Portlet ApplicationContext的PortletContext属性的前缀.
	 * 完成是portlet名称.
	 */
	public static final String PORTLET_CONTEXT_PREFIX = FrameworkPortlet.class.getName() + ".CONTEXT.";

	/**
	 * 默认USER_INFO属性名称, 用于搜索当前用户名: "user.login.id", "user.name".
	 */
	public static final String[] DEFAULT_USERINFO_ATTRIBUTE_NAMES = {"user.login.id", "user.name"};


	/** 要使用的Portlet ApplicationContext实现类 */
	private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;

	/** 此portlet的命名空间 */
	private String namespace;

	/** 显式上下文配置位置 */
	private String contextConfigLocation;

	/** 是否应该将上下文发布为PortletContext属性? */
	private boolean publishContext = true;

	/** 是否应该在每个请求结束时发布PortletRequestHandledEvent? */
	private boolean publishEvents = true;

	/** 将LocaleContext和RequestAttributes公开为子线程可继承? */
	private boolean threadContextInheritable = false;

	/** USER_INFO属性, 可能包含当前用户的用户名 */
	private String[] userinfoUsernameAttributes = DEFAULT_USERINFO_ATTRIBUTE_NAMES;

	/** 此portlet的ApplicationContext */
	private ApplicationContext portletApplicationContext;

	/** 用于检测是否已调用onRefresh的标志 */
	private boolean refreshEventReceived = false;


	/**
	 * 设置自定义上下文类.
	 * 此类必须是ApplicationContext类型;
	 * 使用默认的FrameworkPortlet实现时, 上下文类还必须实现 ConfigurablePortletApplicationContext.
	 */
	public void setContextClass(Class<?> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * 返回自定义上下文类.
	 */
	public Class<?> getContextClass() {
		return this.contextClass;
	}

	/**
	 * 设置此portlet的自定义命名空间, 用于构建默认上下文配置位置.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * 返回此portlet的命名空间, 如果未设置自定义命名空间, 则返回默认方案.
	 * (e.g. 名称为"test"的portlet表示"test-portlet")
	 */
	public String getNamespace() {
		return (this.namespace != null) ? this.namespace : getPortletName() + DEFAULT_NAMESPACE_SUFFIX;
	}

	/**
	 * 显式设置上下文配置位置, 而不是依赖于从命名空间构建的默认位置.
	 * 此位置字符串可以包含由任意数量的逗号和空格分隔的多个位置.
	 */
	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * 返回显式上下文配置位置.
	 */
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * 设置是否将此portlet的上下文作为PortletContext属性发布, 可用于Web容器中的所有对象.
	 * 默认为 true.
	 * <p>这在测试期间尤其方便, 尽管让其他应用程序对象以这种方式访问​​上下文是否是一种好的做法值得商榷.
	 */
	public void setPublishContext(boolean publishContext) {
		this.publishContext = publishContext;
	}

	/**
	 * 设置此portlet是否应在每个请求结束时发布PortletRequestHandledEvent.
	 * 默认为 true; 可以关闭以获得轻微的性能提升, 前提是没有ApplicationListeners依赖此类事件.
	 */
	public void setPublishEvents(boolean publishEvents) {
		this.publishEvents = publishEvents;
	}

	/**
	 * 设置是否将LocaleContext和RequestAttributes公开为子线程可继承 (使用{@link java.lang.InheritableThreadLocal}).
	 * <p>默认为 "false", 以避免对衍生的后台线程产生副作用.
	 * 将其切换为"true", 以启用在请求处理期间生成并仅用于此请求的自定义子线程的继承
	 * (也就是说, 在他们的初始任务结束后, 不重用线程).
	 * <p><b>WARNING:</b> 如果正在访问可能配置为按需添加新线程的线程池 (e.g. a JDK {@link java.util.concurrent.ThreadPoolExecutor}),
	 * 不要对子线程使用继承, 因为这会将继承的上下文暴露给此类.
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}

	/**
	 * 在尝试查找当前用户的用户名时, 设置要在USER_INFO映射中搜索的属性列表.
	 */
	public void setUserinfoUsernameAttributes(String[] userinfoUsernameAttributes) {
		this.userinfoUsernameAttributes = userinfoUsernameAttributes;
	}


	/**
	 * GenericPortletBean的重写方法, 在设置任何bean属性后调用.
	 * 创建此portlet的ApplicationContext.
	 */
	@Override
	protected final void initPortletBean() throws PortletException {
		getPortletContext().log("Initializing Spring FrameworkPortlet '" + getPortletName() + "'");
		if (logger.isInfoEnabled()) {
			logger.info("FrameworkPortlet '" + getPortletName() + "': initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			this.portletApplicationContext = initPortletApplicationContext();
			initFrameworkPortlet();
		}
		catch (PortletException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isInfoEnabled()) {
			long elapsedTime = System.currentTimeMillis() - startTime;
			logger.info("FrameworkPortlet '" + getPortletName() + "': initialization completed in " + elapsedTime + " ms");
		}
	}

	/**
	 * 初始化并发布此portlet的Portlet ApplicationContext.
	 * <p>委托给{@link #createPortletApplicationContext}进行实际创建.
	 * 可以在子类中重写.
	 * 
	 * @return 此portlet的ApplicationContext
	 */
	protected ApplicationContext initPortletApplicationContext() {
		ApplicationContext parent = PortletApplicationContextUtils.getWebApplicationContext(getPortletContext());
		ApplicationContext pac = createPortletApplicationContext(parent);

		if (!this.refreshEventReceived) {
			// 显然不是具有刷新支持的ConfigurableApplicationContext: 在这里手动触发初始onRefresh.
			onRefresh(pac);
		}

		if (this.publishContext) {
			// 将上下文发布为portlet上下文属性
			String attName = getPortletContextAttributeName();
			getPortletContext().setAttribute(attName, pac);
			if (logger.isDebugEnabled()) {
				logger.debug("Published ApplicationContext of portlet '" + getPortletName() +
						"' as PortletContext attribute with name [" + attName + "]");
			}
		}
		return pac;
	}

	/**
	 * 为此portlet实例化Portlet ApplicationContext, 默认的XmlPortletApplicationContext或自定义上下文类.
	 * <p>此实现期望自定义上下文实现ConfigurablePortletApplicationContext. 可以在子类中重写.
	 * 
	 * @param parent 要使用的父级ApplicationContext, 或 null
	 * 
	 * @return 此portlet的Portlet ApplicationContext
	 */
	protected ApplicationContext createPortletApplicationContext(ApplicationContext parent) {
		Class<?> contextClass = getContextClass();
		if (logger.isDebugEnabled()) {
			logger.debug("Portlet with name '" + getPortletName() +
					"' will try to create custom ApplicationContext context of class '" +
					contextClass.getName() + "'" + ", using parent context [" + parent + "]");
		}
		if (!ConfigurablePortletApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Fatal initialization error in portlet with name '" + getPortletName() +
					"': custom ApplicationContext class [" + contextClass.getName() +
					"] is not of type ConfigurablePortletApplicationContext");
		}
		ConfigurablePortletApplicationContext pac =
				(ConfigurablePortletApplicationContext) BeanUtils.instantiateClass(contextClass);

		// 分配最佳的id值.
		String portletContextName = getPortletContext().getPortletContextName();
		if (portletContextName != null) {
			pac.setId(ConfigurablePortletApplicationContext.APPLICATION_CONTEXT_ID_PREFIX + portletContextName + "." + getPortletName());
		}
		else {
			pac.setId(ConfigurablePortletApplicationContext.APPLICATION_CONTEXT_ID_PREFIX + getPortletName());
		}

		pac.setEnvironment(getEnvironment());
		pac.setParent(parent);
		pac.setPortletContext(getPortletContext());
		pac.setPortletConfig(getPortletConfig());
		pac.setNamespace(getNamespace());
		pac.setConfigLocation(getContextConfigLocation());
		pac.addApplicationListener(new SourceFilteringListener(pac, this));

		// 在刷新上下文时, 将在任何情况下调用wac环境的 #initPropertySources;
		// 在此实时地确保portlet属性源适用于在 #refresh 之前发生的任何后处理或初始化
		ConfigurableEnvironment env = pac.getEnvironment();
		if (env instanceof StandardPortletEnvironment) {
			((StandardPortletEnvironment) env).initPropertySources(pac.getServletContext(), getPortletContext(), getPortletConfig());
		}

		postProcessPortletApplicationContext(pac);
		pac.refresh();

		return pac;
	}

	/**
	 * 在给定的Portlet ApplicationContext刷新并作为此portlet的上下文激活之前对其进行后处理.
	 * <p>默认实现为空. 此方法返回后, 将自动调用{@code refresh()}.
	 * 
	 * @param pac 配置的Portlet ApplicationContext (尚未刷新)
	 */
	protected void postProcessPortletApplicationContext(ConfigurableApplicationContext pac) {
	}

	/**
	 * 返回此portlet的ApplicationContext的PortletContext属性名称.
	 * <p>默认实现返回 PORTLET_CONTEXT_PREFIX + portlet名称.
	 */
	public String getPortletContextAttributeName() {
		return PORTLET_CONTEXT_PREFIX + getPortletName();
	}

	/**
	 * 返回此portlet的ApplicationContext.
	 */
	public final ApplicationContext getPortletApplicationContext() {
		return this.portletApplicationContext;
	}


	/**
	 * 在设置任何bean属性并加载ApplicationContext之后, 将调用此方法.
	 * <p>默认实现为空; 子类可以重写此方法以执行它们所需的任何初始化.
	 * 
	 * @throws PortletException 初始化异常
	 */
	protected void initFrameworkPortlet() throws PortletException {
	}

	/**
	 * 刷新此portlet的应用程序上下文以及portlet的依赖状态.
	 */
	public void refresh() {
		ApplicationContext pac = getPortletApplicationContext();
		if (!(pac instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException("Portlet ApplicationContext does not support refresh: " + pac);
		}
		((ConfigurableApplicationContext) pac).refresh();
	}

	/**
	 * ApplicationListener端点, 从此servlet的WebApplicationContext接收事件.
	 * <p>在{@link org.springframework.context.event.ContextRefreshedEvent}的情况下,
	 * 默认实现调用{@link #onRefresh}, 触发刷新此servlet的依赖于上下文的状态.
	 * 
	 * @param event 传入的ApplicationContext事件
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.refreshEventReceived = true;
		onRefresh(event.getApplicationContext());
	}

	/**
	 * 可以重写的模板方法, 以添加特定于portlet的刷新工作.
	 * 成功刷新上下文后调用.
	 * <p>此实现为空.
	 * 
	 * @param context 当前的Portlet ApplicationContext
	 */
	protected void onRefresh(ApplicationContext context) {
		// For subclasses: do nothing by default.
	}


	/**
	 * 在单元测试中被覆盖以获得更友好的行为.
	 */
	@Override
	protected String getTitle(RenderRequest renderRequest) {
		try {
			return super.getTitle(renderRequest);
		}
		catch (NullPointerException ex) {
			return getPortletName();
		}
	}

	/**
	 * 将操作请求委托给 processRequest/doActionService.
	 */
	@Override
	public final void processAction(ActionRequest request, ActionResponse response)
			throws PortletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 将渲染请求委托给 processRequest/doRenderService.
	 */
	@Override
	protected final void doDispatch(RenderRequest request, RenderResponse response)
			throws PortletException, IOException {

		processRequest(request, response);
	}

	@Override
	public void serveResource(ResourceRequest request, ResourceResponse response)
			throws PortletException, IOException {

		processRequest(request, response);
	}

	@Override
	public void processEvent(EventRequest request, EventResponse response)
			throws PortletException, IOException {

		processRequest(request, response);
	}

	/**
	 * 处理此请求, 无论结果如何都发布事件.
	 * 实际的事件处理由抽象的{@code doActionService()}和{@code doRenderService()}模板方法执行.
	 */
	protected final void processRequest(PortletRequest request, PortletResponse response)
			throws PortletException, IOException {

		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		// 公开当前LocaleResolver和请求为LocaleContext.
		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		LocaleContextHolder.setLocaleContext(buildLocaleContext(request), this.threadContextInheritable);

		// 将当前RequestAttributes公开给当前线程.
		RequestAttributes previousRequestAttributes = RequestContextHolder.getRequestAttributes();
		PortletRequestAttributes requestAttributes = null;
		if (previousRequestAttributes == null ||
				PortletRequestAttributes.class == previousRequestAttributes.getClass() ||
				ServletRequestAttributes.class == previousRequestAttributes.getClass()) {
			requestAttributes = new PortletRequestAttributes(request, response);
			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Bound request context to thread: " + request);
		}

		try {
			String phase = (String) request.getAttribute(PortletRequest.LIFECYCLE_PHASE);
			if (PortletRequest.ACTION_PHASE.equals(phase)) {
				doActionService((ActionRequest) request, (ActionResponse) response);
			}
			else if (PortletRequest.RENDER_PHASE.equals(phase)) {
				doRenderService((RenderRequest) request, (RenderResponse) response);
			}
			else if (PortletRequest.RESOURCE_PHASE.equals(phase)) {
				doResourceService((ResourceRequest) request, (ResourceResponse) response);
			}
			else if (PortletRequest.EVENT_PHASE.equals(phase)) {
				doEventService((EventRequest) request, (EventResponse) response);
			}
			else {
				throw new IllegalStateException("Invalid portlet request phase: " + phase);
			}
		}
		catch (PortletException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (IOException ex) {
			failureCause = ex;
			throw ex;
		}
		catch (Throwable ex) {
			failureCause = ex;
			throw new PortletException("Request processing failed", ex);
		}

		finally {
			// 清除请求属性并重置线程绑定上下文.
			LocaleContextHolder.setLocaleContext(previousLocaleContext, this.threadContextInheritable);
			if (requestAttributes != null) {
				RequestContextHolder.setRequestAttributes(previousRequestAttributes, this.threadContextInheritable);
				requestAttributes.requestCompleted();
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Cleared thread-bound resource request context: " + request);
			}

			if (failureCause != null) {
				logger.error("Could not complete request", failureCause);
			}
			else {
				logger.debug("Successfully completed request");
			}
			if (this.publishEvents) {
				// 无论是否成功， 都要发布一个事件.
				long processingTime = System.currentTimeMillis() - startTime;
				this.portletApplicationContext.publishEvent(
						new PortletRequestHandledEvent(this,
								getPortletConfig().getPortletName(), request.getPortletMode().toString(),
								(request instanceof ActionRequest ? "action" : "render"),
								request.getRequestedSessionId(), getUsernameForRequest(request),
								processingTime, failureCause));
			}
		}
	}

	/**
	 * 为给定请求构建LocaleContext, 将请求的主要区域设置公开为当前区域设置.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 相应的LocaleContext
	 */
	protected LocaleContext buildLocaleContext(PortletRequest request) {
		return new SimpleLocaleContext(request.getLocale());
	}

	/**
	 * 确定给定请求的用户名.
	 * <p>默认实现首先尝试UserPrincipal. 如果不存在, 则检查 USER_INFO map.
	 * 可以在子类中重写.
	 * 
	 * @param request 当前的portlet请求
	 * 
	 * @return 用户名, 或{@code null}
	 */
	protected String getUsernameForRequest(PortletRequest request) {
		// Try the principal.
		Principal userPrincipal = request.getUserPrincipal();
		if (userPrincipal != null) {
			return userPrincipal.getName();
		}

		// Try the remote user name.
		String userName = request.getRemoteUser();
		if (userName != null) {
			return userName;
		}

		// Try the Portlet USER_INFO map.
		Map<?, ?> userInfo = (Map<?, ?>) request.getAttribute(PortletRequest.USER_INFO);
		if (userInfo != null) {
			for (int i = 0, n = this.userinfoUsernameAttributes.length; i < n; i++) {
				userName = (String) userInfo.get(this.userinfoUsernameAttributes[i]);
				if (userName != null) {
					return userName;
				}
			}
		}

		// Nothing worked...
		return null;
	}


	/**
	 * 子类必须实现此方法才能处理操作请求.
	 * <p>该约定与GenericPortlet的{@code processAction}方法基本相同.
	 * <p>此类拦截调用以确保发生异常处理和事件发布.
	 * 
	 * @param request 当前的操作请求
	 * @param response 当前的操作响应
	 * 
	 * @throws Exception 在任何处理失败的情况下
	 */
	protected abstract void doActionService(ActionRequest request, ActionResponse response)
			throws Exception;

	/**
	 * 子类必须实现此方法才能处理渲染请求.
	 * <p>该约定与GenericPortlet的{@code doDispatch}方法基本相同.
	 * <p>此类拦截调用以确保发生异常处理和事件发布.
	 * 
	 * @param request 当前的渲染请求
	 * @param response 当前的渲染响应
	 * 
	 * @throws Exception 处理失败
	 */
	protected abstract void doRenderService(RenderRequest request, RenderResponse response)
			throws Exception;

	/**
	 * 子类必须实现此方法才能处理资源请求.
	 * <p>该约定与GenericPortlet的{@code serveResource}方法基本相同.
	 * <p>此类拦截调用以确保发生异常处理和事件发布.
	 * 
	 * @param request 当前的资源请求
	 * @param response 当前的资源响应
	 * 
	 * @throws Exception 处理失败
	 */
	protected abstract void doResourceService(ResourceRequest request, ResourceResponse response)
			throws Exception;

	/**
	 * 子类必须实现此方法才能处理事件请求.
	 * <p>该约定与GenericPortlet的{@code processEvent}方法基本相同.
	 * <p>此类拦截调用以确保发生异常处理和事件发布..
	 * 
	 * @param request 当前的事件请求
	 * @param response 当前的事件响应
	 * 
	 * @throws Exception 处理失败
	 */
	protected abstract void doEventService(EventRequest request, EventResponse response)
			throws Exception;


	/**
	 * 关闭此portlet的ApplicationContext.
	 */
	@Override
	public void destroy() {
		getPortletContext().log("Destroying Spring FrameworkPortlet '" + getPortletName() + "'");
		if (this.portletApplicationContext instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) this.portletApplicationContext).close();
		}
	}
}
