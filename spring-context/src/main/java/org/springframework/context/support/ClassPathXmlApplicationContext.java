package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * 独立的XML应用程序上下文, 从类路径获取上下文定义文件, 将普通路径解释为包含包路径的类路径资源名称 (e.g. "mypackage/myresource.txt").
 * 适用于测试工具以及JAR中嵌入的应用程序上下文.
 *
 * <p>可以通过 {@link #getConfigLocations}覆盖配置位置默认值,
 * 配置位置可以表示 "/myfiles/context.xml"等具体文件, 也可以表示 "/myfiles/*-context.xml"等Ant样式模式
 * (see the {@link org.springframework.util.AntPathMatcher} javadoc for pattern details).
 *
 * <p>Note: 如果有多个配置位置, 以后的bean定义将覆盖先前加载的文件中定义的那些.
 * 这可以用来通过额外的XML文件故意覆盖某些bean定义.
 *
 * <p><b>这是一个简单的一站式的便利ApplicationContext.
 * 考虑将 {@link GenericApplicationContext}类与
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}结合使用, 以实现更灵活的上下文设置.</b>
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

	private Resource[] configResources;


	/**
	 * 为bean样式配置, 创建一个新的ClassPathXmlApplicationContext.
	 */
	public ClassPathXmlApplicationContext() {
	}

	/**
	 * 为bean样式配置, 创建一个新的ClassPathXmlApplicationContext.
	 * 
	 * @param parent 父级上下文
	 */
	public ClassPathXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * 从给定的XML文件加载定义并自动刷新上下文.
	 * 
	 * @param configLocation 资源位置
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[] {configLocation}, true, null);
	}

	/**
	 * 从给定的XML文件加载定义, 并自动刷新上下文.
	 * 
	 * @param configLocations 资源位置数组
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * 从给定的XML文件加载定义, 并自动刷新上下文.
	 * 
	 * @param configLocations 资源位置数组
	 * @param parent 父级上下文
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
		this(configLocations, true, parent);
	}

	/**
	 * 从给定的XML文件加载定义.
	 * 
	 * @param configLocations 资源位置数组
	 * @param refresh 是否自动刷新上下文, 加载所有bean定义并创建所有单例.
	 * 或者, 在进一步配置上下文后手动调用刷新.
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * 从给定的XML文件加载定义.
	 * 
	 * @param configLocations 资源位置数组
	 * @param refresh 是否自动刷新上下文, 加载所有bean定义并创建所有单例.
	 * 或者, 在进一步配置上下文后手动调用刷新.
	 * @param parent 父级上下文
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * 从给定的XML文件加载定义并自动刷新上下文.
	 * <p>这是一种相对于给定Class, 加载类路径资源的便捷方法.
	 * 要获得完全的灵活性, 请考虑将GenericApplicationContext与XmlBeanDefinitionReader和ClassPathResource参数一起使用.
	 * 
	 * @param path 类路径中的相对(或绝对)路径
	 * @param clazz 用于加载资源的类 (给定路径的基础)
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
		this(new String[] {path}, clazz);
	}

	/**
	 * 从给定的XML文件加载定义并自动刷新上下文.
	 * 
	 * @param paths 类路径中的相对(或绝对)路径
	 * @param clazz 用于加载资源的类 (给定路径的基础)
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz) throws BeansException {
		this(paths, clazz, null);
	}

	/**
	 * 从给定的XML文件加载定义并自动刷新上下文.
	 * 
	 * @param paths 类路径中的相对(或绝对)路径
	 * @param clazz 用于加载资源的类 (给定路径的基础)
	 * @param parent 父级上下文
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz, ApplicationContext parent)
			throws BeansException {

		super(parent);
		Assert.notNull(paths, "Path array must not be null");
		Assert.notNull(clazz, "Class argument must not be null");
		this.configResources = new Resource[paths.length];
		for (int i = 0; i < paths.length; i++) {
			this.configResources[i] = new ClassPathResource(paths[i], clazz);
		}
		refresh();
	}


	@Override
	protected Resource[] getConfigResources() {
		return this.configResources;
	}

}
