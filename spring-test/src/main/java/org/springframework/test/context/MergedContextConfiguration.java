package org.springframework.test.context;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code MergedContextConfiguration}封装在测试类及其所有超类上声明的<em>合并</em>上下文配置,
 * 通过{@link ContextConfiguration @ContextConfiguration},
 * {@link ActiveProfiles @ActiveProfiles}, 和{@link TestPropertySource @TestPropertySource}.
 *
 * <p>合并的上下文资源位置, 带注解的类, 活动的配置文件, 属性资源位置, 和内联属性,
 * 表示测试类层次结构中的所有声明值, 同时考虑了
 * {@link ContextConfiguration#inheritLocations}, {@link ActiveProfiles#inheritProfiles},
 * {@link TestPropertySource#inheritLocations}, 和{@link TestPropertySource#inheritProperties}标志的语义.
 *
 * <p>{@link SmartContextLoader}使用{@code MergedContextConfiguration}加载
 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
 *
 * <p>{@link org.springframework.test.context.cache.ContextCache ContextCache}还使用
 * {@code MergedContextConfiguration}作为缓存使用此{@code MergedContextConfiguration}属性加载的
 * {@link org.springframework.context.ApplicationContext ApplicationContext}的键.
 */
public class MergedContextConfiguration implements Serializable {

	private static final long serialVersionUID = -3290560718464957422L;

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	private static final Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> EMPTY_INITIALIZER_CLASSES =
			Collections.<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> emptySet();

	private static final Set<ContextCustomizer> EMPTY_CONTEXT_CUSTOMIZERS = Collections.<ContextCustomizer> emptySet();


	private final Class<?> testClass;

	private final String[] locations;

	private final Class<?>[] classes;

	private final Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses;

	private final String[] activeProfiles;

	private final String[] propertySourceLocations;

	private final String[] propertySourceProperties;

	private final Set<ContextCustomizer> contextCustomizers;

	private final ContextLoader contextLoader;

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration parent;


	private static String[] processStrings(String[] array) {
		return (array != null ? array : EMPTY_STRING_ARRAY);
	}

	private static Class<?>[] processClasses(Class<?>[] classes) {
		return (classes != null ? classes : EMPTY_CLASS_ARRAY);
	}

	private static Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> processContextInitializerClasses(
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses) {

		return (contextInitializerClasses != null ?
				Collections.unmodifiableSet(contextInitializerClasses) : EMPTY_INITIALIZER_CLASSES);
	}

	private static Set<ContextCustomizer> processContextCustomizers(Set<ContextCustomizer> contextCustomizers) {
		return (contextCustomizers != null ?
				Collections.unmodifiableSet(contextCustomizers) : EMPTY_CONTEXT_CUSTOMIZERS);
	}

	private static String[] processActiveProfiles(String[] activeProfiles) {
		if (activeProfiles == null) {
			return EMPTY_STRING_ARRAY;
		}

		// 活动的配置文件必须是唯一的
		Set<String> profilesSet = new LinkedHashSet<String>(Arrays.asList(activeProfiles));
		return StringUtils.toStringArray(profilesSet);
	}

	/**
	 * 仅基于加载器的完全限定名称或&quot;null&quot;生成所提供的{@link ContextLoader}的null-安全{@link String}表示.
	 */
	protected static String nullSafeToString(ContextLoader contextLoader) {
		return (contextLoader != null ? contextLoader.getClass().getName() : "null");
	}


	/**
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的上下文资源位置
	 * @param classes 合并的注解类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param contextLoader 已解析的{@code ContextLoader}
	 */
	public MergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			String[] activeProfiles, ContextLoader contextLoader) {

		this(testClass, locations, classes, null, activeProfiles, contextLoader);
	}

	/**
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的上下文资源位置
	 * @param classes 合并的带注解的类
	 * @param contextInitializerClasses 合并的上下文初始化器类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param contextLoader 已解析的{@code ContextLoader}
	 */
	public MergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, ContextLoader contextLoader) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, contextLoader, null, null);
	}

	/**
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的上下文资源位置
	 * @param classes 合并的带注解的类
	 * @param contextInitializerClasses 合并的上下文初始化器类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param contextLoader 已解析的{@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate 用于检索父上下文的缓存感知上下文加载器委托
	 * @param parent 父配置或{@code null}如果没有父配置
	 */
	public MergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parent) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, null, null, contextLoader,
			cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * 复制提供的{@code MergedContextConfiguration}中的所有字段.
	 */
	public MergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		this(mergedConfig.testClass, mergedConfig.locations, mergedConfig.classes,
			mergedConfig.contextInitializerClasses, mergedConfig.activeProfiles, mergedConfig.propertySourceLocations,
			mergedConfig.propertySourceProperties, mergedConfig.contextCustomizers,
			mergedConfig.contextLoader, mergedConfig.cacheAwareContextLoaderDelegate, mergedConfig.parent);
	}

	/**
	 * <p>如果为{@code locations}, {@code classes}, {@code activeProfiles},
	 * {@code propertySourceLocations}, 或{@code propertySourceProperties}提供{@code null}值, 则会存储一个空数组.
	 * 如果为{@code contextInitializerClasses}提供了{@code null}值, 则将存储一个空集.
	 * 此外, 将对活动配置文件进行排序, 并删除重复的配置文件.
	 * 
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的上下文资源位置
	 * @param classes 合并的带注解的类
	 * @param contextInitializerClasses 合并的上下文初始化器类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param propertySourceLocations 合并的{@code PropertySource}位置
	 * @param propertySourceProperties 合并的{@code PropertySource}属性
	 * @param contextLoader 已解析的{@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate 用于检索父上下文的缓存感知上下文加载器委托
	 * @param parent 父配置或{@code null}如果没有父配置
	 */
	public MergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String[] propertySourceLocations, String[] propertySourceProperties,
			ContextLoader contextLoader, CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			MergedContextConfiguration parent) {
		this(testClass, locations, classes, contextInitializerClasses, activeProfiles,
				propertySourceLocations, propertySourceProperties,
				EMPTY_CONTEXT_CUSTOMIZERS, contextLoader,
				cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * <p>如果为{@code locations}, {@code classes}, {@code activeProfiles},
	 * {@code propertySourceLocations}, 或{@code propertySourceProperties}提供了{@code null}值, 则会存储一个空数组.
	 * 如果为{@code contextInitializerClasses}或{@code contextCustomizers}提供了{@code null}值, 则会存储一个空集合.
	 * 此外, 将对活动配置文件进行排序, 并删除重复的配置文件.
	 * 
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的上下文资源位置
	 * @param classes 合并的带注解的类
	 * @param contextInitializerClasses 合并的上下文初始化器类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param propertySourceLocations 合并的{@code PropertySource}位置
	 * @param propertySourceProperties 合并的{@code PropertySource}属性
	 * @param contextCustomizers 上下文定制器
	 * @param contextLoader 已解析的{@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate 用于检索父上下文的缓存感知上下文加载器委托
	 * @param parent 父配置或{@code null}如果没有父配置
	 */
	public MergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String[] propertySourceLocations, String[] propertySourceProperties,
			Set<ContextCustomizer> contextCustomizers, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parent) {

		this.testClass = testClass;
		this.locations = processStrings(locations);
		this.classes = processClasses(classes);
		this.contextInitializerClasses = processContextInitializerClasses(contextInitializerClasses);
		this.activeProfiles = processActiveProfiles(activeProfiles);
		this.propertySourceLocations = processStrings(propertySourceLocations);
		this.propertySourceProperties = processStrings(propertySourceProperties);
		this.contextCustomizers = processContextCustomizers(contextCustomizers);
		this.contextLoader = contextLoader;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
		this.parent = parent;
	}


	/**
	 * 获取与此{@code MergedContextConfiguration}相关联的{@linkplain Class 测试类}.
	 */
	public Class<?> getTestClass() {
		return this.testClass;
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}的 {@code ApplicationContext}配置文件的合并资源位置.
	 * <p>上下文资源位置通常表示XML配置文件或Groovy脚本.
	 */
	public String[] getLocations() {
		return this.locations;
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}的合并注解类.
	 */
	public Class<?>[] getClasses() {
		return this.classes;
	}

	/**
	 * 确定此{@code MergedContextConfiguration}实例是否具有基于路径的上下文资源位置.
	 * 
	 * @return {@code true} 如果{@link #getLocations() locations}数组不为空
	 */
	public boolean hasLocations() {
		return !ObjectUtils.isEmpty(getLocations());
	}

	/**
	 * 确定此{@code MergedContextConfiguration}实例是否具有基于类的资源.
	 * 
	 * @return {@code true} 如果{@link #getClasses() classes}数组不为空
	 */
	public boolean hasClasses() {
		return !ObjectUtils.isEmpty(getClasses());
	}

	/**
	 * 确定此{@code MergedContextConfiguration}实例是否具有基于路径的上下文资源位置或基于类的资源.
	 * 
	 * @return {@code true} 如果{@link #getLocations() locations}或{@link #getClasses() classes}数组不为空
	 */
	public boolean hasResources() {
		return (hasLocations() || hasClasses());
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}的合并{@code ApplicationContextInitializer}类.
	 */
	public Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> getContextInitializerClasses() {
		return this.contextInitializerClasses;
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}的合并活动bean定义配置文件.
	 */
	public String[] getActiveProfiles() {
		return this.activeProfiles;
	}

	/**
	 * 为{@linkplain #getTestClass() 测试类}获取测试{@code PropertySources}的合并资源位置.
	 */
	public String[] getPropertySourceLocations() {
		return this.propertySourceLocations;
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}的合并测试{@code PropertySource}属性.
	 * <p>属性将被加载到{@code Environment}的{@code PropertySources}集合中.
	 */
	public String[] getPropertySourceProperties() {
		return this.propertySourceProperties;
	}

	/**
	 * 获取将在加载应用程序上下文时应用的合并的{@link ContextCustomizer ContextCustomizers}.
	 */
	public Set<ContextCustomizer> getContextCustomizers() {
		return this.contextCustomizers;
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}已解析的{@link ContextLoader}.
	 */
	public ContextLoader getContextLoader() {
		return this.contextLoader;
	}

	/**
	 * 在上下文层次结构中获取父应用程序上下文的{@link MergedContextConfiguration}.
	 * 
	 * @return 父配置或{@code null}如果没有父配置
	 */
	public MergedContextConfiguration getParent() {
		return this.parent;
	}

	/**
	 * 从上下文缓存中获取此{@code MergedContextConfiguration}定义的上下文的父{@link ApplicationContext}.
	 * <p>如果尚未加载父上下文, 则将加载它, 将其存储在缓存中, 然后返回.
	 * 
	 * @return 父级{@code ApplicationContext}或{@code null}如果没有父级
	 */
	public ApplicationContext getParentApplicationContext() {
		if (this.parent == null) {
			return null;
		}
		Assert.state(this.cacheAwareContextLoaderDelegate != null,
				"Cannot retrieve a parent application context without access to the CacheAwareContextLoaderDelegate");
		return this.cacheAwareContextLoaderDelegate.loadContext(this.parent);
	}


	/**
	 * 通过比较对象的
	 * {@linkplain #getLocations() locations},
	 * {@linkplain #getClasses() annotated classes},
	 * {@linkplain #getContextInitializerClasses() context initializer classes},
	 * {@linkplain #getActiveProfiles() active profiles},
	 * {@linkplain #getPropertySourceLocations() property source locations},
	 * {@linkplain #getPropertySourceProperties() property source properties},
	 * {@linkplain #getParent() parents},
	 * 和它们的{@link #getContextLoader() ContextLoaders}的完全限定名称,
	 * 确定提供的对象是否等于此{@code MergedContextConfiguration}实例.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}

		MergedContextConfiguration otherConfig = (MergedContextConfiguration) other;
		if (!Arrays.equals(this.locations, otherConfig.locations)) {
			return false;
		}
		if (!Arrays.equals(this.classes, otherConfig.classes)) {
			return false;
		}
		if (!this.contextInitializerClasses.equals(otherConfig.contextInitializerClasses)) {
			return false;
		}
		if (!Arrays.equals(this.activeProfiles, otherConfig.activeProfiles)) {
			return false;
		}
		if (!Arrays.equals(this.propertySourceLocations, otherConfig.propertySourceLocations)) {
			return false;
		}
		if (!Arrays.equals(this.propertySourceProperties, otherConfig.propertySourceProperties)) {
			return false;
		}
		if (!this.contextCustomizers.equals(otherConfig.contextCustomizers)) {
			return false;
		}

		if (this.parent == null) {
			if (otherConfig.parent != null) {
				return false;
			}
		}
		else if (!this.parent.equals(otherConfig.parent)) {
			return false;
		}

		if (!nullSafeToString(this.contextLoader).equals(nullSafeToString(otherConfig.contextLoader))) {
			return false;
		}

		return true;
	}

	/**
	 * 为{@code MergedContextConfiguration}的所有属性生成唯一的哈希码, 不包括{@linkplain #getTestClass() 测试类}.
	 */
	@Override
	public int hashCode() {
		int result = Arrays.hashCode(this.locations);
		result = 31 * result + Arrays.hashCode(this.classes);
		result = 31 * result + this.contextInitializerClasses.hashCode();
		result = 31 * result + Arrays.hashCode(this.activeProfiles);
		result = 31 * result + Arrays.hashCode(this.propertySourceLocations);
		result = 31 * result + Arrays.hashCode(this.propertySourceProperties);
		result = 31 * result + this.contextCustomizers.hashCode();
		result = 31 * result + (this.parent != null ? this.parent.hashCode() : 0);
		result = 31 * result + nullSafeToString(this.contextLoader).hashCode();
		return result;
	}

	/**
	 * Provide a String representation of the {@linkplain #getTestClass() test class},
	 * {@linkplain #getLocations() locations}, {@linkplain #getClasses() annotated classes},
	 * {@linkplain #getContextInitializerClasses() context initializer classes},
	 * {@linkplain #getActiveProfiles() active profiles},
	 * {@linkplain #getPropertySourceLocations() property source locations},
	 * {@linkplain #getPropertySourceProperties() property source properties},
	 * {@linkplain #getContextCustomizers() context customizers},
	 * the name of the {@link #getContextLoader() ContextLoader}, and the
	 * {@linkplain #getParent() parent configuration}.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", this.testClass)
				.append("locations", ObjectUtils.nullSafeToString(this.locations))
				.append("classes", ObjectUtils.nullSafeToString(this.classes))
				.append("contextInitializerClasses", ObjectUtils.nullSafeToString(this.contextInitializerClasses))
				.append("activeProfiles", ObjectUtils.nullSafeToString(this.activeProfiles))
				.append("propertySourceLocations", ObjectUtils.nullSafeToString(this.propertySourceLocations))
				.append("propertySourceProperties", ObjectUtils.nullSafeToString(this.propertySourceProperties))
				.append("contextCustomizers", this.contextCustomizers)
				.append("contextLoader", nullSafeToString(this.contextLoader))
				.append("parent", this.parent)
				.toString();
	}

}
