package org.springframework.test.context.web;

import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code WebMergedContextConfiguration}通过
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration},
 * {@link WebAppConfiguration @WebAppConfiguration},
 * 和{@link org.springframework.test.context.ActiveProfiles @ActiveProfiles}
 * 封装在测试类及其所有超类上声明的<em>合并</em>上下文配置.
 *
 * <p>{@code WebMergedContextConfiguration}通过添加对通过{@code @WebAppConfiguration}
 * 配置的{@link #getResourceBasePath() 资源库路径}的支持来扩展{@link MergedContextConfiguration}的约定.
 * 这允许{@link org.springframework.test.context.TestContext TestContext}
 * 正确缓存使用此{@code WebMergedContextConfiguration}的属性加载的相应
 * {@link org.springframework.web.context.WebApplicationContext WebApplicationContext}.
 */
public class WebMergedContextConfiguration extends MergedContextConfiguration {

	private static final long serialVersionUID = 7323361588604247458L;

	private final String resourceBasePath;


	/**
	 * <p>委托给
	 * {@link #WebMergedContextConfiguration(Class, String[], Class[], Set, String[], String[], String[], String, ContextLoader, CacheAwareContextLoaderDelegate, MergedContextConfiguration)}.
	 * 
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的资源位置
	 * @param classes 合并的带注解的类
	 * @param contextInitializerClasses 合并的上下文初始化类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param resourceBasePath Web应用程序根目录的资源路径
	 * @param contextLoader 已解析的{@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate 用于检索父上下文的缓存感知上下文加载器委托
	 * @param parent 父级配置或{@code null}
	 * 
	 * @since 3.2.2
	 * @deprecated as of Spring 4.1, use
	 * {@link #WebMergedContextConfiguration(Class, String[], Class[], Set, String[], String[], String[], String, ContextLoader, CacheAwareContextLoaderDelegate, MergedContextConfiguration)}
	 * instead.
	 */
	@Deprecated
	public WebMergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String resourceBasePath, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parent) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, null, null, resourceBasePath,
			contextLoader, cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * 复制构造函数
	 * <p>如果为{@code resourceBasePath}提供了<em>empty</em>值, 则将使用空字符串.
	 * 
	 * @param resourceBasePath Web应用程序根目录的资源路径
	 */
	public WebMergedContextConfiguration(MergedContextConfiguration mergedConfig, String resourceBasePath) {
		super(mergedConfig);
		this.resourceBasePath = !StringUtils.hasText(resourceBasePath) ? "" : resourceBasePath;
	}

	/**
	 * <p>如果为{@code locations}, {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
	 * 或{@code propertySourceProperties}提供了{@code null}值, 则会存储一个空数组.
	 * 如果为{@code contextInitializerClasses}提供了{@code null}值, 则将存储一个空集.
	 * 如果为{@code resourceBasePath}提供了<em>empty</em>值, 则将使用空字符串.
	 * 此外, 将对活动配置文件进行排序, 并删除重复的配置文件.
	 * 
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的资源位置
	 * @param classes 合并的带注解的类
	 * @param contextInitializerClasses 合并的上下文初始化类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param propertySourceLocations 合并的{@code PropertySource}位置
	 * @param propertySourceProperties 合并的{@code PropertySource}属性
	 * @param resourceBasePath Web应用程序根目录的资源路径
	 * @param contextLoader 已解析的{@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate 用于检索父级上下文的缓存感知上下文加载器委托
	 * @param parent 父级配置或{@code null}
	 */
	public WebMergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String[] propertySourceLocations, String[] propertySourceProperties,
			String resourceBasePath, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parent) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceLocations,
			propertySourceProperties, null, resourceBasePath, contextLoader, cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * <p>如果为{@code locations}, {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
	 * 或{@code propertySourceProperties}提供了{@code null}值, 则会存储一个空数组.
	 * 如果为{@code contextInitializerClasses} 或 {@code contextCustomizers}提供了{@code null}值, 则会存储一个空集合.
	 * 如果为{@code resourceBasePath}提供了<em>empty</em>值, 则将使用空字符串.
	 * 此外, 将对活动配置文件进行排序, 并删除重复的配置文件.
	 * 
	 * @param testClass 合并配置的测试类
	 * @param locations 合并的上下文资源位置
	 * @param classes 合并的带注解的类
	 * @param contextInitializerClasses 合并的上下文初始化类
	 * @param activeProfiles 合并的活动bean定义配置文件
	 * @param propertySourceLocations 合并的{@code PropertySource}位置
	 * @param propertySourceProperties 合并的{@code PropertySource}属性
	 * @param contextCustomizers 上下文定制器
	 * @param resourceBasePath Web应用程序根目录的资源路径
	 * @param contextLoader 已解析的{@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate 用于检索父级上下文的缓存感知上下文加载器委托
	 * @param parent 父级配置或{@code null}
	 */
	public WebMergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String[] propertySourceLocations, String[] propertySourceProperties,
			Set<ContextCustomizer> contextCustomizers, String resourceBasePath, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parent) {

		super(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceLocations,
			propertySourceProperties, contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parent);

		this.resourceBasePath = (StringUtils.hasText(resourceBasePath) ? resourceBasePath : "");
	}

	/**
	 * 获取{@linkplain #getTestClass() 测试类}的Web应用程序根目录的资源路径, 通过{@code @WebAppConfiguration}配置.
	 */
	public String getResourceBasePath() {
		return this.resourceBasePath;
	}


	/**
	 * 通过比较两个对象的{@linkplain #getLocations() 位置},
	 * {@linkplain #getClasses() 带注解的类},
	 * {@linkplain #getContextInitializerClasses() 上下文初始化类},
	 * {@linkplain #getActiveProfiles() 活动的配置文件},
	 * {@linkplain #getResourceBasePath() 资源基础路径},
	 * {@linkplain #getParent() 父级}, 以及{@link #getContextLoader() ContextLoaders}的完全限定名称,
	 * 确定提供的对象是否等于此{@code WebMergedContextConfiguration}实例.
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (super.equals(other) &&
				this.resourceBasePath.equals(((WebMergedContextConfiguration) other).resourceBasePath)));
	}

	/**
	 * 为{@code WebMergedContextConfiguration}的所有属性生成唯一的哈希码, 不包括{@linkplain #getTestClass() 测试类}.
	 */
	@Override
	public int hashCode() {
		return super.hashCode() * 31 + this.resourceBasePath.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", getTestClass())
				.append("locations", ObjectUtils.nullSafeToString(getLocations()))
				.append("classes", ObjectUtils.nullSafeToString(getClasses()))
				.append("contextInitializerClasses", ObjectUtils.nullSafeToString(getContextInitializerClasses()))
				.append("activeProfiles", ObjectUtils.nullSafeToString(getActiveProfiles()))
				.append("propertySourceLocations", ObjectUtils.nullSafeToString(getPropertySourceLocations()))
				.append("propertySourceProperties", ObjectUtils.nullSafeToString(getPropertySourceProperties()))
				.append("contextCustomizers", getContextCustomizers())
				.append("resourceBasePath", getResourceBasePath())
				.append("contextLoader", nullSafeToString(getContextLoader()))
				.append("parent", getParent())
				.toString();
	}

}
