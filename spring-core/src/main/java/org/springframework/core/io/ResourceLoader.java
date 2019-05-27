package org.springframework.core.io;

import org.springframework.util.ResourceUtils;

/**
 * 用于加载资源的策略接口 (e.. 类路径或文件系统资源).
 * 需要{@link org.springframework.context.ApplicationContext}来提供此功能,
 * 以及扩展的{@link org.springframework.core.io.support.ResourcePatternResolver}支持.
 *
 * <p>{@link DefaultResourceLoader}是一个独立的实现, 可以在ApplicationContext外部使用, 也可以由{@link ResourceEditor}使用.
 *
 * <p>使用特定上下文的资源加载策略, 在ApplicationContext中运行时, 可以从Strings填充Resource和Resource数组类型的Bean属性.
 */
public interface ResourceLoader {

	/** 从类路径加载的伪URL前缀: "classpath:" */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * 返回指定资源位置的Resource句柄.
	 * <p>句柄应始终是可重用的资源描述符, 允许多个{@link Resource#getInputStream()}调用.
	 * <p><ul>
	 * <li>必须支持完全限定的URL, e.g. "file:C:/test.dat".
	 * <li>必须支持classpath伪URL, e.g. "classpath:test.dat".
	 * <li>应该支持相对文件路径, e.g. "WEB-INF/test.dat".
	 * (这将是特定于实现的, 通常由ApplicationContext实现提供.)
	 * </ul>
	 * <p>请注意, Resource句柄并不意味着现有资源;
	 * 需要调用{@link Resource#exists}来检查是否存在.
	 * 
	 * @param location 资源位置
	 * 
	 * @return 相应的资源句柄 (never {@code null})
	 */
	Resource getResource(String location);

	/**
	 * 公开此ResourceLoader使用的ClassLoader.
	 * <p>需要直接访问ClassLoader的客户端可以使用ResourceLoader以统一的方式执行此操作, 而不是依赖于线程上下文ClassLoader.
	 * 
	 * @return ClassLoader (如果连系统ClassLoader都不可访问, 则只有{@code null})
	 */
	ClassLoader getClassLoader();

}
