package org.springframework.web.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * {@link org.springframework.web.context.WebApplicationContext}实现,
 * 它从XML文档中获取其配置, 由{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}理解.
 * 这基本上相当于Web环境的
 * {@link org.springframework.context.support.GenericXmlApplicationContext}.
 *
 * <p>默认情况下, 配置将从"/WEB-INF/applicationContext.xml"获取根上下文,
 * 而"/WEB-INF/test-servlet.xml"获取具有命名空间"test-servlet"的上下文
 * (例如具有servlet-name "test"的DispatcherServlet实例).
 *
 * <p>配置位置默认值可以通过{@link org.springframework.web.context.ContextLoader}的"contextConfigLocation" context-param
 * 和{@link org.springframework.web.servlet.FrameworkServlet}的servlet init-param覆盖.
 * 配置位置可以表示具体文件, 如"/WEB-INF/context.xml"或Ant样式模式"/WEB-INF/*-context.xml"
 * (有关模式详情, 请参阅{@link org.springframework.util.PathMatcher} javadoc).
 *
 * <p>Note: 如果有多个配置位置, 以后的bean定义将覆盖先前加载的文件中定义的那些.
 * 这可以用来通过额外的XML文件故意覆盖某些bean定义.
 *
 * <p><b>对于读取不同bean定义格式的WebApplicationContext, 创建{@link AbstractRefreshableWebApplicationContext}的类似子类.</b>
 * 这样的上下文实现可以指定为ContextLoader的"contextClass" context-param或FrameworkServlet的"contextClass" init-param.
 */
public class XmlWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	/** 根上下文的默认配置位置 */
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

	/** 用于构建命名空间的配置位置的默认前缀 */
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

	/** 用于构建命名空间的配置位置的默认后缀 */
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";


	/**
	 * 通过XmlBeanDefinitionReader加载bean定义.
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// 为给定的BeanFactory创建一个新的XmlBeanDefinitionReader.
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// 使用此上下文的资源加载环境配置bean定义读取器.
		beanDefinitionReader.setEnvironment(getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// 允许子类提供读取器的自定义初始化, 然后继续实际加载bean定义.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文的bean定义的bean定义读取器. 默认实现为空.
	 * <p>可以在子类中重写, e.g. 用于关闭XML验证或使用不同的XmlBeanDefinitionParser实现.
	 * 
	 * @param beanDefinitionReader 此上下文使用的bean定义读取器
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的XmlBeanDefinitionReader加载bean定义.
	 * <p>bean工厂的生命周期由refreshBeanFactory方法处理; 因此, 这个方法只是加载和/或注册bean定义.
	 * <p>委托给ResourcePatternResolver将位置模式解析为Resource实例.
	 * 
	 * @throws IOException 如果找不到所需的XML文档
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				reader.loadBeanDefinitions(configLocation);
			}
		}
	}

	/**
	 * 对于具有命名空间"test-servlet"的上下文, 根上下文的默认位置是"/WEB-INF/applicationContext.xml",
	 * 和"/WEB-INF/test-servlet.xml" (比如具有servlet-name "test"的DispatcherServlet实例).
	 */
	@Override
	protected String[] getDefaultConfigLocations() {
		if (getNamespace() != null) {
			return new String[] {DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace() + DEFAULT_CONFIG_LOCATION_SUFFIX};
		}
		else {
			return new String[] {DEFAULT_CONFIG_LOCATION};
		}
	}
}
