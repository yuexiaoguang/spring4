package org.springframework.web.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 执行根应用程序上下文的实际初始化工作.
 * 由{@link ContextLoaderListener}调用.
 *
 * <p>在{@code web.xml} context-param级别查找{@link #CONTEXT_CLASS_PARAM "contextClass"}参数,
 * 以指定上下文类类型, 如果未找到回退到{@link org.springframework.web.context.support.XmlWebApplicationContext}.
 * 使用默认的ContextLoader实现, 指定的任何上下文类都需要实现{@link ConfigurableWebApplicationContext}接口.
 *
 * <p>处理{@link #CONFIG_LOCATION_PARAM "contextConfigLocation"} context-param并将其值传递给上下文实例,
 * 将其解析为可能由多个逗号和空格分隔的多个文件路径,
 * e.g. "WEB-INF/applicationContext1.xml, WEB-INF/applicationContext2.xml".
 * 还支持Ant样式的路径模式,
 * e.g. "WEB-INF/*Context.xml,WEB-INF/spring*.xml" or "WEB-INF/&#42;&#42;/*Context.xml".
 * 如果没有明确指定, 则上下文实现应该使用默认位置 (使用 XmlWebApplicationContext: "/WEB-INF/applicationContext.xml").
 *
 * <p>Note: 在多个配置位置的情况下, 后面加载的bean定义将覆盖先前加载的文件中定义的那些,
 * 至少在使用Spring的默认ApplicationContext实现之一时.
 * 这可以用来通过额外的XML文件故意覆盖某些bean定义.
 *
 * <p>除了加载根应用程序上下文之外, 此类还可以选择加载或获取共享父上下文, 并将其挂接到根应用程序上下文.
 * 有关更多信息, 请参阅{@link #loadParentContext(ServletContext)}方法.
 *
 * <p>从Spring 3.1开始, {@code ContextLoader}支持通过
 * {@link #ContextLoader(WebApplicationContext)}构造函数注入根Web应用程序上下文,
 * 允许在Servlet 3.0+环境中进行编程配置.
 * See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
 */
public class ContextLoader {

	/**
	 * 根WebApplicationContext id的配置参数, 用作底层BeanFactory的序列化ID: {@value}
	 */
	public static final String CONTEXT_ID_PARAM = "contextId";

	/**
	 * 可以指定根上下文的配置位置的servlet上下文参数的名称(i.e., {@value}), 否则回退到实现的默认值.
	 */
	public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

	/**
	 * 要使用的根WebApplicationContext实现类的配置参数: {@value}
	 */
	public static final String CONTEXT_CLASS_PARAM = "contextClass";

	/**
	 * 用于初始化根Web应用程序上下文的{@link ApplicationContextInitializer}类的配置参数: {@value}
	 */
	public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

	/**
	 * 用于初始化当前应用程序中的所有Web应用程序上下文的全局{@link ApplicationContextInitializer}类的配置参数: {@value}
	 */
	public static final String GLOBAL_INITIALIZER_CLASSES_PARAM = "globalInitializerClasses";

	/**
	 * 可选的servlet上下文参数 (i.e., "{@code locatorFactorySelector}"),
	 * 仅在使用{@link #loadParentContext(ServletContext servletContext)}的默认实现获取父上下文时使用.
	 * 指定{@link ContextSingletonBeanFactoryLocator#getInstance(String selector)}方法调用中使用的'selector',
	 * 用于获取从中获取父上下文的BeanFactoryLocator实例.
	 * <p>默认为{@code classpath*:beanRefContext.xml},
	 * 匹配应用于{@link ContextSingletonBeanFactoryLocator#getInstance()}方法的默认值.
	 * 在这种情况下, 提供"parentContextKey"参数就足够了.
	 */
	public static final String LOCATOR_FACTORY_SELECTOR_PARAM = "locatorFactorySelector";

	/**
	 * 可选的servlet上下文参数 (i.e., "{@code parentContextKey}"),
	 * 仅在使用{@link #loadParentContext(ServletContext servletContext)}的默认实现获取父上下文时使用.
	 * 指定{@link BeanFactoryLocator#useBeanFactory(String factoryKey)}方法调用中使用的'factoryKey',
	 * 从BeanFactoryLocator实例获取父应用程序上下文.
	 * <p>当依赖于候选工厂引用的默认{@code classpath*:beanRefContext.xml}选择器时,
	 * 提供此"parentContextKey"参数就足够了.
	 */
	public static final String LOCATOR_FACTORY_KEY_PARAM = "parentContextKey";

	/**
	 * 在单个init-param字符串值中, 任何数量的这些字符都被视为多个值之间的分隔符.
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

	/**
	 * 定义ContextLoader的默认策略名称的类路径资源的名称 (相对于ContextLoader类).
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";


	private static final Properties defaultStrategies;

	static {
		// 从属性文件加载默认策略实现.
		// 这当前是严格内部的, 不应由应用程序开发人员自定义.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
		}
	}


	/**
	 * 从(线程上下文) ClassLoader到相应的'当前' WebApplicationContext的Map.
	 */
	private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread =
			new ConcurrentHashMap<ClassLoader, WebApplicationContext>(1);

	/**
	 * '当前' WebApplicationContext, 如果ContextLoader类部署在Web应用程序ClassLoader本身中.
	 */
	private static volatile WebApplicationContext currentContext;


	/**
	 * 此加载器管理的根WebApplicationContext实例.
	 */
	private WebApplicationContext context;

	/**
	 * 通过ContextSingletonBeanFactoryLocator加载父工厂时, 保存BeanFactoryReference.
	 */
	private BeanFactoryReference parentContextRef;

	/** 要应用于上下文的实际ApplicationContextInitializer实例 */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();


	/**
	 * 基于"contextClass"和"contextConfigLocation" servlet context-params创建一个Web应用程序上下文.
	 * 有关每个默认值的详细信息, 请参阅类级文档.
	 * <p>在{@code web.xml}中将{@code ContextLoaderListener}子类声明为{@code <listener>}时,
	 * 通常使用此构造函数, 因为需要使用无参数构造函数.
	 * <p>创建的应用程序上下文将使用属性名称
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}注册到ServletContext中,
	 * 子类可以在容器关闭时调用{@link #closeWebApplicationContext}方法来关闭应用程序上下文.
	 */
	public ContextLoader() {
	}

	/**
	 * 使用给定的应用程序上下文创建一个新的{@code ContextLoader}.
	 * 此构造函数在Servlet 3.0+环境中非常有用, 在这些环境中,
	 * 可以通过{@link ServletContext#addListener} API对基于实例的监听器进行注册.
	 * <p>上下文不确定是否{@linkplain ConfigurableApplicationContext#refresh() 刷新}.
	 * 如果
	 * (a) 它是{@link ConfigurableWebApplicationContext}的实现, 而且
	 * (b) <strong>还没有</strong>刷新 (推荐的方法),
	 * 然后会发生以下情况:
	 * <ul>
	 * <li>如果尚未为给定的上下文分配{@linkplain ConfigurableApplicationContext#setId id}, 则会为其分配一个</li>
	 * <li>{@code ServletContext}和{@code ServletConfig}对象将被委托给应用程序上下文</li>
	 * <li>将调用{@link #customizeContext}</li>
	 * <li>将应用通过"contextInitializerClasses" init-param指定的任何{@link ApplicationContextInitializer}.</li>
	 * <li>将调用{@link ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已经刷新或未实现{@code ConfigurableWebApplicationContext},
	 * 则假设用户已根据其特定需求执行(或不执行)这些操作, 则不会发生上述任何情况.
	 * <p>有关用法示例, 请参阅{@link org.springframework.web.WebApplicationInitializer}.
	 * <p>在任何情况下, 给定的应用程序上下文将使用属性名称
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}注册到ServletContext中,
	 * 并且子类可以在容器关闭时调用{@link #closeWebApplicationContext}方法来关闭应用程序上下文.
	 * 
	 * @param context 要管理的应用程序上下文
	 */
	public ContextLoader(WebApplicationContext context) {
		this.context = context;
	}


	/**
	 * 指定应使用哪个{@link ApplicationContextInitializer}实例来初始化此{@code ContextLoader}使用的应用程序上下文.
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(ApplicationContextInitializer<?>... initializers) {
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}


	/**
	 * 使用构造时提供的应用程序上下文初始化给定servlet上下文的Spring的Web应用程序上下文,
	 * 或者根据"{@link #CONTEXT_CLASS_PARAM contextClass}"和"{@link #CONFIG_LOCATION_PARAM contextConfigLocation}" context-params
	 * 创建一个新的Web应用程序上下文.
	 * 
	 * @param servletContext 当前的servlet上下文
	 * 
	 * @return 新的WebApplicationContext
	 */
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
					"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		Log logger = LogFactory.getLog(ContextLoader.class);
		servletContext.log("Initializing Spring root WebApplicationContext");
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			// 将上下文存储在本地实例变量中, 以保证它在ServletContext关闭时可用.
			if (this.context == null) {
				this.context = createWebApplicationContext(servletContext);
			}
			if (this.context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// 上下文尚未刷新 -> 提供诸如设置父上下文, 设置应用程序上下文ID等服务
					if (cwac.getParent() == null) {
						// 注入上下文实例时没有显式父级 -> 确定根Web应用程序上下文的父级
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			}
			else if (ccl != null) {
				currentContextPerThread.put(ccl, this.context);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" +
						WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
			}
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
			}

			return this.context;
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
		catch (Error err) {
			logger.error("Context initialization failed", err);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
			throw err;
		}
	}

	/**
	 * 实例化此加载器的根WebApplicationContext, 默认上下文类或自定义上下文类.
	 * <p>此实现期望自定义上下文以实现{@link ConfigurableWebApplicationContext}接口.
	 * 可以在子类中重写.
	 * <p>此外, 在刷新上下文之前调用{@link #customizeContext}, 允许子类对上下文执行自定义修改.
	 * 
	 * @param sc 当前的servlet上下文
	 * 
	 * @return 根WebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		Class<?> contextClass = determineContextClass(sc);
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

	/**
	 * 返回要使用的WebApplicationContext实现类,
	 * 如果指定, 则使用默认的XmlWebApplicationContext或自定义上下文类.
	 * 
	 * @param servletContext 当前的servlet上下文
	 * 
	 * @return 要使用的WebApplicationContext实现类
	 */
	protected Class<?> determineContextClass(ServletContext servletContext) {
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
		if (contextClassName != null) {
			try {
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		}
		else {
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			try {
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// 应用程序上下文ID仍设置为其原始默认值 -> 根据可用信息分配更有用的ID
			String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
			if (idParam != null) {
				wac.setId(idParam);
			}
			else {
				// 生成默认id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(sc.getContextPath()));
			}
		}

		wac.setServletContext(sc);
		String configLocationParam = sc.getInitParameter(CONFIG_LOCATION_PARAM);
		if (configLocationParam != null) {
			wac.setConfigLocation(configLocationParam);
		}

		// 在刷新上下文时, 将在任何情况下调用wac环境的 #initPropertySources;
		// 在这里实时操作, 确保servlet属性源适用于在#refresh之前发生的任何后处理或初始化
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(sc, null);
		}

		customizeContext(sc, wac);
		wac.refresh();
	}

	/**
	 * 在配置位置提供给上下文之后但在上下文<em>刷新之前</em>, 自定义此ContextLoader创建的{@link ConfigurableWebApplicationContext}.
	 * <p>默认实现{@linkplain #determineContextInitializerClasses(ServletContext) 确定}
	 * 通过{@linkplain #CONTEXT_INITIALIZER_CLASSES_PARAM 上下文初始化参数}
	 * 和使用给定的Web应用程序上下文的{@linkplain ApplicationContextInitializer#initialize 每次调用}
	 * 指定了什么上下文初始化器类.
	 * <p>实现了{@link org.springframework.core.Ordered Ordered}接口
	 * 或使用@{@link org.springframework.core.annotation.Order Order}注解标记的
	 * {@code ApplicationContextInitializers}将被适当的排序.
	 * 
	 * @param sc 当前的servlet上下文
	 * @param wac 新创建的应用程序上下文
	 */
	protected void customizeContext(ServletContext sc, ConfigurableWebApplicationContext wac) {
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses =
				determineContextInitializerClasses(sc);

		for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			this.contextInitializers.add(BeanUtils.instantiateClass(initializerClass));
		}

		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	/**
	 * 如果{@link #CONTEXT_INITIALIZER_CLASSES_PARAM}指定了任何类,
	 * 返回{@link ApplicationContextInitializer}实现类.
	 * 
	 * @param servletContext 当前的servlet上下文
	 */
	protected List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>
			determineContextInitializerClasses(ServletContext servletContext) {

		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes =
				new ArrayList<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>();

		String globalClassNames = servletContext.getInitParameter(GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		String localClassNames = servletContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
		if (localClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(localClassNames, INIT_PARAM_DELIMITERS)) {
				classes.add(loadInitializerClass(className));
			}
		}

		return classes;
	}

	@SuppressWarnings("unchecked")
	private Class<ApplicationContextInitializer<ConfigurableApplicationContext>> loadInitializerClass(String className) {
		try {
			Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
			if (!ApplicationContextInitializer.class.isAssignableFrom(clazz)) {
				throw new ApplicationContextException(
						"Initializer class does not implement ApplicationContextInitializer interface: " + clazz);
			}
			return (Class<ApplicationContextInitializer<ConfigurableApplicationContext>>) clazz;
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
		}
	}

	/**
	 * 具有默认实现的模板方法 (可以由子类覆盖), 以加载或获取将用作根WebApplicationContext的父上下文的ApplicationContext实例.
	 * 如果方法的返回值为null, 则不设置父上下文.
	 * <p>这里加载父上下文的主要原因是允许多个根Web应用程序上下文都是共享EAR上下文的子项,
	 * 或者也可以共享EJB可见的相同父上下文.
	 * 对于纯Web应用程序, 通常无需担心根Web应用程序上下文具有父上下文.
	 * <p>默认实现使用
	 * {@link org.springframework.context.access.ContextSingletonBeanFactoryLocator},
	 * 通过{@link #LOCATOR_FACTORY_SELECTOR_PARAM}和{@link #LOCATOR_FACTORY_KEY_PARAM}配置,
	 * 加载一个父上下文, 该上下文将由ContextsingletonBeanFactoryLocator的所有其他用户共享, 这些用户也使用相同的配置参数.
	 * 
	 * @param servletContext 当前的servlet上下文
	 * 
	 * @return 父应用程序上下文, 或{@code null}
	 */
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		ApplicationContext parentContext = null;
		String locatorFactorySelector = servletContext.getInitParameter(LOCATOR_FACTORY_SELECTOR_PARAM);
		String parentContextKey = servletContext.getInitParameter(LOCATOR_FACTORY_KEY_PARAM);

		if (parentContextKey != null) {
			// locatorFactorySelector可以为null, 表示默认的"classpath*:beanRefContext.xml"
			BeanFactoryLocator locator = ContextSingletonBeanFactoryLocator.getInstance(locatorFactorySelector);
			Log logger = LogFactory.getLog(ContextLoader.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Getting parent context definition: using parent context key of '" +
						parentContextKey + "' with BeanFactoryLocator");
			}
			this.parentContextRef = locator.useBeanFactory(parentContextKey);
			parentContext = (ApplicationContext) this.parentContextRef.getFactory();
		}

		return parentContext;
	}

	/**
	 * 关闭给定servlet上下文的Spring的Web应用程序上下文.
	 * 如果使用ContextSingletonBeanFactoryLocator的默认{@link #loadParentContext(ServletContext)}实现,
	 * 已加载任何共享父上下文, 则释放对该共享父上下文的一个引用.
	 * <p>如果重写{@link #loadParentContext(ServletContext)}, 可能还必须重写此方法.
	 * 
	 * @param servletContext WebApplicationContext运行的ServletContext
	 */
	public void closeWebApplicationContext(ServletContext servletContext) {
		servletContext.log("Closing Spring root WebApplicationContext");
		try {
			if (this.context instanceof ConfigurableWebApplicationContext) {
				((ConfigurableWebApplicationContext) this.context).close();
			}
		}
		finally {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = null;
			}
			else if (ccl != null) {
				currentContextPerThread.remove(ccl);
			}
			servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			if (this.parentContextRef != null) {
				this.parentContextRef.release();
			}
		}
	}


	/**
	 * 获取当前线程的Spring根Web应用程序上下文
	 * (i.e. 对于当前线程的上下文ClassLoader, 它需要是Web应用程序的ClassLoader).
	 * 
	 * @return 当前的根Web应用程序上下文, 或{@code null}
	 */
	public static WebApplicationContext getCurrentWebApplicationContext() {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		if (ccl != null) {
			WebApplicationContext ccpt = currentContextPerThread.get(ccl);
			if (ccpt != null) {
				return ccpt;
			}
		}
		return currentContext;
	}
}
