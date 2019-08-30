package org.springframework.web.portlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.StateAwareResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.style.StylerUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.portlet.multipart.MultipartActionRequest;
import org.springframework.web.portlet.multipart.PortletMultipartResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewRendererServlet;
import org.springframework.web.servlet.ViewResolver;

/**
 * 在Portlet MVC框架内使用的中央调度器, e.g. 用于Web UI控制器.
 * 调度到已注册的处理器以处理portlet请求.
 *
 * <p>这个portlet非常灵活: 它可以与几乎任何工作流一起使用, 并安装适当的适配器类.
 * 它提供以下功能, 使其与其他请求驱动的Portlet MVC框架区别开来:
 *
 * <ul>
 * <li>它基于JavaBeans配置机制.
 *
 * <li>它可以使用任何{@link HandlerMapping}实现 - 预构建或作为应用程序的一部分提供 - 来控制对处理器对象的请求路由.
 * 默认是{@link org.springframework.web.portlet.mvc.annotation.DefaultAnnotationHandlerMapping}.
 * HandlerMapping对象可以在portlet的应用程序上下文中定义为bean, 实现HandlerMapping接口, 覆盖默认的HandlerMapping.
 * HandlerMappings可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>它可以使用任何{@link HandlerAdapter}; 这允许使用任何处理器接口.
 * Spring的{@link org.springframework.web.portlet.mvc.Controller}接口的默认适配器是
 * {@link org.springframework.web.portlet.mvc.SimpleControllerHandlerAdapter}.
 * 还将注册默认的{@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter}.
 * HandlerAdapter对象可以作为bean添加到应用程序上下文中, 覆盖默认的HandlerAdapter.
 * 与HandlerMappings一样, HandlerAdapters可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>可以通过{@link HandlerExceptionResolver}指定调度器的异常解析策略, 例如将某些异常映射到错误页面.
 * 默认无. 可以通过应用程序上下文添加其他HandlerExceptionResolver.
 * HandlerExceptionResolver可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>可以通过{@link ViewResolver}实现指定其视图解析策略, 将符号视图名称解析为View对象.
 * 默认为{@link org.springframework.web.servlet.view.InternalResourceViewResolver}.
 * ViewResolver对象可以作为bean添加到应用程序上下文中, 覆盖默认的ViewResolver.
 * ViewResolvers可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>调度器解析multipart请求的策略由
 * {@link org.springframework.web.portlet.multipart.PortletMultipartResolver}实现确定.
 * 包括Apache Commons FileUpload的实现:
 * {@link org.springframework.web.portlet.multipart.CommonsPortletMultipartResolver}.
 * MultipartResolver bean名称是"portletMultipartResolver"; 默认无.
 * </ul>
 *
 * <p><b>NOTE: 只有在调度程序中存在相应的{@code HandlerMapping} (用于类型级注解)和/或{@code HandlerAdapter} (用于方法级注解)时,
 * 才会处理{@code @RequestMapping}注解.</b>
 * 默认就是这种情况.
 * 但是, 如果要定义自定义{@code HandlerMappings}或{@code HandlerAdapters},
 * 则需要确保定义相应的自定义{@code DefaultAnnotationHandlerMapping}和/或{@code AnnotationMethodHandlerAdapter}
 *  - 前提是打算使用{@code @RequestMapping}.
 *
 * <p><b>Web应用程序可以定义任意数量的DispatcherPortlet.</b>
 * 每个portlet将在其自己的命名空间中运行, 使用映射, 处理器等加载其自己的应用程序上下文.
 * 只有{@link org.springframework.web.context.ContextLoaderListener}加载的根应用程序上下文将被共享.
 *
 * <p>Thanks to Rainer Schmitz, Nick Lothian and Eric Dalquist for their suggestions!
 */
public class DispatcherPortlet extends FrameworkPortlet {

	/**
	 * 此命名空间的Bean工厂中PortletMultipartResolver对象的已知名称.
	 */
	public static final String MULTIPART_RESOLVER_BEAN_NAME = "portletMultipartResolver";

	/**
	 * 此命名空间的Bean工厂中HandlerMapping对象的已知名称.
	 * 仅在"detectAllHandlerMappings"关闭时使用.
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

	/**
	 * 此命名空间的Bean工厂中HandlerAdapter对象的已知名称.
	 * 仅在"detectAllHandlerAdapters"关闭时使用.
	 */
	public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

	/**
	 * 此命名空间的Bean工厂中HandlerExceptionResolver对象的已知名称.
	 * 仅在"detectAllHandlerExceptionResolvers"关闭时使用.
	 */
	public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

	/**
	 * 此命名空间的Bean工厂中的ViewResolver对象的已知名称.
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

	/**
	 * ViewRendererServlet的默认URL.
	 * 此桥接servlet用于将portlet渲染请求转换为servlet请求, 以便利用{@code org.springframework.web.view}包中的视图支持.
	 */
	public static final String DEFAULT_VIEW_RENDERER_URL = "/WEB-INF/servlet/view";

	/**
	 * 与此类的Servlet版本不同, 必须处理portlet请求的两阶段性质.
	 * 为此, 需要传递在操作阶段发生的任何异常, 以便它可以在渲染阶段显示.
	 * 向前传递事物并为每个渲染请求保留它们的唯一直接方法是通过渲染参数, 但这些只限于String对象, 需要传递Exception本身.
	 * 唯一的另一种方法是在会话中.
	 * 使用会话的坏处是无法知道何时完成重新渲染请求, 因此不知道何时可以从会话中删除对象.
	 * 因此, 当最终离开一个请求的渲染阶段并继续进行其他操作时, 最终将使用旧的异常来污染会话.
	 */
	public static final String ACTION_EXCEPTION_SESSION_ATTRIBUTE =
			DispatcherPortlet.class.getName() + ".ACTION_EXCEPTION";

