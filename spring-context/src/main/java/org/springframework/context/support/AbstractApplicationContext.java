package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link org.springframework.context.ApplicationContext}接口的抽象实现.
 * 不强制要求用于配置的存储类型; 只需实现常见的上下文功能.
 * 使用Template Method设计模式, 需要具体的子类来实现抽象方法.
 *
 * <p>与普通的BeanFactory相比, ApplicationContext应该检测在其内部bean工厂中定义的特殊bean:
 * 因此, 此类自动注册
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * {@link org.springframework.context.ApplicationListener ApplicationListeners},
 * 它们在上下文中定义为bean.
 *
 * <p>{@link org.springframework.context.MessageSource} 也可以在上下文中作为bean提供, 名称为 "messageSource";
 * 否则, 将消息解析委托给父上下文.
 * 此外, 应用程序事件的多播器可以在上下文中
 * 以类型为{@link org.springframework.context.event.ApplicationEventMulticaster}的 "applicationEventMulticaster" bean提供;
 * 否则, 将使用{@link org.springframework.context.event.SimpleApplicationEventMulticaster}类型的默认多播器.
 *
 * <p>通过扩展 {@link org.springframework.core.io.DefaultResourceLoader}实现资源加载.
 * 因此, 将非URL资源路径视为类路径资源
 * (支持包含包路径的完整类路径资源名称, e.g. "mypackage/myresource.dat"),
 * 除非在子类中覆盖{@link #getResourceByPath}方法.
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext, DisposableBean {

	/**
	 * 工厂中MessageSource bean的名称.
	 * 如果未提供, 则将消息解析委托给父级.
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * 工厂中LifecycleProcessor bean的名称.
	 * 如果未提供, 将使用DefaultLifecycleProcessor.
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	/**
	 * 工厂中ApplicationEventMulticaster bean的名称.
	 * 如果未提供, 将使用默认的 SimpleApplicationEventMulticaster.
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";


	static {
		// 实时加载 ContextClosedEvent类, 以避免在WebLogic 8.1中关闭应用程序时出现奇怪的类加载器问题. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 此上下文的唯一ID */
	private String id = ObjectUtils.identityToString(this);

	/** 显示名称 */
	private String displayName = ObjectUtils.identityToString(this);

	/** 父级上下文 */
	private ApplicationContext parent;

	/** 此上下文使用的环境 */
	private ConfigurableEnvironment environment;

	/** 刷新时, 要应用的BeanFactoryPostProcessor */
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors =
			new ArrayList<BeanFactoryPostProcessor>();

	/** 此上下文启动时的系统时间, 以毫秒为单位 */
	private long startupDate;

	/** 指示此上下文当前是否处于活动状态 */
	private final AtomicBoolean active = new AtomicBoolean();

	/** 指示此上下文是否已关闭 */
	private final AtomicBoolean closed = new AtomicBoolean();

	/** 用于“刷新”和“销毁”的同步监视器 */
	private final Object startupShutdownMonitor = new Object();

	/** 如果已注册, 则引用JVM关闭挂钩 */
	private Thread shutdownHook;

	/** 此上下文使用的ResourcePatternResolver */
	private ResourcePatternResolver resourcePatternResolver;

	/** 用于在此上下文中管理bean的生命周期的LifecycleProcessor */
	private LifecycleProcessor lifecycleProcessor;

	/** 将这个接口的实现委托给的MessageSource */
	private MessageSource messageSource;

	/** 事件发布中使用的帮助类 */
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** 静态指定的监听器 */
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<ApplicationListener<?>>();

	/** 实时发布的ApplicationEvent */
	private Set<ApplicationEvent> earlyApplicationEvents;


	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * @param parent 父级上下文
	 */
	public AbstractApplicationContext(ApplicationContext parent) {
		this();
		setParent(parent);
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * 设置此应用程序上下文的唯一ID.
	 * <p>默认是上下文实例的对象ID, 如果上下文本身被定义为bean, 则为上下文bean的名称.
	 * 
	 * @param id 上下文的唯一ID
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * 设置此上下文的友好名称.
	 * 通常在具体上下文实现的初始化期间完成.
	 * <p>默认值是上下文实例的对象ID.
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * 返回此上下文的友好名称.
	 * 
	 * @return 此上下文的显示名称 (never {@code null})
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * 返回父上下文, 或{@code null} 如果没有父上下文 (即, 此上下文是上下文层次结构的根).
	 */
	@Override
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * 设置此应用程序上下文的{@code Environment}.
	 * <p>默认值由{@link #createEnvironment()}确定.
	 * 使用此方法替换默认值是一种选择, 但也应考虑通过 {@link #getEnvironment()}进行配置.
	 * 在任何一种情况下, 都应在 {@link #refresh()}之前执行此类修改.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * 以可配置的形式返回此应用程序上下文的{@code Environment}, 以便进一步自定义.
	 * <p>如果未指定, 则将通过 {@link #createEnvironment()}初始化默认环境.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * 创建并返回新的 {@link StandardEnvironment}.
	 * <p>子类可以覆盖此方法, 以便提供自定义的 {@link ConfigurableEnvironment}实现.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * 返回此上下文的内部bean工厂, 作为AutowireCapableBeanFactory, 如果已经可用.
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * 首次加载此上下文时, 返回时间戳 (ms).
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * 将给定事件发布给所有监听器.
	 * <p>Note: 监听器在MessageSource之后初始化, 以便能够在监听器实现中访问它.
	 * 因此, MessageSource 实现无法发布事件.
	 * 
	 * @param event 要发布的事件 (可能是特定于应用程序或标准框架事件)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * 将给定事件发布给所有监听器.
	 * <p>Note: 监听器在MessageSource之后初始化, 以便能够在监听器实现中访问它.
	 * 因此, MessageSource 实现无法发布事件.
	 * 
	 * @param event 要发布的事件 (可能是 {@link ApplicationEvent}, 或有效负载对象转换为{@link PayloadApplicationEvent})
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * 将给定事件发布给所有监听器.
	 * 
	 * @param event 要发布的事件 (可能是 {@link ApplicationEvent}, 或有效负载对象转换为 {@link PayloadApplicationEvent})
	 * @param eventType 解析的事件类型, 如果已知
	 */
	protected void publishEvent(Object event, ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Publishing event in " + getDisplayName() + ": " + event);
		}

		// 如有必要, 将事件装饰为ApplicationEvent
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		}
		else {
			applicationEvent = new PayloadApplicationEvent<Object>(this, event);
			if (eventType == null) {
				eventType = ((PayloadApplicationEvent) applicationEvent).getResolvableType();
			}
		}

		// 现在就进行多播 - 或者在多播器初始化后延迟地进行多播
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// 通过父级上下文发布事件...
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			}
			else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * 返回上下文使用的内部ApplicationEventMulticaster.
	 * 
	 * @return 内部ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException 如果上下文尚未初始化
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	/**
	 * 返回上下文使用的内部 LifecycleProcessor.
	 * 
	 * @return 内部 LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException 如果上下文尚未初始化
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * 返回用于将位置模式解析为Resource实例的ResourcePatternResolver.
	 * 默认是 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}, 支持Ant风格的位置模式.
	 * <p>可以在子类中重写, 以用于扩展解析策略, 例如在Web环境中.
	 * <p><b>需要解析位置模式时不要调用此方法.</b>
	 * 调用上下文的 {@code getResources}方法, 它将委托给 ResourcePatternResolver.
	 * 
	 * @return 此上下文的ResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * 设置此应用程序上下文的父级.
	 * <p>父级{@linkplain ApplicationContext#getEnvironment() 环境}
	 * 是{@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} 与此(子) 应用程序上下文环境,
	 * 如果父级是非{@code null} 且其环境是一个{@link ConfigurableEnvironment}实例.
	 */
	@Override
	public void setParent(ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}


	/**
	 * 返回将应用于内部BeanFactory的BeanFactoryPostProcessor列表.
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		else {
			this.applicationListeners.add(listener);
		}
	}

	/**
	 * 返回静态指定的ApplicationListener列表.
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// 准备此上下文以进行刷新.
			prepareRefresh();

			// 告诉子类刷新内部bean工厂.
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// 准备bean工厂以在此上下文中使用.
			prepareBeanFactory(beanFactory);

			try {
				// 允许在上下文子类中对bean工厂进行后处理.
				postProcessBeanFactory(beanFactory);

				// 在上下文中调用注册为bean的工厂处理器.
				invokeBeanFactoryPostProcessors(beanFactory);

				// 注册拦截bean创建的bean处理器.
				registerBeanPostProcessors(beanFactory);

				// 初始化此上下文的消息源.
				initMessageSource();

				// 初始化此上下文的事件多播器.
				initApplicationEventMulticaster();

				// 在特定的上下文子类中初始化其他特殊bean.
				onRefresh();

				// 检查监听器bean并注册它们.
				registerListeners();

				// 实例化所有剩余 (non-lazy-init)单例.
				finishBeanFactoryInitialization(beanFactory);

				// 最后一步: 发布相应的事件.
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// 销毁已创建的单例, 以避免悬空资源.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// 向调用者传播异常.
				throw ex;
			}

			finally {
				// 重置Spring核心中的常见内省缓存, 因为我们可能不再需要单例bean的元数据...
				resetCommonCaches();
			}
		}
	}

	/**
	 * 准备此上下文以进行刷新, 设置其启动日期和活动标志, 并执行属性源的初始化.
	 */
	protected void prepareRefresh() {
		this.startupDate = System.currentTimeMillis();
		this.closed.set(false);
		this.active.set(true);

		if (logger.isInfoEnabled()) {
			logger.info("Refreshing " + this);
		}

		// 在上下文环境中初始化占位符属性源
		initPropertySources();

		// 验证标记为必需的所有属性是否可解析
		// see ConfigurablePropertyResolver#setRequiredProperties
		getEnvironment().validateRequiredProperties();

		// 允许收集早期的ApplicationEvents, 以便在多播器可用时发布...
		this.earlyApplicationEvents = new LinkedHashSet<ApplicationEvent>();
	}

	/**
	 * <p>用实际实例替换任何stub属性源.
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * 告诉子类刷新内部bean工厂.
	 * 
	 * @return 新鲜的BeanFactory实例
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		refreshBeanFactory();
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (logger.isDebugEnabled()) {
			logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
		}
		return beanFactory;
	}

	/**
	 * 配置工厂的标准上下文特征, 例如上下文的ClassLoader和后处理器.
	 * 
	 * @param beanFactory 要配置的BeanFactory
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 告诉内部bean工厂, 使用上下文的类加载器等.
		beanFactory.setBeanClassLoader(getClassLoader());
		beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// 使用上下文回调配置bean工厂.
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

		// BeanFactory接口未在普通工厂中注册为可解析类型.
		// MessageSource 作为bean注册 (并发现用于自动装配).
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// 注册早期的后处理器以检测内部bean作为ApplicationListener.
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// 检测LoadTimeWeaver并准备织入.
		if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// 为类型匹配设置临时ClassLoader.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// 注册默认环境bean.
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
	}

	/**
	 * 在标准初始化之后修改应用程序上下文的内部bean工厂.
	 * 将加载所有bean定义, 但尚未实例化任何bean.
	 * 这允许在某些ApplicationContext实现中注册特殊的BeanPostProcessor等.
	 * 
	 * @param beanFactory 应用程序上下文使用的bean工厂
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	/**
	 * 实例化并调用所有已注册的 BeanFactoryPostProcessor bean, 如果给定了顺序, 则遵守显式顺序.
	 * <p>必须在单例实例化之前调用.
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// 如果在此期间找到, 则检测LoadTimeWeaver并准备织入
		// (e.g. 通过ConfigurationClassPostProcessor注册的@Bean方法)
		if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * 实例化并调用所有已注册的 BeanPostProcessor bean, 如果给定了顺序, 则遵守显式顺序.
	 * <p>必须在应用程序bean实例化之前调用.
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * 初始化MessageSource.
	 * 如果在此上下文中未定义, 则使用父级的.
	 */
	protected void initMessageSource() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// 如果尚未注册父级MessageSource, 则仅将父级上下文设置为父级MessageSource.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// 使用空MessageSource可以接受getMessage调用.
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME +
						"': using default [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * 初始化 ApplicationEventMulticaster.
	 * 如果在上下文中没有定义, 则使用SimpleApplicationEventMulticaster.
	 */
	protected void initApplicationEventMulticaster() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			this.applicationEventMulticaster =
					beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ApplicationEventMulticaster with name '" +
						APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
						"': using default [" + this.applicationEventMulticaster + "]");
			}
		}
	}

	/**
	 * 初始化LifecycleProcessor.
	 * 如果在上下文中没有定义, 则使用DefaultLifecycleProcessor.
	 */
	protected void initLifecycleProcessor() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			defaultProcessor.setBeanFactory(beanFactory);
			this.lifecycleProcessor = defaultProcessor;
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate LifecycleProcessor with name '" +
						LIFECYCLE_PROCESSOR_BEAN_NAME +
						"': using default [" + this.lifecycleProcessor + "]");
			}
		}
	}

	/**
	 * 可以重写的模板方法, 以添加特定于上下文的刷新工作.
	 * 在实例化单例之前, 调用特殊bean的初始化.
	 * <p>实现是空的.
	 * 
	 * @throws BeansException 发生错误
	 */
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * 添加实现ApplicationListener作为监听器的bean.
	 * 不影响其他监听器, 可以添加而不必是bean.
	 */
	protected void registerListeners() {
		// 首先注册静态指定的监听器.
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// 不要在这里初始化FactoryBeans: 需要保留所有未初始化的常规bean, 让后处理器应用于它们!
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// 现在, 最终拥有一个多播器, 发布早期应用程序事件...
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (earlyEventsToProcess != null) {
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * 完成此上下文的bean工厂的初始化, 初始化所有剩余的单例bean.
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// 初始化此上下文的转换服务.
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
				beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(
					beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// 如果之前没有注册bean后处理器 (例如 PropertyPlaceholderConfigurer bean), 则注册默认的嵌入值解析器:
		// 此时, 主要用于注解属性值的解析.
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(new StringValueResolver() {
				@Override
				public String resolveStringValue(String strVal) {
					return getEnvironment().resolvePlaceholders(strVal);
				}
			});
		}

		// 尽早初始化LoadTimeWeaverAware bean, 以允许尽早注册其变换器.
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// 停止使用临时ClassLoader进行类型匹配.
		beanFactory.setTempClassLoader(null);

		// 允许缓存所有bean定义元数据, 而不期望进一步的更改.
		beanFactory.freezeConfiguration();

		// 实例化所有剩余 (非延迟初始化) 单例.
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * 完成此上下文的刷新, 调用LifecycleProcessor的 onRefresh() 方法并发布 {@link org.springframework.context.event.ContextRefreshedEvent}.
	 */
	protected void finishRefresh() {
		// 为此上下文初始化生命周期处理器.
		initLifecycleProcessor();

		// 首先将刷新传播到生命周期处理器.
		getLifecycleProcessor().onRefresh();

		// 发布最终的事件.
		publishEvent(new ContextRefreshedEvent(this));

		// 如果处于活动状态, 请参与LiveBeansView MBean.
		LiveBeansView.registerApplicationContext(this);
	}

	/**
	 * 取消此上下文的刷新尝试, 在抛出异常后重置 {@code active}标志.
	 * 
	 * @param ex 导致取消的异常情况
	 */
	protected void cancelRefresh(BeansException ex) {
		this.active.set(false);
	}

	/**
	 * 重置Spring的常见核心缓存, 特别是 {@link ReflectionUtils}, {@link ResolvableType}, {@link CachedIntrospectionResults}缓存.
	 */
	protected void resetCommonCaches() {
		ReflectionUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * 在JVM运行时注册关闭挂钩, 在JVM关闭时关闭此上下文, 除非此时已关闭.
	 * <p>委托给 {@code doClose()} 进行实际关闭处理.
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// 尚未注册关闭挂钩.
			this.shutdownHook = new Thread() {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * DisposableBean回调用于销毁此实例.
	 * <p>{@link #close()} 方法是关闭ApplicationContext的native方法, 此方法只需委托给它.
	 */
	@Override
	public void destroy() {
		close();
	}

	/**
	 * 关闭此应用程序上下文, 销毁其bean工厂中的所有bean.
	 * <p>委托给 {@code doClose()} 进行实际关闭处理.
	 * 如果已注册, 还会删除JVM关闭挂钩, 因为它不再需要.
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// 如果注册了JVM关闭钩子, 现在就不再需要它了:
			// 已经明确地关闭了上下文.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * 实际执行上下文关闭: 发布ContextClosedEvent, 并销毁此应用程序上下文的bean工厂中的单例.
	 * <p>由 {@code close()} 和JVM关闭挂钩调用.
	 */
	protected void doClose() {
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isInfoEnabled()) {
				logger.info("Closing " + this);
			}

			LiveBeansView.unregisterApplicationContext(this);

			try {
				// Publish shutdown event.
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// 停止所有生命周期bean, 以避免在个别销毁期间出现延迟.
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				}
				catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// 在上下文的BeanFactory中销毁所有缓存的单例.
			destroyBeans();

			// 关闭此上下文本身的状态.
			closeBeanFactory();

			// 如果他们愿意, 让子类做一些最后的清理工作...
			onClose();

			this.active.set(false);
		}
	}

	/**
	 * 用于销毁此上下文管理的所有bean的模板方法.
	 * 默认实现销毁此上下文中所有缓存的单例, 调用{@code DisposableBean.destroy()} 和/或指定的 "destroy-method".
	 * <p>可以重写, 以在标准单例销毁之前或之后, 添加特定于上下文的bean销毁步骤, 同时上下文的BeanFactory仍处于活动状态.
	 */
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * 模板方法, 可以重写以添加特定于上下文的关闭工作.
	 * 默认实现是空的.
	 * <p>在这个上下文的BeanFactory关闭之后, 在{@link #doClose}的关闭过程结束时调用.
	 * 如果在BeanFactory仍处于活动状态时需要执行自定义关闭逻辑, 重写{@link #destroyBeans()}方法.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * 断言此上下文的BeanFactory当前处于活动状态, 如果不是, 则抛出 {@link IllegalStateException}.
	 * <p>由依赖于活动上下文的所有{@link BeanFactory}委派方法调用, i.e. 特别是所有bean访问器方法.
	 * <p>默认实现整体检查此上下文的 {@link #isActive() 'active'}状态.
	 * 如果{@link #getBeanFactory()}本身在这种情况下抛出异常, 则可以覆盖以便更具体的检查, 或者no-op.
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			}
			else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException{

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * 如果它实现了ConfigurableApplicationContext, 则返回父级上下文的内部bean工厂; 否则, 返回父级上下文本身.
	 */
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext) ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent();
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	@Override
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * 返回上下文使用的内部MessageSource.
	 * 
	 * @return 内部MessageSource (never {@code null})
	 * @throws IllegalStateException 如果上下文尚未初始化
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * 如果它也是AbstractApplicationContext, 则返回父级上下文的内部消息源; 否则, 返回父级上下文本身.
	 */
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext) ?
			((AbstractApplicationContext) getParent()).messageSource : getParent();
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	@Override
	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * 子类必须实现此方法才能执行实际的配置加载.
	 * 在其他初始化工作之前, {@link #refresh()}调用该方法.
	 * <p>子类将创建一个新的bean工厂并保存对它的引用, 或者返回它所拥有的单个BeanFactory实例.
	 * 在后一种情况下, 如果多次刷新上下文, 它通常会抛出IllegalStateException.
	 * 
	 * @throws BeansException 如果bean工厂初始化失败
	 * @throws IllegalStateException 如果已初始化, 并且不支持多次刷新尝试
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * 子类必须实现此方法才能释放其内部bean工厂.
	 * 在所有其他关机工作之后, {@link #close()}会调用此方法.
	 * <p>永远不应该抛出异常, 而是记录关闭失败.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * 子类必须在此处返回其内部bean工厂.
	 * 它们应该有效地实现查找, 以便可以重复调用它而不会降低性能.
	 * <p>Note: 在返回内部bean工厂之前, 子类应检查上下文是否仍处于活动状态.
	 * 一旦关闭上下文, 通常应将内部工厂视为不可用.
	 * 
	 * @return 这个应用程序上下文的内部bean工厂 (never {@code null})
	 * @throws IllegalStateException 如果上下文还没有内部bean工厂
	 * (通常如果从未调用 {@link #refresh()}) 或者已经关闭了上下文
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * 返回有关此上下文的信息.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(": startup date [").append(new Date(getStartupDate()));
		sb.append("]; ");
		ApplicationContext parent = getParent();
		if (parent == null) {
			sb.append("root of context hierarchy");
		}
		else {
			sb.append("parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
