package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.io.ResourceLoader;

/**
 * 希望被通知其运行的 <b>ResourceLoader</b> (通常是 ApplicationContext) 的对象实现的接口.
 * 这是通过ApplicationContextAware接口替代完整的ApplicationContext依赖项.
 *
 * <p>请注意, 资源依赖项也可以作为Resource类型的bean属性公开,
 * 通过字符串填充由bean工厂自动进行类型转换.
 * 这样就不需要为了访问特定的文件资源而实现任何回调接口.
 *
 * <p>当您的应用程序对象必须访问其名称已计算的各种文件资源时, 通常需要ResourceLoader.
 * 一个好的策略是使对象使用DefaultResourceLoader, 但仍然实现ResourceLoaderAware以允许在ApplicationContext中运行时重写.
 * 有关示例, 请参阅ReloadableResourceBundleMessageSource.
 *
 * <p>也可以为了<b>ResourcePatternResolver</b>接口, 检查传入的ResourceLoader并进行相应的转换,
 * 能够将资源模式解析为Resource对象的数组.
 * 在ApplicationContext中运行时, 这将始终有效 (上下文接口扩展了ResourcePatternResolver).
 * 使用PathMatchingResourcePatternResolver作为默认值.
 * 另请参阅{@code ResourcePatternUtils.getResourcePatternResolver}方法.
 *
 * <p>作为ResourcePatternResolver依赖项的替代方案, 请考虑公开Resource数组类型的bean属性, 通过模式字符串填充, 由bean工厂自动进行类型转换.
 */
public interface ResourceLoaderAware extends Aware {

	/**
	 * 设置此对象运行的ResourceLoader.
	 * <p>这可能是 ResourcePatternResolver, 可以通过{@code instanceof ResourcePatternResolver}进行检查.
	 * 另请参阅{@code ResourcePatternUtils.getResourcePatternResolver}方法.
	 * <p>在普通bean属性填充之后, 但在初始化回调之前调用, 例如InitializingBean的{@code afterPropertiesSet}或自定义init方法.
	 * 在ApplicationContextAware的{@code setApplicationContext}之前调用.
	 * 
	 * @param resourceLoader 此对象要使用的ResourceLoader对象
	 */
	void setResourceLoader(ResourceLoader resourceLoader);

}
