package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * 独立的XML应用程序上下文, 从文件系统或URL中获取上下文定义文件,
 * 将普通路径解释为相对文件系统位置 (e.g. "mydir/myfile.txt").
 * 适用于测试工具以及独立环境.
 *
 * <p><b>NOTE:</b> 普通路径将始终被解释为相对于当前VM工作目录, 即使它们以斜杠开头.
 * (这与Servlet容器中的语义一致.)
 * <b>使用显式 "file:" 前缀来强制执行绝对文件路径.</b>
 *
 * <p>可以通过{@link #getConfigLocations}覆盖配置位置默认值,
 * 配置位置可以表示"/myfiles/context.xml"等具体文件, 也可以表示 "/myfiles/*-context.xml"等Ant样式模式
 * (see the {@link org.springframework.util.AntPathMatcher} javadoc for pattern details).
 *
 * <p>Note: 如果有多个配置位置, 以后的bean定义将覆盖先前加载的文件中定义的那些.
 * 这可以用来通过额外的XML文件故意覆盖某些bean定义.
 *
 * <p><b>这是一个简单的一站式的便利ApplicationContext.
 * 考虑将{@link GenericApplicationContext}类
 * 与{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}结合使用, 以实现更灵活的上下文设置.</b>
 */
public class FileSystemXmlApplicationContext extends AbstractXmlApplicationContext {

	/**
	 * 为bean样式配置创建新的FileSystemXmlApplicationContext.
	 */
	public FileSystemXmlApplicationContext() {
	}

	/**
	 * 为bean样式配置创建新的FileSystemXmlApplicationContext.
	 * 
	 * @param parent 父级上下文
	 */
	public FileSystemXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * 从给定的XML文件加载定义并自动刷新上下文.
	 * 
	 * @param configLocation 文件路径
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[] {configLocation}, true, null);
	}

	/**
	 * 从给定的XML文件加载定义并自动刷新上下文.
	 * 
	 * @param configLocations 文件路径
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * 从给定的XML文件加载定义并自动刷新上下文.
	 * 
	 * @param configLocations 文件路径
	 * @param parent 父级上下文
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
		this(configLocations, true, parent);
	}

	/**
	 * 从给定的XML文件加载定义.
	 * 
	 * @param configLocations 文件路径
	 * @param refresh 是否自动刷新上下文, 加载所有bean定义并创建所有单例.
	 * 或者, 在进一步配置上下文后手动调用刷新.
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * 从给定的XML文件加载定义.
	 * 
	 * @param configLocations 文件路径
	 * @param refresh 是否自动刷新上下文, 加载所有bean定义并创建所有单例.
	 * 或者, 在进一步配置上下文后手动调用刷新.
	 * @param parent 父级上下文
	 * 
	 * @throws BeansException 如果上下文创建失败
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * 将资源路径解析为文件系统路径.
	 * <p>Note: 即使给定路径以斜杠开头, 它也会被解释为相对于当前VM工作目录.
	 * 这与Servlet容器中的语义一致.
	 * 
	 * @param path 资源的路径
	 * 
	 * @return Resource handle
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		if (path != null && path.startsWith("/")) {
			path = path.substring(1);
		}
		return new FileSystemResource(path);
	}

}
