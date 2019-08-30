package org.springframework.web.portlet.context;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * 基于Portlet的{@link org.springframework.web.context.WebApplicationContext}实现,
 * 它从XML文档中获取其配置, 由{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}理解.
 * 对于portlet环境, 这基本上等同于{@link org.springframework.context.support.AbstractXmlApplicationContext}.
 *
 * <p>默认情况下, 配置将从"/WEB-INF/applicationContext.xml"获取根上下文,
 * 而从"/WEB-INF/test-portlet.xml"获取具有命名空间"test-portlet"的上下文 (如对于具有portlet-name "test"的DispatcherPortlet实例).
 *
 * <p>可以通过{@link org.springframework.web.portlet.FrameworkPortlet}的"contextConfigLocation" portlet init-param覆盖配置位置默认值.
 * 配置位置可以表示"/WEB-INF/context.xml"等具体文件, 也可以表示"/WEB-INF/*-context.xml"等Ant样式模式
 * (有关模式详细信息, 请参阅{@link org.springframework.util.PathMatcher} javadoc).
 *
 * <p>Note: 如果有多个配置位置, 以后的bean定义将覆盖先前加载的文件中定义的那些.
 * 这可以用来通过额外的XML文件故意覆盖某些bean定义.
 *
 * <p><b>对于以不同bean定义格式读取的基于Portlet的上下文, 创建{@link AbstractRefreshablePortletApplicationContext}的类似子类.</b>
 * 可以将这样的上下文实现指定为FrameworkPortlet实例的"contextClass" init-param.
 */
public class XmlPortletApplicationContext extends AbstractRefreshablePortletApplicationContext {

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
		beanDefinitionReader.setEnvironment(this.getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// 允许子类提供读取器的自定义初始化, 然后继续实际加载bean定义.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文的bean定义的bean定义读取器.
	 * 默认实现为空.
	 * <p>可以在子类中重写, e.g. 用于关闭XML验证或使用不同的XmlBeanDefinitionParser实现.
	 * 
	 * @param beanDefinitionReader 此上下文使用的bean定义读取器
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的XmlBeanDefinitionReader加载bean定义.
	 * <p>bean工厂的生命周期由refreshBeanFactory方法处理; 因此, 这个方法只是加载和/或注册bean定义.
	 * <p>委托ResourcePatternResolver将位置模式解析为Resource实例.
	 * 
	 * @throws org.springframework.beans.BeansException 在bean注册错误的情况下
	 * @throws java.io.IOException 如果找不到所需的XML文档
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (int i = 0; i < configLocations.length; i++) {
				reader.loadBeanDefinitions(configLocations[i]);
			}
		}
	}

	/**
	 * 根上下文的默认位置是"/WEB-INF/applicationContext.xml", 以及"/WEB-INF/test-portlet.xml",
	 * 用于具有命名空间"test-portlet"的上下文 (对于具有portlet-name "test"的DispatcherPortlet实例).
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