	/**
	 * 此渲染参数用于向渲染阶段指示在操作阶段期间发生异常.
	 */
	public static final String ACTION_EXCEPTION_RENDER_PARAMETER = "actionException";

	/**
	 * 未找到请求的映射处理器时要使用的日志类别.
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.portlet.PageNotFound";

	/**
	 * 定义DispatcherPortet的默认策略名称的类路径资源的名称 (相对于DispatcherPortlet类).
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherPortlet.properties";


	/**
	 * 未找到请求的映射处理器时使用的其他记录器.
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	private static final Properties defaultStrategies;

	static {
		// 从属性文件加载默认策略实现.
		// 这当前是严格内部的, 不应由应用程序开发人员自定义.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherPortlet.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'DispatcherPortlet.properties': " + ex.getMessage());
		}
	}


	/** 检测所有HandlerMappings或只是期望"handlerMapping" bean? */
	private boolean detectAllHandlerMappings = true;

	/** 检测所有HandlerAdapter或只是期望"handlerAdapter" bean? */
	private boolean detectAllHandlerAdapters = true;

	/** 检测所有HandlerExceptionResolvers或只是期望"handlerExceptionResolver" bean? */
	private boolean detectAllHandlerExceptionResolvers = true;

	/** 检测所有ViewResolvers或只是期望"viewResolver" bean? */
	private boolean detectAllViewResolvers = true;

	/** 是否应将doAction期间抛出的异常转发给doRender */
	private boolean forwardActionException = true;

	/** 是否应将doEvent期间抛出的异常转发给doRender */
	private boolean forwardEventException = false;

	/** 指向ViewRendererServlet的URL */
	private String viewRendererUrl = DEFAULT_VIEW_RENDERER_URL;


	/** 此portlet使用的MultipartResolver */
	private PortletMultipartResolver multipartResolver;

	/** 此portlet使用的HandlerMappings列表 */
	private List<HandlerMapping> handlerMappings;

	/** 此portlet使用的HandlerAdapter列表 */
	private List<HandlerAdapter> handlerAdapters;

	/** 此portlet使用的HandlerExceptionResolvers列表 */
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	/** 此portlet使用的ViewResolvers列表 */
	private List<ViewResolver> viewResolvers;


