package org.springframework.context.support;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 内置XML支持的应用程序上下文.
 * 这是{@link ClassPathXmlApplicationContext} 和 {@link FileSystemXmlApplicationContext}的灵活替代方案,
 * 可通过setter配置, 最终{@link #refresh()}调用激活上下文.
 *
 * <p>如果有多个配置文件, 以后文件中的bean定义将覆盖早期文件中定义的bean定义.
 * 可用于通过附加到列表的额外配置文件有意覆盖某些bean定义.
 */
public class GenericXmlApplicationContext extends GenericApplicationContext {

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);


	/**
	 * 创建需要 {@link #load loaded}, 然后手动 {@link #refresh refreshed}的GenericXmlApplicationContext.
	 */
	public GenericXmlApplicationContext() {
	}

	/**
	 * 从给定资源加载bean定义并自动刷新上下文.
	 * 
	 * @param resources 要加载的资源
	 */
	public GenericXmlApplicationContext(Resource... resources) {
		load(resources);
		refresh();
	}

	/**
	 * 从给定的资源位置加载bean定义并自动刷新上下文.
	 * 
	 * @param resourceLocations 要加载的资源
	 */
	public GenericXmlApplicationContext(String... resourceLocations) {
		load(resourceLocations);
		refresh();
	}

	/**
	 * 从给定的资源位置加载bean定义并自动刷新上下文.
	 * 
	 * @param relativeClass 加载每个指定资源名称时, 其包将用作前缀的类
	 * @param resourceNames 要加载的资源的相对合格的名称
	 */
	public GenericXmlApplicationContext(Class<?> relativeClass, String... resourceNames) {
		load(relativeClass, resourceNames);
		refresh();
	}


	/**
	 * 暴露底层的 {@link XmlBeanDefinitionReader}, 用于额外的配置和{@code loadBeanDefinition} 变体.
	 */
	public final XmlBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * 设置是否使用XML验证. 默认 {@code true}.
	 */
	public void setValidating(boolean validating) {
		this.reader.setValidating(validating);
	}

	/**
	 * 将给定环境委托给底层 {@link XmlBeanDefinitionReader}.
	 * 应该在调用{@code #load}之前调用.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(getEnvironment());
	}


	//---------------------------------------------------------------------
	// Convenient methods for loading XML bean definition files
	//---------------------------------------------------------------------

	/**
	 * 从给定的XML资源加载bean定义.
	 * 
	 * @param resources 要加载的一个或多个资源
	 */
	public void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}

	/**
	 * 从给定的XML资源加载bean定义.
	 * 
	 * @param resourceLocations 要加载的一个或多个资源位置
	 */
	public void load(String... resourceLocations) {
		this.reader.loadBeanDefinitions(resourceLocations);
	}

	/**
	 * 从给定的XML资源加载bean定义.
	 * 
	 * @param relativeClass 加载每个指定资源名称时, 其包将用作前缀的类
	 * @param resourceNames 要加载的资源的相对合格的名称
	 */
	public void load(Class<?> relativeClass, String... resourceNames) {
		Resource[] resources = new Resource[resourceNames.length];
		for (int i = 0; i < resourceNames.length; i++) {
			resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
		}
		this.load(resources);
	}

}
