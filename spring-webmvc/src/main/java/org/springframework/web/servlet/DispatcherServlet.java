package org.springframework.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * HTTP请求处理器/控制器的中央调度器, e.g. 用于Web UI控制器或基于HTTP的远程服务导出器.
 * 调度到已注册的处理器以处理Web请求, 提供方便的映射和异常处理工具.
 *
 * <p>这个servlet非常灵活: 它可以与几乎任何工作流一起使用, 并安装适当的适配器类.
 * 它提供以下功能, 使其与其他请求驱动的Web MVC框架区别开来:
 *
 * <ul>
 * <li>它基于JavaBeans配置机制.
 *
 * <li>它可以使用任何{@link HandlerMapping}实现 - 预构建或作为应用程序的一部分提供 - 来控制对处理器对象的请求路由.
 * 默认是{@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * 和{@link org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping}.
 * HandlerMapping对象可以在servlet的应用程序上下文中定义为bean, 实现HandlerMapping接口, 覆盖默认的HandlerMapping.
 * HandlerMappings可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>它可以使用任何{@link HandlerAdapter}; 这允许使用任何处理器接口.
 * 默认适配器是{@link org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter},
 * {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter},
 * 分别用于Spring的{@link org.springframework.web.HttpRequestHandler}
 * 和{@link org.springframework.web.servlet.mvc.Controller}接口.
 * 默认{@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter}也将被注册.
 * HandlerAdapter对象可以作为bean添加到应用程序上下文中, 覆盖默认的HandlerAdapter.
 * 与HandlerMappings一样, HandlerAdapters可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>可以通过{@link HandlerExceptionResolver}指定调度器的异常解析策略, 例如将某些异常映射到错误页面.
 * 默认为{@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver},
 * {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver},
 * 和{@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}.
 * 可以通过应用程序上下文覆盖这些HandlerExceptionResolvers.
 * HandlerExceptionResolver可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>可以通过{@link ViewResolver}实现指定其视图解析策略, 将符号视图名称解析为View对象.
 * 默认为{@link org.springframework.web.servlet.view.InternalResourceViewResolver}.
 * ViewResolver对象可以作为bean添加到应用程序上下文中, 覆盖默认的ViewResolver.
 * ViewResolvers可以被赋予任何bean名称 (它们按类型测试).
 *
 * <li>如果用户未提供{@link View}或视图名称, 则配置的{@link RequestToViewNameTranslator}会将当前请求转换为视图名称.
 * 相应的bean名称是"viewNameTranslator"; 默认为
 * {@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}.
 *
 * <li>调度器解析multipart请求的策略由{@link org.springframework.web.multipart.MultipartResolver}实现确定.
 * 包括Apache Commons FileUpload和Servlet 3的实现; 典型的选择是
 * {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}.
 * MultipartResolver bean名称是"multipartResolver"; 默认无.
 *
 * <li>其语言环境解析策略由{@link LocaleResolver}确定. 开箱即用的实现通过HTTP accept header, cookie, 或会话工作.
 * LocaleResolver bean名称是"localeResolver"; 默认为
 * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}.
 *
 * <li>其主题解析策略由{@link ThemeResolver}决定. 包括固定主题以及cookie和会话存储的实现.
 * ThemeResolver bean名称是"themeResolver"; 默认为
 * {@link org.springframework.web.servlet.theme.FixedThemeResolver}.
 * </ul>
 *
 * <p><b>NOTE: 只有在调度器中存在相应的{@code HandlerMapping}(用于类型级注释)
 * 和/或{@code HandlerAdapter}(用于方法级注释)时, 才会处理{@code @RequestMapping}注解.</b>
 * 默认情况下就是这种情况. 但是, 如果要定义自定义{@code HandlerMappings}或{@code HandlerAdapters},
 * 则需要确保定义相应的自定义{@code DefaultAnnotationHandlerMapping}和/或{@code AnnotationMethodHandlerAdapter}
 *  - 前提是打算使用{@code @RequestMapping}.
 *
 * <p><b>Web应用程序可以定义任意数量的DispatcherServlet.</b>
 * 每个servlet都在自己的命名空间中运行, 使用映射, 处理器等加载自己的应用程序上下文.
 * 只有{@link org.springframework.web.context.ContextLoaderListener}加载的根应用程序上下文将被共享.
 *
 * <p>从Spring 3.1开始, {@code DispatcherServlet}现在可以注入Web应用程序上下文, 而不是在内部创建自己的.
 * 这在Servlet 3.0+环境中很有用, 它支持servlet实例的编程注册.
 * 有关详细信息, 请参阅{@link #DispatcherServlet(WebApplicationContext)} javadoc.
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

	/** 此命名空间的Bean工厂中的MultipartResolver对象的已知名称. */
	public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

	/** 此命名空间的Bean工厂中LocaleResolver对象的已知名称. */
	public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

	/** 此命名空间的Bean工厂中ThemeResolver对象的已知名称. */
	public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

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
	 * 此命名空间的Bean工厂中RequestToViewNameTranslator对象的已知名称.
	 */
	public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

	/**
	 * 此命名空间的Bean工厂中的ViewResolver对象的已知名称.
	 * 仅在"detectAllViewResolvers"关闭时使用.
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

	/**
	 * 此命名空间的Bean工厂中FlashMapManager对象的已知名称.
	 */
	public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

	/**
	 * 保存当前Web应用程序上下文的请求属性.
	 * 否则, 只有标签等可以获得全局Web应用程序上下文.
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

	/**
	 * 用于保存当前LocaleResolver的请求属性, 可通过视图检索.
	 */
	public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

	/**
	 * 用于保存当前ThemeResolver的请求属性, 可通过视图检索.
	 */
	public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

	/**
	 * 用于保存当前ThemeSource的请求属性, 可通过视图检索.
	 */
	public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

	/**
	 * 请求属性的名称, 其中包含只读的{@code Map<String,?>}, 该Map中包含前一个请求保存的"input" flash属性.
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * 请求属性的名称, 用于保存"output" {@link FlashMap}, 该Map中包含要为后续请求保存的属性.
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * 保存{@link FlashMapManager}的请求属性的名称.
	 */
	public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

	/**
	 * 暴露使用{@link HandlerExceptionResolver}解析但未呈现给视图的异常的请求属性的名称 (e.g. 设置状态码).
	 */
	public static final String EXCEPTION_ATTRIBUTE = DispatcherServlet.class.getName() + ".EXCEPTION";

	/** 未找到请求的映射处理器时要使用的日志类别. */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 定义DispatcherServlet的默认策略名称的类路径资源的名称 (相对于DispatcherServlet类).
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";

	/**
	 * DispatcherServlet的默认策略属性的通用前缀.
	 */
	private static final String DEFAULT_STRATEGIES_PREFIX = "org.springframework.web.servlet";

	/** 未找到请求的映射处理器时, 使用的其他记录器. */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	private static final Properties defaultStrategies;

	static {
		// 从属性文件加载默认策略实现.
		// 这当前是严格内部的, 不应由应用程序开发人员自定义.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
		}
	}

	/** 检测所有HandlerMappings, 或只是期望"handlerMapping" bean? */
	private boolean detectAllHandlerMappings = true;

	/** 检测所有 HandlerAdapters, 或只是期望"handlerAdapter" bean? */
	private boolean detectAllHandlerAdapters = true;

	/** 检测所有 HandlerExceptionResolvers, 或只是期望"handlerExceptionResolver" bean? */
	private boolean detectAllHandlerExceptionResolvers = true;

	/** 检测所有 ViewResolvers, 或只是期望"viewResolver" bean? */
	private boolean detectAllViewResolvers = true;

	/** 如果没有找到Handler来处理此请求, 则抛出NoHandlerFoundException? **/
	private boolean throwExceptionIfNoHandlerFound = false;

	/** 在include请求后执行请求属性的清理? */
	private boolean cleanupAfterInclude = true;

	/** 此servlet使用的MultipartResolver */
	private MultipartResolver multipartResolver;

	/** 此servlet使用的LocaleResolver */
	private LocaleResolver localeResolver;

	/** 此servlet使用的ThemeResolver */
	private ThemeResolver themeResolver;

	/** 此servlet使用的HandlerMapping */
	private List<HandlerMapping> handlerMappings;

	/** 此servlet使用的HandlerAdapter */
	private List<HandlerAdapter> handlerAdapters;

	/** 此servlet使用的HandlerExceptionResolver */
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	/** 此servlet使用的RequestToViewNameTranslator */
	private RequestToViewNameTranslator viewNameTranslator;

	/** 此servlet使用的FlashMapManager */
	private FlashMapManager flashMapManager;

	/** 此servlet使用的ViewResolver */
	private List<ViewResolver> viewResolvers;


	/**
	 * 创建一个新的{@code DispatcherServlet},
	 * 它将根据servlet init-params提供的默认值和值创建自己的内部Web应用程序上下文.
	 * 通常在Servlet 2.5或更早版本的环境中使用,
	 * 其中servlet注册的唯一选项是通过{@code web.xml}, 这需要使用no-arg构造函数.
	 * <p>调用{@link #setContextConfigLocation} (init-param 'contextConfigLocation')
	 * 将指定{@linkplain #DEFAULT_CONTEXT_CLASS default XmlWebApplicationContext}将加载哪些XML文件
	 * <p>调用{@link #setContextClass} (init-param 'contextClass')会覆盖默认的{@code XmlWebApplicationContext},
	 * 并允许指定替代类, 例如{@code AnnotationConfigWebApplicationContext}.
	 * <p>调用{@link #setContextInitializerClasses} (init-param 'contextInitializerClasses')
	 * 指示在 refresh() 之前应使用哪些{@code ApplicationContextInitializer}类来进一步配置内部应用程序上下文.
	 */
	public DispatcherServlet() {
		super();
		setDispatchOptionsRequest(true);
	}

	/**
	 * 这个构造函数在Servlet 3.0+环境中很有用, 在这些环境中,
	 * 通过{@link ServletContext#addServlet} API可以实现基于实例的servlet注册.
	 * <p>使用此构造函数表示将忽略以下属性/ init-param:
	 * <ul>
	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
	 * </ul>
	 * <p>给定的Web应用程序上下文不确定是否{@linkplain ConfigurableApplicationContext#refresh() 刷新}.
	 * 如果<strong>还没有</strong>刷新 (建议的方法), 则会发生以下情况:
	 * <ul>
	 * <li>如果给定的上下文还没有{@linkplain ConfigurableApplicationContext#setParent parent},
	 * 则根应用程序上下文将被设置为父项.</li>
	 * <li>如果尚未为给定的上下文分配{@linkplain ConfigurableApplicationContext#setId id}, 则会为其分配一个</li>
	 * <li>{@code ServletContext}和{@code ServletConfig}对象将被委托给应用程序上下文</li>
	 * <li>将调用{@link #postProcessWebApplicationContext}</li>
	 * <li>将应用通过"contextInitializerClasses" init-param 或通过{@link #setContextInitializers}属性
	 * 指定的任何{@code ApplicationContextInitializer}.</li>
	 * <li>如果上下文实现了{@link ConfigurableApplicationContext},
	 * 将调用{@link ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已经刷新, 则假设用户已根据其特定需求执行 (或不执行)这些操作, 则不会发生上述任何情况.
	 * <p>有关用法示例, 请参阅{@link org.springframework.web.WebApplicationInitializer}.
	 * 
	 * @param webApplicationContext 要使用的上下文
	 */
	public DispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
		setDispatchOptionsRequest(true);
	}


	/**
	 * 设置是否在此servlet的上下文中检测所有HandlerMapping bean.
	 * 否则, 只需要一个名为"handlerMapping"的bean.
	 * <p>默认为"true".
	 * 如果希望此servlet使用单个HandlerMapping, 关闭此功能, 尽管在上下文中定义了多个HandlerMapping bean.
	 */
	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
		this.detectAllHandlerMappings = detectAllHandlerMappings;
	}

	/**
	 * 设置是否在此servlet的上下文中检测所有HandlerAdapter bean.
	 * 否则, 只需要一个名为"handlerAdapter"的bean.
	 * <p>默认为"true".
	 * 如果希望此servlet使用单个HandlerAdapter, 关闭此功能, 尽管在上下文中定义了多个HandlerAdapter bean.
	 */
	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
	}

	/**
	 * 设置是否在此servlet的上下文中检测所有HandlerExceptionResolver bean.
	 * 否则, 只需要一个名为"handlerExceptionResolver"的bean.
	 * <p>默认为"true".
	 * 如果希望此servlet使用单个HandlerExceptionResolver, 请关闭此功能,
	 * 尽管在上下文中定义了多个HandlerExceptionResolver bean.
	 */
	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
	}

	/**
	 * 设置是否在此servlet的上下文中检测所有ViewResolver bean.
	 * 否则, 只需要一个名为"viewResolver"的bean.
	 * <p>默认为"true".
	 * 如果希望此servlet使用单个ViewResolver, 关闭此项, 尽管在上下文中定义了多个ViewResolver bean.
	 */
	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
		this.detectAllViewResolvers = detectAllViewResolvers;
	}

	/**
	 * 设置是否在未找到此请求的Handler时抛出NoHandlerFoundException.
	 * 然后可以使用HandlerExceptionResolver或{@code @ExceptionHandler}控制器方法捕获此异常.
	 * <p>请注意, 如果使用{@link org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler},
	 * 则请求将始终转发到默认servlet, 并且在这种情况下永远不会抛出NoHandlerFoundException.
	 * <p>默认为 "false", 表示DispatcherServlet通过Servlet响应发送NOT_FOUND错误.
	 */
	public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
		this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
	}

	/**
	 * 设置是否在include请求后执行请求属性的清理, 即在include请求中处理DispatcherServlet后, 是否重置所有请求属性的原始状态.
	 * 否则, 只会重置DispatcherServlet自己的请求属性, 但不会重置JSP的模型属性或视图设置的特殊属性 (例如, JSTL).
	 * <p>默认为"true", 强烈建议使用. 视图不应该依赖于由(动态) include设置的请求属性.
	 * 这允许include的控制器呈现的JSP视图使用任何模型属性, 即使使用与主JSP中相同的名称, 也不会产生副作用.
	 * 仅针对特殊需要关闭此选项, 例如, 故意允许主JSP从include的控制器呈现的JSP视图中访问属性.
	 */
	public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
		this.cleanupAfterInclude = cleanupAfterInclude;
	}


	/**
	 * 此实现调用{@link #initStrategies}.
	 */
	@Override
	protected void onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * 初始化此servlet使用的策略对象.
	 * <p>可以在子类中重写以初始化其他策略对象.
	 */
	protected void initStrategies(ApplicationContext context) {
		initMultipartResolver(context);
		initLocaleResolver(context);
		initThemeResolver(context);
		initHandlerMappings(context);
		initHandlerAdapters(context);
		initHandlerExceptionResolvers(context);
		initRequestToViewNameTranslator(context);
		initViewResolvers(context);
		initFlashMapManager(context);
	}

	/**
	 * 初始化此类使用的MultipartResolver.
	 * <p>如果没有在此命名空间的BeanFactory中定义具有给定名称的bean, 则不提供multipart处理.
	 */
	private void initMultipartResolver(ApplicationContext context) {
		try {
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver [" + this.multipartResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// 默认没有multipart解析器.
			this.multipartResolver = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MultipartResolver with name '" + MULTIPART_RESOLVER_BEAN_NAME +
						"': no multipart request handling provided");
			}
		}
	}

	/**
	 * 初始化此类使用的LocaleResolver.
	 * <p>如果没有在此命名空间的BeanFactory中定义具有给定名称的bean, 则默认为AcceptHeaderLocaleResolver.
	 */
	private void initLocaleResolver(ApplicationContext context) {
		try {
			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using LocaleResolver [" + this.localeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate LocaleResolver with name '" + LOCALE_RESOLVER_BEAN_NAME +
						"': using default [" + this.localeResolver + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的ThemeResolver.
	 * <p>如果没有在此命名空间的BeanFactory中定义具有给定名称的bean, 默认为FixedThemeResolver.
	 */
	private void initThemeResolver(ApplicationContext context) {
		try {
			this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using ThemeResolver [" + this.themeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ThemeResolver with name '" + THEME_RESOLVER_BEAN_NAME +
						"': using default [" + this.themeResolver + "]");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerMapping.
	 * <p>如果没有在此命名空间的BeanFactory定义HandlerMapping bean, 默认为BeanNameUrlHandlerMapping.
	 */
	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		if (this.detectAllHandlerMappings) {
			// 在ApplicationContext中查找所有HandlerMapping, 包括祖先上下文.
			Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
				// 按顺序排列HandlerMapping.
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
				logger.debug("No HandlerMappings found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerAdapter.
	 * <p>如果没有在此命名空间的BeanFactory定义HandlerAdapter bean, 默认为SimpleControllerHandlerAdapter.
	 */
	private void initHandlerAdapters(ApplicationContext context) {
		this.handlerAdapters = null;

		if (this.detectAllHandlerAdapters) {
			// 在ApplicationContext中查找所有HandlerAdapter, 包括祖先上下文.
			Map<String, HandlerAdapter> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<HandlerAdapter>(matchingBeans.values());
				// 按顺序排列HandlerAdapter.
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

		// 如果没有找到其他适配器, 通过注册默认的HandlerAdapter来确保至少有一些HandlerAdapter.
		if (this.handlerAdapters == null) {
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerAdapters found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * 初始化此类使用的HandlerExceptionResolver.
	 * <p>如果在此命名空间的BeanFactory中没有定义给定名称的bean, 则默认没有异常解析器.
	 */
	private void initHandlerExceptionResolvers(ApplicationContext context) {
		this.handlerExceptionResolvers = null;

		if (this.detectAllHandlerExceptionResolvers) {
			// 在ApplicationContext中查找所有HandlerExceptionResolver, 包括祖先上下文.
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<HandlerExceptionResolver>(matchingBeans.values());
				// 按排序顺序保留HandlerExceptionResolver.
				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
			}
		}
		else {
			try {
				HandlerExceptionResolver her =
						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				this.handlerExceptionResolvers = Collections.singletonList(her);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// 忽略, 没有HandlerExceptionResolver也没关系.
			}
		}

		// 如果没有找到其他解析器, 通过注册默认的HandlerExceptionResolver确保至少有一些HandlerExceptionResolver.
		if (this.handlerExceptionResolvers == null) {
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerExceptionResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * 初始化此servlet实例使用的RequestToViewNameTranslator.
	 * <p>如果未配置任何实现, 则默认为DefaultRequestToViewNameTranslator.
	 */
	private void initRequestToViewNameTranslator(ApplicationContext context) {
		try {
			this.viewNameTranslator =
					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using RequestToViewNameTranslator [" + this.viewNameTranslator + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate RequestToViewNameTranslator with name '" +
						REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME + "': using default [" + this.viewNameTranslator +
						"]");
			}
		}
	}

	/**
	 * 初始化此类使用的ViewResolver.
	 * <p>如果没有在此命名空间的BeanFactory中定义ViewResolver bean, 默认为InternalResourceViewResolver.
	 */
	private void initViewResolvers(ApplicationContext context) {
		this.viewResolvers = null;

		if (this.detectAllViewResolvers) {
			// 在ApplicationContext中查找所有ViewResolver, 包括祖先上下文.
			Map<String, ViewResolver> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.values());
				// 按顺序保留ViewResolver.
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
				logger.debug("No ViewResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * 初始化此servlet实例使用的{@link FlashMapManager}.
	 * <p>如果没有配置任何实现, 默认为
	 * {@code org.springframework.web.servlet.support.DefaultFlashMapManager}.
	 */
	private void initFlashMapManager(ApplicationContext context) {
		try {
			this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using FlashMapManager [" + this.flashMapManager + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate FlashMapManager with name '" +
						FLASH_MAP_MANAGER_BEAN_NAME + "': using default [" + this.flashMapManager + "]");
			}
		}
	}

	/**
	 * 返回这个servlet的ThemeSource; 否则返回{@code null}.
	 * <p>默认是将WebApplicationContext作为ThemeSource返回, 前提是它实现了ThemeSource接口.
	 * 
	 * @return the ThemeSource, if any
	 */
	public final ThemeSource getThemeSource() {
		if (getWebApplicationContext() instanceof ThemeSource) {
			return (ThemeSource) getWebApplicationContext();
		}
		else {
			return null;
		}
	}

	/**
	 * 获取此servlet的MultipartResolver.
	 * 
	 * @return 此servlet使用的MultipartResolver, 或{@code null} (表示没有可用的multipart支持)
	 */
	public final MultipartResolver getMultipartResolver() {
		return this.multipartResolver;
	}

	/**
	 * 返回给定策略接口的默认策略对象.
	 * <p>默认实现委托给{@link #getDefaultStrategies}, 期望列表中有一个对象.
	 * 
	 * @param context 当前WebApplicationContext
	 * @param strategyInterface 策略接口
	 * 
	 * @return 相应的策略对象
	 */
	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
		List<T> strategies = getDefaultStrategies(context, strategyInterface);
		if (strategies.size() != 1) {
			throw new BeanInitializationException(
					"DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
		}
		return strategies.get(0);
	}

	/**
	 * 为给定的策略接口创建默认策略对象列表.
	 * <p>默认实现使用"DispatcherServlet.properties"文件 (与DispatcherServlet类在同一个包中) 来确定类名.
	 * 它通过上下文的BeanFactory实例化策略对象.
	 * 
	 * @param context 当前WebApplicationContext
	 * @param strategyInterface 策略接口
	 * 
	 * @return 相应策略对象的列表
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
					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
					Object strategy = createDefaultStrategy(context, clazz);
					strategies.add((T) strategy);
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherServlet's default strategy class [" + className +
									"] for interface [" + key + "]", ex);
				}
				catch (LinkageError err) {
					throw new BeanInitializationException(
							"Error loading DispatcherServlet's default strategy class [" + className +
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
	 * @param context 当前的WebApplicationContext
	 * @param clazz 要实例化的策略实现类
	 * 
	 * @return 完全配置的策略实例
	 */
	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
		return context.getAutowireCapableBeanFactory().createBean(clazz);
	}


	/**
	 * 将DispatcherServlet特定的请求属性公开, 并委托给{@link #doDispatch}以进行实际调度.
	 */
	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
			logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
					" processing " + request.getMethod() + " request for [" + getRequestUri(request) + "]");
		}

		//在include的情况下保留请求属性的快照, 以便能够在include之后恢复原始属性.
		Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			attributesSnapshot = new HashMap<String, Object>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// 使框架对象可供处理器和视图对象使用.
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
		if (inputFlashMap != null) {
			request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
		}
		request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

		try {
			doDispatch(request, response);
		}
		finally {
			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				// 如果是include, 则还原原始属性快照.
				if (attributesSnapshot != null) {
					restoreAttributesAfterInclude(request, attributesSnapshot);
				}
			}
		}
	}

	/**
	 * 处理到处理器的实际调度.
	 * <p>处理器将通过按顺序应用servlet的HandlerMappings来获得.
	 * 将通过查询servlet安装的HandlerAdapter来获取HandlerAdapter, 以找到支持处理器类的第一个.
	 * <p>所有HTTP方法都由此方法处理. 由HandlerAdapter或处理器自行决定哪些方法可以接受.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @throws Exception 在任何处理失败的情况下
	 */
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				processedRequest = checkMultipart(request);
				multipartRequestParsed = (processedRequest != request);

				// 确定当前请求的处理器.
				mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null || mappedHandler.getHandler() == null) {
					noHandlerFound(processedRequest, response);
					return;
				}

				// 确定当前请求的处理器适配器.
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// 处理last-modified header, 如果处理器支持.
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (logger.isDebugEnabled()) {
						logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
					}
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// 实际调用处理器.
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}

				applyDefaultViewName(processedRequest, mv);
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			catch (Throwable err) {
				// 从4.3开始, 处理从处理器方法抛出的错误, 使它们可用于@ExceptionHandler方法和其他场景.
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Throwable err) {
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// 而不是postHandle和afterCompletion
				if (mappedHandler != null) {
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// 清理multipart请求使用的资源.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}

	/**
	 * 是否需要视图名称转换?
	 */
	private void applyDefaultViewName(HttpServletRequest request, ModelAndView mv) throws Exception {
		if (mv != null && !mv.hasView()) {
			mv.setViewName(getDefaultViewName(request));
		}
	}

	/**
	 * 处理处理器选择和处理器调用的结果, 该结果是将被解析为ModelAndView的ModelAndView或Exception.
	 */
	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) throws Exception {

		boolean errorView = false;

		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			}
			else {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// 处理器是否返回要渲染的视图?
		if (mv != null && !mv.wasCleared()) {
			render(mv, request, response);
			if (errorView) {
				WebUtils.clearErrorRequestAttributes(request);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Null ModelAndView returned to DispatcherServlet with name '" + getServletName() +
						"': assuming HandlerAdapter completed request handling");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// 并行处理在转发期间开始
			return;
		}

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}

	/**
	 * 为给定请求构建LocaleContext, 将请求的主要区域设置公开为当前区域设置.
	 * <p>默认实现使用调度器的LocaleResolver来获取当前区域设置, 该区域设置可能在请求期间发生更改.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 相应的LocaleContext
	 */
	@Override
	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
		if (this.localeResolver instanceof LocaleContextResolver) {
			return ((LocaleContextResolver) this.localeResolver).resolveLocaleContext(request);
		}
		else {
			return new LocaleContext() {
				@Override
				public Locale getLocale() {
					return localeResolver.resolveLocale(request);
				}
			};
		}
	}

	/**
	 * 将请求转换为multipart请求, 并使multipart解析器可用.
	 * <p>如果未设置multipart解析器, 只需使用现有请求.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 已处理的请求 (必要时包含multipart包装器)
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
				logger.debug("Request is already a MultipartHttpServletRequest - if not in a forward, " +
						"this typically results from an additional MultipartFilter in web.xml");
			}
			else if (hasMultipartException(request) ) {
				logger.debug("Multipart resolution failed for current request before - " +
						"skipping re-resolution for undisturbed error rendering");
			}
			else {
				try {
					return this.multipartResolver.resolveMultipart(request);
				}
				catch (MultipartException ex) {
					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
						logger.debug("Multipart resolution failed for error dispatch", ex);
						// 使用下面的常规请求句柄继续处理错误分派
					}
					else {
						throw ex;
					}
				}
			}
		}
		// 如果之前没有返回: 返回原始请求.
		return request;
	}

	/**
	 * 检查multipart异常的"javax.servlet.error.exception"属性.
	 */
	private boolean hasMultipartException(HttpServletRequest request) {
		Throwable error = (Throwable) request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
		while (error != null) {
			if (error instanceof MultipartException) {
				return true;
			}
			error = error.getCause();
		}
		return false;
	}

	/**
	 * 清理给定multipart请求使用的任何资源.
	 * 
	 * @param request 当前的HTTP请求
	 */
	protected void cleanupMultipart(HttpServletRequest request) {
		MultipartHttpServletRequest multipartRequest =
				WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
		if (multipartRequest != null) {
			this.multipartResolver.cleanupMultipart(multipartRequest);
		}
	}

	/**
	 * 返回此请求的HandlerExecutionChain.
	 * <p>按顺序尝试所有处理器映射.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return HandlerExecutionChain, 或{@code null} 如果找不到处理器
	 */
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		for (HandlerMapping hm : this.handlerMappings) {
			if (logger.isTraceEnabled()) {
				logger.trace(
						"Testing handler map [" + hm + "] in DispatcherServlet with name '" + getServletName() + "'");
			}
			HandlerExecutionChain handler = hm.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

	/**
	 * 找不到处理器 -> 设置适当的HTTP响应状态.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @throws Exception 如果准备响应失败
	 */
	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			pageNotFoundLogger.warn("No mapping found for HTTP request with URI [" + getRequestUri(request) +
					"] in DispatcherServlet with name '" + getServletName() + "'");
		}
		if (this.throwExceptionIfNoHandlerFound) {
			throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
					new ServletServerHttpRequest(request).getHeaders());
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * 返回此处理器对象的HandlerAdapter.
	 * 
	 * @param handler 用于查找适配器的处理器对象
	 * 
	 * @throws ServletException 如果找不到处理器的HandlerAdapter. 这是一个致命的错误.
	 */
	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		for (HandlerAdapter ha : this.handlerAdapters) {
			if (logger.isTraceEnabled()) {
				logger.trace("Testing handler adapter [" + ha + "]");
			}
			if (ha.supports(handler)) {
				return ha;
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

	/**
	 * 通过注册的HandlerExceptionResolvers确定错误ModelAndView.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应的ModelAndView
	 * @throws Exception 如果没有找到错误ModelAndView
	 */
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		// 检查注册的HandlerExceptionResolvers...
		ModelAndView exMv = null;
		for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
			exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
			if (exMv != null) {
				break;
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// 对于普通错误模型, 可能仍需要视图名称转换...
			if (!exMv.hasView()) {
				exMv.setViewName(getDefaultViewName(request));
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Handler execution resulted in exception - forwarding to resolved error view: " + exMv, ex);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		throw ex;
	}

	/**
	 * 渲染给定的ModelAndView.
	 * <p>这是处理请求的最后阶段. 它可能涉及按名称解析视图.
	 * 
	 * @param mv 要呈现的ModelAndView
	 * @param request 当前的HTTP servlet请求
	 * @param response 当前的HTTP servlet响应
	 * 
	 * @throws ServletException 如果视图丢失或无法解析
	 * @throws Exception 如果渲染视图有问题
	 */
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// 确定请求的区域设置并将其应用于响应.
		Locale locale = this.localeResolver.resolveLocale(request);
		response.setLocale(locale);

		View view;
		if (mv.isReference()) {
			// 需要解析视图名称.
			view = resolveViewName(mv.getViewName(), mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		}
		else {
			// 无需查找: ModelAndView对象包含实际的View对象.
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// 委托给要渲染的View对象.
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering view [" + view + "] in DispatcherServlet with name '" + getServletName() + "'");
		}
		try {
			if (mv.getStatus() != null) {
				response.setStatus(mv.getStatus().value());
			}
			view.render(mv.getModelInternal(), request, response);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "] in DispatcherServlet with name '" +
						getServletName() + "'", ex);
			}
			throw ex;
		}
	}

	/**
	 * 将提供的请求转换为默认视图名称.
	 * 
	 * @param request 当前的HTTP servlet请求
	 * 
	 * @return 视图名称 (如果未找到默认值, 则为{@code null})
	 * @throws Exception 如果视图名称转换失败
	 */
	protected String getDefaultViewName(HttpServletRequest request) throws Exception {
		return this.viewNameTranslator.getViewName(request);
	}

	/**
	 * 将给定的视图名称解析为View对象 (要渲染的).
	 * <p>默认实现会询问此调度器的所有ViewResolver.
	 * 可以根据特定模型属性或请求参数覆盖自定义解析策略.
	 * 
	 * @param viewName 要解析的视图的名称
	 * @param model 要传递给视图的模型
	 * @param locale 当前的语言环境
	 * @param request 当前的HTTP servlet请求
	 * 
	 * @return View对象, 或{@code null}
	 * @throws Exception 如果视图无法解析 (通常在创建实际View对象时出现问题)
	 */
	protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale,
			HttpServletRequest request) throws Exception {

		for (ViewResolver viewResolver : this.viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				return view;
			}
		}
		return null;
	}

	private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	/**
	 * 在include之后恢复请求属性.
	 * 
	 * @param request 当前的HTTP请求
	 * @param attributesSnapshot include之前的请求属性的快照
	 */
	@SuppressWarnings("unchecked")
	private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?,?> attributesSnapshot) {
		// 需要在此处复制到单独的Collection中, 以避免在删除属性时Enumeration的副作用.
		Set<String> attrsToCheck = new HashSet<String>();
		Enumeration<?> attrNames = request.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = (String) attrNames.nextElement();
			if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
				attrsToCheck.add(attrName);
			}
		}

		// 添加可能已删除的属性
		attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());

		// 如果合适, 分别迭代要检查的属性, 恢复原始值或删除属性.
		for (String attrName : attrsToCheck) {
			Object attrValue = attributesSnapshot.get(attrName);
			if (attrValue == null){
				request.removeAttribute(attrName);
			}
			else if (attrValue != request.getAttribute(attrName)) {
				request.setAttribute(attrName, attrValue);
			}
		}
	}

	private static String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return uri;
	}

}