	/**
	 * 设置是否在此portlet的上下文中检测所有HandlerMapping bean.
	 * 否则, 只需要一个名为"handlerMapping"的bean.
	 * <p>默认为true.
	 * 如果希望此portlet使用单个HandlerMapping, 请关闭此功能, 尽管在上下文中定义了多个HandlerMapping bean.
	 */
	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
		this.detectAllHandlerMappings = detectAllHandlerMappings;
	}

	/**
	 * 设置是否在此portlet的上下文中检测所有HandlerAdapter bean.
	 * 否则, 只需要一个名为"handlerAdapter"的bean.
	 * <p>默认为"true".
	 * 如果希望此portlet使用单个HandlerAdapter, 请关闭此功能, 尽管在上下文中定义了多个HandlerAdapter bean.
	 */
	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
	}

	/**
	 * 设置是否在此portlet的上下文中检测所有HandlerExceptionResolver bean.
	 * 否则, 只需要一个名为"handlerExceptionResolver"的bean.
	 * <p>默认为 true.
	 * 如果希望此portlet使用单个HandlerExceptionResolver, 请关闭此项, 尽管在上下文中定义了多个HandlerExceptionResolver bean.
	 */
	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
	}

	/**
	 * 设置是否在此portlet的上下文中检测所有ViewResolver bean.
	 * 否则, 只需要一个名为"viewResolver"的bean.
	 * <p>默认为 true.
	 * 如果希望此portlet使用单个ViewResolver, 请关闭此功能, 尽管在上下文中定义了多个ViewResolver bean.
	 */
	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
		this.detectAllViewResolvers = detectAllViewResolvers;
	}

	/**
	 * 设置是否通过会话属性将在操作阶段期间抛出的异常转发到渲染阶段.
	 * <p>默认为 true. 如果希望portlet容器为操作请求提供即时异常处理, 请将其关闭.
	 */
	public void setForwardActionException(boolean forwardActionException) {
		this.forwardActionException = forwardActionException;
	}

	/**
	 * 设置是否通过会话属性将在事件阶段期间抛出的异常转发到渲染阶段.
	 * <p>默认为 false.
	 * 如果希望{@link DispatcherPortlet}将异常转发到渲染阶段, 请启用此功能,
	 * 类似于默认情况下{@link #setForwardActionException 操作异常}的操作.
	 */
	public void setForwardEventException(boolean forwardEventException) {
		this.forwardEventException = forwardEventException;
	}

	/**
	 * 设置ViewRendererServlet的URL.
	 * 该servlet用于最终渲染portlet应用程序中的所有视图.
	 */
	public void setViewRendererUrl(String viewRendererUrl) {
		this.viewRendererUrl = viewRendererUrl;
	}


	/**
	 * 此实现调用{@link #initStrategies}.
	 */
	@Override
	public void onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * 刷新此portlet使用的策略对象.
	 * <p>可以在子类中重写以初始化其他策略对象.
	 */
	protected void initStrategies(ApplicationContext context) {
		initMultipartResolver(context);
		initHandlerMappings(context);
		initHandlerAdapters(context);
		initHandlerExceptionResolvers(context);
		initViewResolvers(context);
	}

	/**
	 * 初始化此类使用的PortletMultipartResolver.
	 * <p>如果没有在BeanFactory中为此命名空间定义具有给定名称的有效bean, 则不提供multipart处理.
	 */
	private void initMultipartResolver(ApplicationContext context) {
		try {
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, PortletMultipartResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver [" + this.multipartResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate PortletMultipartResolver with name '"	+ MULTIPART_RESOLVER_BEAN_NAME +
						"': no multipart request handling provided");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerMappings.
	 * <p>如果没有在此命名空间的BeanFactory中定义HandlerMapping bean, 默认为PortletModeHandlerMapping.
	 */
	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		if (this.detectAllHandlerMappings) {
			// 在ApplicationContext中查找所有HandlerMappings, 包括祖先上下文.
			Map<String, HandlerMapping> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
					context, HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
				// 按顺序排列HandlerMappings.
				AnnotationAwareOrderComparator.sort(this.handlerMappings);
			}
		}
		else {
			try {
				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
				this.handlerMappings = Collections.singletonList(hm);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略, 稍后会添加一个默认的HandlerMapping.
			}
		}

		// 如果没有找到其他映射, 请确保至少有一个HandlerMapping, 通过注册默认的HandlerMapping.
		if (this.handlerMappings == null) {
			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerMappings found in portlet '" + getPortletName() + "': using default");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerAdapter.
	 * <p>如果没有在此命名空间的BeanFactory中定义HandlerAdapter bean, 默认为SimpleControllerHandlerAdapter.
	 */
	private void initHandlerAdapters(ApplicationContext context) {
		this.handlerAdapters = null;

		if (this.detectAllHandlerAdapters) {
			// 在ApplicationContext中查找所有HandlerAdapter, 包括祖先上下文.
			Map<String, HandlerAdapter> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
					context, HandlerAdapter.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<HandlerAdapter>(matchingBeans.values());
				// 按顺序排列HandlerAdapters.
				AnnotationAwareOrderComparator.sort(this.handlerAdapters);
			}
		}
		else {
			try {
				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
				this.handlerAdapters = Collections.singletonList(ha);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略, 稍后会添加一个默认的HandlerAdapter.
			}
		}

		// 如果没有找到其他适配器, 请通过注册默认的HandlerAdapter来确保至少有一些HandlerAdapter.
		if (this.handlerAdapters == null) {
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerAdapters found in portlet '" + getPortletName() + "': using default");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerExceptionResolver.
	 * <p>如果在此命名空间的BeanFactory中没有使用给定名称定义bean, 则默认没有异常解析器.
	 */
	private void initHandlerExceptionResolvers(ApplicationContext context) {
		this.handlerExceptionResolvers = null;

		if (this.detectAllHandlerExceptionResolvers) {
			// 在ApplicationContext中查找所有HandlerExceptionResolvers, 包括祖先上下文.
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
					context, HandlerExceptionResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<HandlerExceptionResolver>(matchingBeans.values());
				// 按排序顺序保留HandlerExceptionResolvers.
				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
			}
		}
		else {
			try {
				HandlerExceptionResolver her = context.getBean(
						HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				this.handlerExceptionResolvers = Collections.singletonList(her);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, no HandlerExceptionResolver is fine too.
			}
		}

		// 为了保持一致性, 请检查默认的HandlerExceptionResolvers...
		// 通常都没有.
		if (this.handlerExceptionResolvers == null) {
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerExceptionResolvers found in portlet '" + getPortletName() + "': using default");
			}
		}
	}

	/**
	 * 初始化此类使用的ViewResolvers.
	 * <p>如果没有在此命名空间的BeanFactory中定义ViewResolver bean, 默认为InternalResourceViewResolver.
	 */
	private void initViewResolvers(ApplicationContext context) {
		this.viewResolvers = null;

		if (this.detectAllViewResolvers) {
			// 在ApplicationContext中查找所有ViewResolvers, 包括祖先上下文.
			Map<String, ViewResolver> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
					context, ViewResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.values());
				// 按顺序保留ViewResolvers.
				AnnotationAwareOrderComparator.sort(this.viewResolvers);
			}
		}
		else {
			try {
				ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
				this.viewResolvers = Collections.singletonList(vr);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略, 稍后会添加一个默认的ViewResolver.
			}
		}

		// 如果没有找到其他解析器, 通过注册默认的ViewResolver来确保至少有一个ViewResolver.
		if (this.viewResolvers == null) {
			this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No ViewResolvers found in portlet '" + getPortletName() + "': using default");
			}
		}
	}


	/**
	 * 返回给定策略接口的默认策略对象.
	 * <p>默认实现委托给{@link #getDefaultStrategies}, 期望列表中有一个对象.
	 * 
	 * @param context 当前的Portlet ApplicationContext
	 * @param strategyInterface 策略接口
	 * 
	 * @return 相应的策略对象
	 */
	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
		List<T> strategies = getDefaultStrategies(context, strategyInterface);
		if (strategies.size() != 1) {
			throw new BeanInitializationException(
					"DispatcherPortlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
		}
		return strategies.get(0);
	}

	/**
	 * 返回给定策略接口的默认策略对象.
	 * <p>默认实现使用"DispatcherPortlet.properties"文件 (与DispatcherPortlet类在同一个包中)来确定类名.
	 * 它实例化策略对象, 并在必要时满足ApplicationContextAware.
	 * 
	 * @param context 当前的Portlet ApplicationContext
	 * @param strategyInterface 策略接口
	 * 
	 * @return 相应的策略对象
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
		String key = strategyInterface.getName();
		String value = defaultStrategies.getProperty(key);
		if (value != null) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
			List<T> strategies = new ArrayList<T>(classNames.length);
			for (String className : classNames) {
				try {
					Class<?> clazz = ClassUtils.forName(className, DispatcherPortlet.class.getClassLoader());
					Object strategy = createDefaultStrategy(context, clazz);
					strategies.add((T) strategy);
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherPortlet's default strategy class [" + className +
									"] for interface [" + key + "]", ex);
				}
				catch (LinkageError err) {
					throw new BeanInitializationException(
							"Error loading DispatcherPortlet's default strategy class [" + className +
									"] for interface [" + key + "]: problem with class file or dependent class", err);
				}
			}
			return strategies;
		}
		else {
			return new LinkedList<T>();
		}
	}

	/**
	 * 创建默认策略.
	 * <p>默认实现使用
	 * {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean}.
	 * 
	 * @param context 当前的Portlet ApplicationContext
	 * @param clazz 要实例化的策略实现类
	 * 
	 * @return 完全配置的策略实例
	 */
	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
		return context.getAutowireCapableBeanFactory().createBean(clazz);
	}


	/**
	 * 获取此portlet的PortletMultipartResolver.
	 * 
	 * @return 此portlet使用的PortletMultipartResolver, 或{@code null} (表示没有可用的multipart支持)
	 */
	public PortletMultipartResolver getMultipartResolver() {
		return this.multipartResolver;
	}


	/**
	 * 实际调度操作请求到处理器.
	 * <p>将通过按顺序应用portlet的HandlerMappings来获取处理器.
	 * 通过查询portlet安装的HandlerAdapter来获取HandlerAdapter, 以找到支持处理器类的第一个对象.
	 * 
	 * @param request 当前的portlet操作请求
	 * @param response 当前的portlet操作响应
	 * 
	 * @throws Exception 处理失败
	 */
	@Override
	protected void doActionService(ActionRequest request, ActionResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("DispatcherPortlet with name '" + getPortletName() + "' received action request");
		}

		ActionRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		int interceptorIndex = -1;

		try {
			processedRequest = checkMultipart(request);

			// 确定当前请求的处理器.
			mappedHandler = getHandler(processedRequest);
			if (mappedHandler == null || mappedHandler.getHandler() == null) {
				noHandlerFound(processedRequest, response);
				return;
			}

			// 应用注册的拦截器的preHandle方法.
			HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
			if (interceptors != null) {
				for (int i = 0; i < interceptors.length; i++) {
					HandlerInterceptor interceptor = interceptors[i];
					if (!interceptor.preHandleAction(processedRequest, response, mappedHandler.getHandler())) {
						triggerAfterActionCompletion(mappedHandler, interceptorIndex, processedRequest, response, null);
						return;
					}
					interceptorIndex = i;
				}
			}

			// 实际调用处理器.
			HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
			ha.handleAction(processedRequest, response, mappedHandler.getHandler());

			// 成功完成后触发完成后事件.
			triggerAfterActionCompletion(mappedHandler, interceptorIndex, processedRequest, response, null);
		}

		catch (Exception ex) {
			// Trigger after-completion for thrown exception.
			triggerAfterActionCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
			// 将异常转发到要显示的渲染阶段.
			if (this.forwardActionException) {
				try {
					exposeActionException(request, response, ex);
					logger.debug("Caught exception during action phase - forwarding to render phase", ex);
				}
				catch (IllegalStateException ex2) {
					// Probably sendRedirect called... need to rethrow exception immediately.
					throw ex;
				}
			}
			else {
				throw ex;
			}
		}
		catch (Throwable err) {
			PortletException ex =
					new PortletException("Error occured during request processing: " + err.getMessage(), err);
			// Trigger after-completion for thrown exception.
			triggerAfterActionCompletion(mappedHandler, interceptorIndex, processedRequest, response, ex);
			throw ex;
		}

		finally {
			// Clean up any resources used by a multipart request.
			if (processedRequest instanceof MultipartActionRequest && processedRequest != request) {
				this.multipartResolver.cleanupMultipart((MultipartActionRequest) processedRequest);
			}
		}
	}

	/**
	 * 实际调度渲染请求到处理器.
	 * <p>将通过按顺序应用portlet的HandlerMappings来获取处理器.
	 * 将通过查询portlet安装的HandlerAdapter来获取HandlerAdapter, 以找到支持处理器类的第一个对象.
	 * 
	 * @param request 当前portlet渲染请求
	 * @param response 当前portlet渲染响应
	 * 
	 * @throws Exception 处理失败
	 */
	@Override
	protected void doRenderService(RenderRequest request, RenderResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("DispatcherPortlet with name '" + getPortletName() + "' received render request");
		}

		HandlerExecutionChain mappedHandler = null;
		int interceptorIndex = -1;

		try {
			ModelAndView mv;
			try {
				// 确定当前请求的处理器.
				mappedHandler = getHandler(request);
				if (mappedHandler == null || mappedHandler.getHandler() == null) {
					noHandlerFound(request, response);
					return;
				}

				// 应用注册拦截器的preHandle方法.
				HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
				if (interceptors != null) {
					for (int i = 0; i < interceptors.length; i++) {
						HandlerInterceptor interceptor = interceptors[i];
						if (!interceptor.preHandleRender(request, response, mappedHandler.getHandler())) {
							triggerAfterRenderCompletion(mappedHandler, interceptorIndex, request, response, null);
							return;
						}
						interceptorIndex = i;
					}
				}

				// 从操作阶段检查转发的异常
				PortletSession session = request.getPortletSession(false);
				if (session != null) {
					if (request.getParameter(ACTION_EXCEPTION_RENDER_PARAMETER) != null) {
						Exception ex = (Exception) session.getAttribute(ACTION_EXCEPTION_SESSION_ATTRIBUTE);
						if (ex != null) {
							logger.debug("Render phase found exception caught during action phase - rethrowing it");
							throw ex;
						}
					}
					else {
						session.removeAttribute(ACTION_EXCEPTION_SESSION_ATTRIBUTE);
					}
				}

				// 实际调用处理器.
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
				mv = ha.handleRender(request, response, mappedHandler.getHandler());

				// 应用注册的拦截器的postHandle方法.
				if (interceptors != null) {
					for (int i = interceptors.length - 1; i >= 0; i--) {
						HandlerInterceptor interceptor = interceptors[i];
						interceptor.postHandleRender(request, response, mappedHandler.getHandler(), mv);
					}
				}
			}
			catch (ModelAndViewDefiningException ex) {
				logger.debug("ModelAndViewDefiningException encountered", ex);
				mv = ex.getModelAndView();
			}
			catch (Exception ex) {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, ex);
			}

			// 处理器是否返回要渲染的视图?
			if (mv != null && !mv.isEmpty()) {
				render(mv, request, response);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Null ModelAndView returned to DispatcherPortlet with name '" +
							getPortletName() + "': assuming HandlerAdapter completed request handling");
				}
			}

			// Trigger after-completion for successful outcome.
			triggerAfterRenderCompletion(mappedHandler, interceptorIndex, request, response, null);
		}

		catch (Exception ex) {
			// Trigger after-completion for thrown exception.
			triggerAfterRenderCompletion(mappedHandler, interceptorIndex, request, response, ex);
			throw ex;
		}
		catch (Throwable err) {
			PortletException ex =
					new PortletException("Error occured during request processing: " + err.getMessage(), err);
			// Trigger after-completion for thrown exception.
			triggerAfterRenderCompletion(mappedHandler, interceptorIndex, request, response, ex);
			throw ex;
		}
	}

	/**
	 * 实际调度资源请求到处理器.
	 * <p>将通过按顺序应用portlet的HandlerMappings来获取处理器.
	 * 将通过查询portlet安装的HandlerAdapter来获取HandlerAdapter, 以找到支持处理器类的第一个对象.
	 * 
	 * @param request 当前portlet渲染请求
	 * @param response 当前portlet渲染响应
	 * 
	 * @throws Exception 处理失败
	 */
	@Override
	protected void doResourceService(ResourceRequest request, ResourceResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("DispatcherPortlet with name '" + getPortletName() + "' received resource request");
		}

		HandlerExecutionChain mappedHandler = null;
		int interceptorIndex = -1;

		try {
			ModelAndView mv;
			try {
				// 确定当前请求的处理器.
				mappedHandler = getHandler(request);
				if (mappedHandler == null || mappedHandler.getHandler() == null) {
					noHandlerFound(request, response);
					return;
				}

				// 应用注册的拦截器的preHandle方法.
				HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
				if (interceptors != null) {
					for (int i = 0; i < interceptors.length; i++) {
						HandlerInterceptor interceptor = interceptors[i];
						if (!interceptor.preHandleResource(request, response, mappedHandler.getHandler())) {
							triggerAfterResourceCompletion(mappedHandler, interceptorIndex, request, response, null);
							return;
						}
						interceptorIndex = i;
					}
				}

				// 实际调用处理器.
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
				mv = ha.handleResource(request, response, mappedHandler.getHandler());

				// 应用注册的拦截器的postHandle方法.
				if (interceptors != null) {
					for (int i = interceptors.length - 1; i >= 0; i--) {
						HandlerInterceptor interceptor = interceptors[i];
						interceptor.postHandleResource(request, response, mappedHandler.getHandler(), mv);
					}
				}
			}
			catch (ModelAndViewDefiningException ex) {
				logger.debug("ModelAndViewDefiningException encountered", ex);
				mv = ex.getModelAndView();
			}
			catch (Exception ex) {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, ex);
			}

			// Did the handler return a view to render?
			if (mv != null && !mv.isEmpty()) {
				render(mv, request, response);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Null ModelAndView returned to DispatcherPortlet with name '" +
							getPortletName() + "': assuming HandlerAdapter completed request handling");
				}
			}

			// Trigger after-completion for successful outcome.
			triggerAfterResourceCompletion(mappedHandler, interceptorIndex, request, response, null);
		}

		catch (Exception ex) {
			// Trigger after-completion for thrown exception.
			triggerAfterResourceCompletion(mappedHandler, interceptorIndex, request, response, ex);
			throw ex;
		}
		catch (Throwable err) {
			PortletException ex =
					new PortletException("Error occured during request processing: " + err.getMessage(), err);
			// Trigger after-completion for thrown exception.
			triggerAfterResourceCompletion(mappedHandler, interceptorIndex, request, response, ex);
			throw ex;
		}
	}

	/**
	 * 实际调度事件请求到处理器.
	 * <p>将通过按顺序应用portlet的HandlerMappings来获取处理器.
	 * 将通过查询portlet安装的HandlerAdapter来获取HandlerAdapter, 以找到支持处理器类的第一个对象.
	 * 
	 * @param request 当前的portlet操作请求
	 * @param response 当前portlet操作响应
	 * 
	 * @throws Exception 处理失败
	 */
	@Override
	protected void doEventService(EventRequest request, EventResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("DispatcherPortlet with name '" + getPortletName() + "' received action request");
		}

		HandlerExecutionChain mappedHandler = null;
		int interceptorIndex = -1;

		try {
			// 确定当前请求的处理器.
			mappedHandler = getHandler(request);
			if (mappedHandler == null || mappedHandler.getHandler() == null) {
				noHandlerFound(request, response);
				return;
			}

			// 应用注册的拦截器的preHandle方法.
			HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
			if (interceptors != null) {
				for (int i = 0; i < interceptors.length; i++) {
					HandlerInterceptor interceptor = interceptors[i];
					if (!interceptor.preHandleEvent(request, response, mappedHandler.getHandler())) {
						triggerAfterEventCompletion(mappedHandler, interceptorIndex, request, response, null);
						return;
					}
					interceptorIndex = i;
				}
			}

			// 实际调用处理器.
			HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
			ha.handleEvent(request, response, mappedHandler.getHandler());

			// Trigger after-completion for successful outcome.
			triggerAfterEventCompletion(mappedHandler, interceptorIndex, request, response, null);
		}

		catch (Exception ex) {
			// Trigger after-completion for thrown exception.
			triggerAfterEventCompletion(mappedHandler, interceptorIndex, request, response, ex);
			// 将异常转发到要显示的渲染阶段.
			if (this.forwardEventException) {
				try {
					exposeActionException(request, response, ex);
					logger.debug("Caught exception during event phase - forwarding to render phase", ex);
				}
				catch (IllegalStateException ex2) {
					// Probably sendRedirect called... need to rethrow exception immediately.
					throw ex;
				}
			}
			else {
				throw ex;
			}
		}
		catch (Throwable err) {
			PortletException ex =
					new PortletException("Error occured during request processing: " + err.getMessage(), err);
			// Trigger after-completion for thrown exception.
			triggerAfterEventCompletion(mappedHandler, interceptorIndex, request, response, ex);
			throw ex;
		}
	}


	/**
	 * 将请求转换为multipart请求, 并使multipart解析器可用.
	 * 如果未设置multipart解析器, 只需使用现有请求.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 已处理的请求 (必要时multipart包装器)
	 */
	protected ActionRequest checkMultipart(ActionRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (request instanceof MultipartActionRequest) {
				logger.debug("Request is already a MultipartActionRequest - probably in a forward");
			}
			else {
				return this.multipartResolver.resolveMultipart(request);
			}
		}
		// 返回原始请求.
		return request;
	}

	/**
	 * 返回此请求的HandlerExecutionChain.
	 * 按顺序尝试所有处理器映射.
	 * 
	 * @param request 当前的portlet请求
	 * 
	 * @return HandlerExecutionChain, 或null
	 */
	protected HandlerExecutionChain getHandler(PortletRequest request) throws Exception {
		for (HandlerMapping hm : this.handlerMappings) {
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Testing handler map [" + hm + "] in DispatcherPortlet with name '" + getPortletName() + "'");
			}
			HandlerExecutionChain handler = hm.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

	/**
	 * 找不到处理器 -> 抛出适当的异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * 
	 * @throws Exception 如果准备响应失败
	 */
	protected void noHandlerFound(PortletRequest request, PortletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			pageNotFoundLogger.warn("No handler found for current request " +
					"in DispatcherPortlet with name '" + getPortletName() +
					"', mode '" + request.getPortletMode() +
					"', phase '" + request.getAttribute(PortletRequest.LIFECYCLE_PHASE) +
					"', parameters " + StylerUtils.style(request.getParameterMap()));
		}
		throw new NoHandlerFoundException("No handler found for portlet request", request);
	}

	/**
	 * 返回此处理器对象的HandlerAdapter.
	 * 
	 * @param handler 用于查找适配器的处理器对象
	 * 
	 * @throws PortletException 如果没有找到处理器的HandlerAdapter. 这是一个致命的错误.
	 */
	protected HandlerAdapter getHandlerAdapter(Object handler) throws PortletException {
		for (HandlerAdapter ha : this.handlerAdapters) {
			if (logger.isDebugEnabled()) {
				logger.debug("Testing handler adapter [" + ha + "]");
			}
			if (ha.supports(handler)) {
				return ha;
			}
		}
		throw new PortletException("No adapter for handler [" + handler +
				"]: Does your handler implement a supported interface like Controller?");
	}

	/**
	 * 将给定的操作异常暴露给给定的响应.
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * @param ex 操作异常 (也可能来自事件阶段)
	 */
	protected void exposeActionException(PortletRequest request, StateAwareResponse response, Exception ex) {
		// 复制所有参数, 除非在操作处理器中重写.
		Enumeration<String> paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			String[] paramValues = request.getParameterValues(paramName);
			if (paramValues != null && !response.getRenderParameterMap().containsKey(paramName)) {
				response.setRenderParameter(paramName, paramValues);
			}
		}
		response.setRenderParameter(ACTION_EXCEPTION_RENDER_PARAMETER, ex.toString());
		request.getPortletSession().setAttribute(ACTION_EXCEPTION_SESSION_ATTRIBUTE, ex);
	}


	/**
	 * 渲染给定的ModelAndView. 这是处理请求的最后阶段.
	 * 它可能涉及按名称解析视图.
	 * 
	 * @param mv 要渲染的ModelAndView
	 * @param request 当前portlet渲染请求
	 * @param response 当前portlet渲染响应
	 * 
	 * @throws Exception 如果渲染视图有问题
	 */
	protected void render(ModelAndView mv, PortletRequest request, MimeResponse response) throws Exception {
		View view;
		if (mv.isReference()) {
			// 需要解析视图名称.
			view = resolveViewName(mv.getViewName(), mv.getModelInternal(), request);
			if (view == null) {
				throw new PortletException("Could not resolve view with name '" + mv.getViewName() +
						"' in portlet with name '" + getPortletName() + "'");
			}
		}
		else {
			// 无需查找: ModelAndView对象包含实际的View对象.
			Object viewObject = mv.getView();
			if (viewObject == null) {
				throw new PortletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in portlet with name '" + getPortletName() + "'");
			}
			if (!(viewObject instanceof View)) {
				throw new PortletException(
						"View object [" + viewObject + "] is not an instance of [org.springframework.web.servlet.View] - " +
						"DispatcherPortlet does not support any other view types");
			}
			view = (View) viewObject;
		}

		// 在响应上设置内容类型.
		// Portlet规范要求在RenderResponse上设置内容类型; 让View在ServletResponse上设置它是不够的.
		if (response.getContentType() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Portlet response content type already set to [" + response.getContentType() + "]");
			}
		}
		else {
			// 尚未指定Portlet内容类型 -> 使用视图确定的类型.
			String contentType = view.getContentType();
			if (contentType != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting portlet response content type to view-determined type [" + contentType + "]");
				}
				response.setContentType(contentType);
			}
		}

		doRender(view, mv.getModelInternal(), request, response);
	}

	/**
	 * 将给定的视图名称解析为View对象 (要渲染的).
	 * <p>默认实现会询问此调度器的所有ViewResolvers.
	 * 可以根据特定模型属性或请求参数覆盖自定义解析策略.
	 * 
	 * @param viewName 要解析的视图的名称
	 * @param model 要传递给视图的模型
	 * @param request 当前portlet渲染请求
	 * 
	 * @return View 对象, 或null
	 * @throws Exception 如果视图无法解析 (通常在创建实际View对象时出现问题)
	 */
	protected View resolveViewName(String viewName, Map<String, ?> model, PortletRequest request) throws Exception {
		for (ViewResolver viewResolver : this.viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, request.getLocale());
			if (view != null) {
				return view;
			}
		}
		return null;
	}

	/**
	 * 实际渲染给定的视图.
	 * <p>默认实现委托给
	 * {@link org.springframework.web.servlet.ViewRendererServlet}.
	 * 
	 * @param view 要渲染的视图
	 * @param model 关联的模型
	 * @param request 当前portlet渲染/资源请求
	 * @param response 当前portlet渲染/资源响应
	 * 
	 * @throws Exception 如果渲染视图有问题
	 */
	protected void doRender(View view, Map<String, ?> model, PortletRequest request, MimeResponse response) throws Exception {
		// 将Portlet ApplicationContext公开给视图对象.
		request.setAttribute(ViewRendererServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, getPortletApplicationContext());

		// ViewRendererServlet需要这些属性.
		request.setAttribute(ViewRendererServlet.VIEW_ATTRIBUTE, view);
		request.setAttribute(ViewRendererServlet.MODEL_ATTRIBUTE, model);

		// 在渲染/资源响应中包含视图的内容.
		doDispatch(getPortletContext().getRequestDispatcher(this.viewRendererUrl), request, response);
	}

	/**
	 * 在给定的PortletRequestDispatcher上执行调度.
	 * <p>默认实现使用转发资源请求和包含渲染请求.
	 * 
	 * @param dispatcher 要使用的PortletRequestDispatcher
	 * @param request 当前portlet渲染/资源请求
	 * @param response 当前portlet渲染/资源响应
	 * 
	 * @throws Exception 如果执行调度时出现问题
	 */
	protected void doDispatch(PortletRequestDispatcher dispatcher, PortletRequest request, MimeResponse response)
			throws Exception {

		// 通常, 更喜欢转发资源, 以便在目标资源中获得完整的Servlet API支持 (e.g. 在uPortal上).
		// 但是, 在Liferay上, 资源转发显示一个空白页面, 因此不得不使用包含...
		if (PortletRequest.RESOURCE_PHASE.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE)) &&
				!dispatcher.getClass().getName().startsWith("com.liferay")) {
			dispatcher.forward(request, response);
		}
		else {
			dispatcher.include(request, response);
		}
	}


	/**
	 * 通过注册的HandlerExceptionResolvers确定错误ModelAndView.
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * @param handler 执行的处理器, 如果在异常时没有选择, 则返回null (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应的ModelAndView
	 * @throws Exception 如果没有找到错误ModelAndView
	 */
	protected ModelAndView processHandlerException(
			RenderRequest request, RenderResponse response, Object handler, Exception ex)
			throws Exception {

		ModelAndView exMv = null;
		for (Iterator<HandlerExceptionResolver> it = this.handlerExceptionResolvers.iterator(); exMv == null && it.hasNext();) {
			HandlerExceptionResolver resolver = it.next();
			exMv = resolver.resolveException(request, response, handler, ex);
		}
		if (exMv != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("HandlerExceptionResolver returned ModelAndView [" + exMv + "] for exception");
			}
			logger.warn("Handler execution resulted in exception - forwarding to resolved error view", ex);
			return exMv;
		}
		else {
			throw ex;
		}
	}

	/**
	 * 通过注册的HandlerExceptionResolvers确定错误ModelAndView.
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * @param handler 执行的处理器, 如果在异常时没有选择, 则返回null (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应的ModelAndView
	 * @throws Exception 如果没有找到错误ModelAndView
	 */
	protected ModelAndView processHandlerException(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex)
			throws Exception {

		ModelAndView exMv = null;
		for (Iterator<HandlerExceptionResolver> it = this.handlerExceptionResolvers.iterator(); exMv == null && it.hasNext();) {
			HandlerExceptionResolver resolver = it.next();
			exMv = resolver.resolveException(request, response, handler, ex);
		}
		if (exMv != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("HandlerExceptionResolver returned ModelAndView [" + exMv + "] for exception");
			}
			logger.warn("Handler execution resulted in exception - forwarding to resolved error view", ex);
			return exMv;
		}
		else {
			throw ex;
		}
	}

	/**
	 * 在映射的HandlerInterceptors上触发afterCompletion回调.
	 * 只需为preHandle调用成功完成并返回true的所有拦截器调用afterCompletion.
	 * 
	 * @param mappedHandler 映射的HandlerExecutionChain
	 * @param interceptorIndex 成功完成的最后一个拦截器的索引
	 * @param ex 处理器执行时抛出异常, 或null
	 */
	private void triggerAfterActionCompletion(HandlerExecutionChain mappedHandler, int interceptorIndex,
			ActionRequest request, ActionResponse response, Exception ex)
			throws Exception {

		// 应用注册的拦截器的afterCompletion方法.
		if (mappedHandler != null) {
			HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
			if (interceptors != null) {
				for (int i = interceptorIndex; i >= 0; i--) {
					HandlerInterceptor interceptor = interceptors[i];
					try {
						interceptor.afterActionCompletion(request, response, mappedHandler.getHandler(), ex);
					}
					catch (Throwable ex2) {
						logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
					}
				}
			}
		}
	}

	/**
	 * 在映射的HandlerInterceptors上触发afterCompletion回调.
	 * 只需为preHandle调用成功完成并返回true的所有拦截器调用afterCompletion.
	 * 
	 * @param mappedHandler 映射的HandlerExecutionChain
	 * @param interceptorIndex 成功完成的最后一个拦截器的索引
	 * @param ex 处理器执行时抛出异常, 或null
	 */
	private void triggerAfterRenderCompletion(HandlerExecutionChain mappedHandler, int interceptorIndex,
			RenderRequest request, RenderResponse response, Exception ex)
			throws Exception {

		// 应用注册的拦截器的afterCompletion方法.
		if (mappedHandler != null) {
			HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
			if (interceptors != null) {
				for (int i = interceptorIndex; i >= 0; i--) {
					HandlerInterceptor interceptor = interceptors[i];
					try {
						interceptor.afterRenderCompletion(request, response, mappedHandler.getHandler(), ex);
					}
					catch (Throwable ex2) {
						logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
					}
				}
			}
		}
	}

	/**
	 * 在映射的HandlerInterceptors上触发afterCompletion回调.
	 * 只需为preHandle调用成功完成并返回true的所有拦截器调用afterCompletion.
	 * 
	 * @param mappedHandler 映射的HandlerExecutionChain
	 * @param interceptorIndex 成功完成的最后一个拦截器的索引
	 * @param ex 处理器执行时抛出异常, 或null
	 */
	private void triggerAfterResourceCompletion(HandlerExecutionChain mappedHandler, int interceptorIndex,
			ResourceRequest request, ResourceResponse response, Exception ex)
			throws Exception {

		// 应用注册的拦截器的afterCompletion方法.
		if (mappedHandler != null) {
			HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
			if (interceptors != null) {
				for (int i = interceptorIndex; i >= 0; i--) {
					HandlerInterceptor interceptor = interceptors[i];
					try {
						interceptor.afterResourceCompletion(request, response, mappedHandler.getHandler(), ex);
					}
					catch (Throwable ex2) {
						logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
					}
				}
			}
		}
	}

	/**
	 * 在映射的HandlerInterceptors上触发afterCompletion回调.
	 * 只需为preHandle调用成功完成并返回true的所有拦截器调用afterCompletion.
	 * 
	 * @param mappedHandler 映射的HandlerExecutionChain
	 * @param interceptorIndex 成功完成的最后一个拦截器的索引
	 * @param ex 处理器执行时抛出异常, 或null
	 */
	private void triggerAfterEventCompletion(HandlerExecutionChain mappedHandler, int interceptorIndex,
			EventRequest request, EventResponse response, Exception ex)
			throws Exception {

		// 应用注册的拦截器的afterCompletion方法.
		if (mappedHandler != null) {
			HandlerInterceptor[] interceptors = mappedHandler.getInterceptors();
			if (interceptors != null) {
				for (int i = interceptorIndex; i >= 0; i--) {
					HandlerInterceptor interceptor = interceptors[i];
					try {
						interceptor.afterEventCompletion(request, response, mappedHandler.getHandler(), ex);
					}
					catch (Throwable ex2) {
						logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
					}
				}
			}
		}
	}
}
