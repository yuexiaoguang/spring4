package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.util.MetaAnnotationUtils;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link TestContextBootstrapper}接口的抽象实现, 它提供了引导程序所需的大部分行为.
 *
 * <p>具体的子类通常只需要提供以下方法的实现:
 * <ul>
 * <li>{@link #getDefaultContextLoaderClass}
 * <li>{@link #processMergedContextConfiguration}
 * </ul>
 *
 * <p>要插入自定义
 * {@link org.springframework.test.context.cache.ContextCache ContextCache}支持,
 * 覆盖{@link #getCacheAwareContextLoaderDelegate()}.
 */
public abstract class AbstractTestContextBootstrapper implements TestContextBootstrapper {

	private final Log logger = LogFactory.getLog(getClass());

	private BootstrapContext bootstrapContext;


	@Override
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return this.bootstrapContext;
	}

	/**
	 * 使用与此引导程序关联的{@link BootstrapContext}中的{@linkplain Class 测试类}构建新的{@link DefaultTestContext},
	 * 并委托给{@link #buildMergedContextConfiguration()} 和 {@link #getCacheAwareContextLoaderDelegate()}.
	 * <p>具体的子类可以选择覆盖此方法以返回自定义的{@link TestContext}实现.
	 */
	@Override
	public TestContext buildTestContext() {
		return new DefaultTestContext(getBootstrapContext().getTestClass(), buildMergedContextConfiguration(),
				getCacheAwareContextLoaderDelegate());
	}

	@Override
	public final List<TestExecutionListener> getTestExecutionListeners() {
		Class<?> clazz = getBootstrapContext().getTestClass();
		Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
		List<Class<? extends TestExecutionListener>> classesList = new ArrayList<Class<? extends TestExecutionListener>>();
		boolean usingDefaults = false;

		AnnotationDescriptor<TestExecutionListeners> descriptor =
				MetaAnnotationUtils.findAnnotationDescriptor(clazz, annotationType);

		// Use defaults?
		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("@TestExecutionListeners is not present for class [%s]: using defaults.",
						clazz.getName()));
			}
			usingDefaults = true;
			classesList.addAll(getDefaultTestExecutionListenerClasses());
		}
		else {
			// 遍历类层次结构...
			while (descriptor != null) {
				Class<?> declaringClass = descriptor.getDeclaringClass();
				TestExecutionListeners testExecutionListeners = descriptor.synthesizeAnnotation();
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Retrieved @TestExecutionListeners [%s] for declaring class [%s].",
							testExecutionListeners, declaringClass.getName()));
				}

				boolean inheritListeners = testExecutionListeners.inheritListeners();
				AnnotationDescriptor<TestExecutionListeners> superDescriptor =
						MetaAnnotationUtils.findAnnotationDescriptor(
								descriptor.getRootDeclaringClass().getSuperclass(), annotationType);

				// 如果没有要继承的监听器, 可能需要将本地声明的监听器与默认值合并.
				if ((!inheritListeners || superDescriptor == null) &&
						testExecutionListeners.mergeMode() == MergeMode.MERGE_WITH_DEFAULTS) {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Merging default listeners with listeners configured via " +
								"@TestExecutionListeners for class [%s].", descriptor.getRootDeclaringClass().getName()));
					}
					usingDefaults = true;
					classesList.addAll(getDefaultTestExecutionListenerClasses());
				}

				classesList.addAll(0, Arrays.asList(testExecutionListeners.listeners()));

				descriptor = (inheritListeners ? superDescriptor : null);
			}
		}

		Collection<Class<? extends TestExecutionListener>> classesToUse = classesList;
		// 如果加载默认监听器, 删除可能的重复项.
		if (usingDefaults) {
			classesToUse = new LinkedHashSet<Class<? extends TestExecutionListener>>(classesList);
		}

		List<TestExecutionListener> listeners = instantiateListeners(classesToUse);
		// 如果加载了默认监听器, 按Ordered/@Order排序.
		if (usingDefaults) {
			AnnotationAwareOrderComparator.sort(listeners);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Using TestExecutionListeners: " + listeners);
		}
		return listeners;
	}

	private List<TestExecutionListener> instantiateListeners(Collection<Class<? extends TestExecutionListener>> classesList) {
		List<TestExecutionListener> listeners = new ArrayList<TestExecutionListener>(classesList.size());
		for (Class<? extends TestExecutionListener> listenerClass : classesList) {
			NoClassDefFoundError ncdfe = null;
			try {
				listeners.add(BeanUtils.instantiateClass(listenerClass));
			}
			catch (NoClassDefFoundError err) {
				ncdfe = err;
			}
			catch (BeanInstantiationException ex) {
				if (!(ex.getCause() instanceof NoClassDefFoundError)) {
					throw ex;
				}
				ncdfe = (NoClassDefFoundError) ex.getCause();
			}
			if (ncdfe != null) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Could not instantiate TestExecutionListener [%s]. " +
							"Specify custom listener classes or make the default listener classes " +
							"(and their required dependencies) available. Offending class: [%s]",
							listenerClass.getName(), ncdfe.getMessage()));
				}
			}
		}
		return listeners;
	}

	/**
	 * 获取此引导程序的默认{@link TestExecutionListener}类.
	 * <p>此方法由{@link #getTestExecutionListeners()}调用, 并委托给{@link #getDefaultTestExecutionListenerClassNames()}以检索类名.
	 * <p>如果无法加载特定的类, 将记录{@code DEBUG}消息, 但不会重新抛出相关的异常.
	 */
	@SuppressWarnings("unchecked")
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> defaultListenerClasses = new LinkedHashSet<Class<? extends TestExecutionListener>>();
		ClassLoader cl = getClass().getClassLoader();
		for (String className : getDefaultTestExecutionListenerClassNames()) {
			try {
				defaultListenerClasses.add((Class<? extends TestExecutionListener>) ClassUtils.forName(className, cl));
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not load default TestExecutionListener class [" + className +
							"]. Specify custom listener classes or make the default listener classes available.", ex);
				}
			}
		}
		return defaultListenerClasses;
	}

	/**
	 * 获取此引导程序的默认{@link TestExecutionListener}类的名称.
	 * <p>默认实现查找在类路径上的所有{@code META-INF/spring.factories}文件中
	 * 配置的所有{@code org.springframework.test.context.TestExecutionListener}条目.
	 * <p>{@link #getDefaultTestExecutionListenerClasses()}调用此方法.
	 * 
	 * @return 默认{@code TestExecutionListener}类的名称的<em>不可修改的</em>列表
	 */
	protected List<String> getDefaultTestExecutionListenerClassNames() {
		List<String> classNames =
				SpringFactoriesLoader.loadFactoryNames(TestExecutionListener.class, getClass().getClassLoader());
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Loaded default TestExecutionListener class names from location [%s]: %s",
					SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION, classNames));
		}
		return Collections.unmodifiableList(classNames);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final MergedContextConfiguration buildMergedContextConfiguration() {
		Class<?> testClass = getBootstrapContext().getTestClass();
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getCacheAwareContextLoaderDelegate();

		if (MetaAnnotationUtils.findAnnotationDescriptorForTypes(
				testClass, ContextConfiguration.class, ContextHierarchy.class) == null) {
			return buildDefaultMergedContextConfiguration(testClass, cacheAwareContextLoaderDelegate);
		}

		if (AnnotationUtils.findAnnotation(testClass, ContextHierarchy.class) != null) {
			Map<String, List<ContextConfigurationAttributes>> hierarchyMap =
					ContextLoaderUtils.buildContextHierarchyMap(testClass);
			MergedContextConfiguration parentConfig = null;
			MergedContextConfiguration mergedConfig = null;

			for (List<ContextConfigurationAttributes> list : hierarchyMap.values()) {
				List<ContextConfigurationAttributes> reversedList = new ArrayList<ContextConfigurationAttributes>(list);
				Collections.reverse(reversedList);

				// 不要使用提供的testClass; 而是确保为实际测试类构建MCC, 该测试类在上下文层次结构中声明了当前级别的配置.
				Assert.notEmpty(reversedList, "ContextConfigurationAttributes list must not be empty");
				Class<?> declaringClass = reversedList.get(0).getDeclaringClass();

				mergedConfig = buildMergedContextConfiguration(
						declaringClass, reversedList, parentConfig, cacheAwareContextLoaderDelegate, true);
				parentConfig = mergedConfig;
			}

			// 返回上下文层次结构中的最后一个级别
			return mergedConfig;
		}
		else {
			return buildMergedContextConfiguration(testClass,
					ContextLoaderUtils.resolveContextConfigurationAttributes(testClass),
					null, cacheAwareContextLoaderDelegate, true);
		}
	}

	private MergedContextConfiguration buildDefaultMergedContextConfiguration(Class<?> testClass,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		List<ContextConfigurationAttributes> defaultConfigAttributesList =
				Collections.singletonList(new ContextConfigurationAttributes(testClass));

		ContextLoader contextLoader = resolveContextLoader(testClass, defaultConfigAttributesList);
		if (logger.isInfoEnabled()) {
			logger.info(String.format(
					"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s], using %s",
					testClass.getName(), contextLoader.getClass().getSimpleName()));
		}
		return buildMergedContextConfiguration(testClass, defaultConfigAttributesList, null,
				cacheAwareContextLoaderDelegate, false);
	}

	/**
	 * 为所提供的{@link Class testClass}, 上下文配置属性和父上下文配置,
	 * 构建{@link MergedContextConfiguration 合并上下文配置}.
	 * 
	 * @param testClass 应为其构建{@code MergedContextConfiguration}的测试类 (must not be {@code null})
	 * @param configAttributesList 指定测试类的上下文配置属性列表,
	 * 排序<em>自下而上</em> (i.e., 好像我们正在遍历类层次结构); never {@code null} or empty
	 * @param parentConfig 上下文层次结构中父应用程序上下文的合并上下文配置, 或{@code null}
	 * @param cacheAwareContextLoaderDelegate 要传递给{@code MergedContextConfiguration}构造函数的缓存感知上下文加载器委托
	 * @param requireLocationsClassesOrInitializers 是否需要位置, 类, 或初始化器;
	 * 通常{@code true}, 但如果配置的加载器支持空配置, 则可以设置为{@code false}
	 * 
	 * @return 合并的上下文配置
	 */
	private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, MergedContextConfiguration parentConfig,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			boolean requireLocationsClassesOrInitializers) {

		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");

		ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList);
		List<String> locations = new ArrayList<String>();
		List<Class<?>> classes = new ArrayList<Class<?>>();
		List<Class<?>> initializers = new ArrayList<Class<?>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations and classes for context configuration attributes %s",
						configAttributes));
			}
			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				smartContextLoader.processContextConfiguration(configAttributes);
				locations.addAll(0, Arrays.asList(configAttributes.getLocations()));
				classes.addAll(0, Arrays.asList(configAttributes.getClasses()));
			}
			else {
				String[] processedLocations = contextLoader.processLocations(
						configAttributes.getDeclaringClass(), configAttributes.getLocations());
				locations.addAll(0, Arrays.asList(processedLocations));
				// Legacy ContextLoaders don't know how to process classes
			}
			initializers.addAll(0, Arrays.asList(configAttributes.getInitializers()));
			if (!configAttributes.isInheritLocations()) {
				break;
			}
		}

		Set<ContextCustomizer> contextCustomizers = getContextCustomizers(testClass,
				Collections.unmodifiableList(configAttributesList));

		if (requireLocationsClassesOrInitializers &&
				areAllEmpty(locations, classes, initializers, contextCustomizers)) {
			throw new IllegalStateException(String.format(
					"%s was unable to detect defaults, and no ApplicationContextInitializers " +
					"or ContextCustomizers were declared for context configuration attributes %s",
					contextLoader.getClass().getSimpleName(), configAttributesList));
		}

		MergedTestPropertySources mergedTestPropertySources =
				TestPropertySourceUtils.buildMergedTestPropertySources(testClass);
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(testClass,
				StringUtils.toStringArray(locations),
				ClassUtils.toClassArray(classes),
				ApplicationContextInitializerUtils.resolveInitializerClasses(configAttributesList),
				ActiveProfilesUtils.resolveActiveProfiles(testClass),
				mergedTestPropertySources.getLocations(),
				mergedTestPropertySources.getProperties(),
				contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);

		return processMergedContextConfiguration(mergedConfig);
	}

	private Set<ContextCustomizer> getContextCustomizers(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		List<ContextCustomizerFactory> factories = getContextCustomizerFactories();
		Set<ContextCustomizer> customizers = new LinkedHashSet<ContextCustomizer>(factories.size());
		for (ContextCustomizerFactory factory : factories) {
			ContextCustomizer customizer = factory.createContextCustomizer(testClass, configAttributes);
			if (customizer != null) {
				customizers.add(customizer);
			}
		}
		return customizers;
	}

	/**
	 * 获取此引导程序的{@link ContextCustomizerFactory}实例.
	 * <p>默认实现使用{@link SpringFactoriesLoader}机制来加载在类路径上的所有{@code META-INF/spring.factories}文件中配置的工厂.
	 */
	protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
		return SpringFactoriesLoader.loadFactories(ContextCustomizerFactory.class, getClass().getClassLoader());
	}

	/**
	 * 解析{@link ContextLoader} {@linkplain Class class}以用于提供的{@link ContextConfigurationAttributes}列表,
	 * 然后实例化并返回{@code ContextLoader}.
	 * <p>如果用户没有显式声明要使用哪个加载器,
	 * 则从{@link #getDefaultContextLoaderClass}返回的值将用作默认的上下文加载器类.
	 * 有关类解析过程的详细信息, 请参阅{@link #resolveExplicitContextLoaderClass}和{@link #getDefaultContextLoaderClass}.
	 * 
	 * @param testClass 应该解析{@code ContextLoader}的测试类; 不能是{@code null}
	 * @param configAttributesList 要处理的配置属性列表; 不能是{@code null};
	 * 必须<em>自下而上</em>排序 (i.e., 好像我们正在遍历类层次结构)
	 * 
	 * @return 已解析的用于提供的{@code testClass}的{@code ContextLoader} (never {@code null})
	 * @throws IllegalStateException 如果{@link #getDefaultContextLoaderClass(Class)}返回 {@code null}
	 */
	protected ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(testClass, "Class must not be null");
		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		Class<? extends ContextLoader> contextLoaderClass = resolveExplicitContextLoaderClass(configAttributesList);
		if (contextLoaderClass == null) {
			contextLoaderClass = getDefaultContextLoaderClass(testClass);
			if (contextLoaderClass == null) {
				throw new IllegalStateException("getDefaultContextLoaderClass() must not return null");
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Using ContextLoader class [%s] for test class [%s]",
					contextLoaderClass.getName(), testClass.getName()));
		}
		return BeanUtils.instantiateClass(contextLoaderClass, ContextLoader.class);
	}

	/**
	 * 解析{@link ContextLoader} {@linkplain Class class}以用于提供的{@link ContextConfigurationAttributes}列表.
	 * <p>从上下文配置属性层次结构中的第一个级别开始:
	 * <ol>
	 * <li>如果{@link ContextConfigurationAttributes}的
	 * {@link ContextConfigurationAttributes#getContextLoaderClass() contextLoaderClass}
	 * 属性配置了显式类, 则将返回该类.</li>
	 * <li>如果未在层次结构中的当前级别指定显式{@code ContextLoader}类, 则遍历层次结构中的下一级别, 并返回步骤 #1.</li>
	 * </ol>
	 * 
	 * @param configAttributesList 要处理的配置属性列表; 不能是{@code null};
	 * 必须<em>自下而上</em>排序 (i.e., 好像我们正在遍历类层次结构)
	 * 
	 * @return 用于提供的配置属性的{@code ContextLoader}类, 或{@code null} 如果未找到显式加载器
	 * @throws IllegalArgumentException 如果提供的配置属性为{@code null}或<em>为空</em>
	 */
	protected Class<? extends ContextLoader> resolveExplicitContextLoaderClass(
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Resolving ContextLoader for context configuration attributes %s",
						configAttributes));
			}
			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
			if (ContextLoader.class != contextLoaderClass) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
							"Found explicit ContextLoader class [%s] for context configuration attributes %s",
							contextLoaderClass.getName(), configAttributes));
				}
				return contextLoaderClass;
			}
		}
		return null;
	}

	/**
	 * 获取{@link CacheAwareContextLoaderDelegate}以用于与{@code ContextCache}的透明交互.
	 * <p>默认实现委托给
	 * {@code getBootstrapContext().getCacheAwareContextLoaderDelegate()}.
	 * <p>具体的子类可以选择覆盖此方法以返回具有自定义
	 * {@link org.springframework.test.context.cache.ContextCache ContextCache}支持的
	 * 自定义{@code CacheAwareContextLoaderDelegate}实现.
	 * 
	 * @return 上下文加载器委托 (never {@code null})
	 */
	protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
		return getBootstrapContext().getCacheAwareContextLoaderDelegate();
	}

	/**
	 * 确定要用于提供的测试类的默认{@link ContextLoader} {@linkplain Class class}.
	 * <p>只有在未通过{@link ContextConfiguration#loader}显式声明{@code ContextLoader}类时, 才会使用此方法返回的类.
	 * 
	 * @param testClass 要检索默认{@code ContextLoader}类的测试类
	 * 
	 * @return 提供的测试类的默认{@code ContextLoader}类 (never {@code null})
	 */
	protected abstract Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass);

	/**
	 * 处理提供的, 新实例化的{@link MergedContextConfiguration}实例.
	 * <p>返回的{@link MergedContextConfiguration}实例可能是原始的包装或替换.
	 * <p>默认实现只是返回提供的未修改的实例.
	 * <p>具体的子类可以根据提供的实例中的属性选择返回{@link MergedContextConfiguration}的专用子类.
	 * 
	 * @param mergedConfig 要处理的{@code MergedContextConfiguration}; never {@code null}
	 * 
	 * @return 完全初始化的{@code MergedContextConfiguration}; never {@code null}
	 */
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		return mergedConfig;
	}


	private static boolean areAllEmpty(Collection<?>... collections) {
		for (Collection<?> collection : collections) {
			if (!collection.isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
