package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * {@link org.springframework.context.ApplicationContext}实现的基类,
 * 从包含由{@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}理解的bean定义的XML文档中绘制配置.
 *
 * <p>子类只需要实现 {@link #getConfigResources}和/或{@link #getConfigLocations}方法.
 * 此外, 它们可能会覆盖 {@link #getResourceByPath}挂钩, 以特定于环境的方式解释相对路径,
 * 和/或{@link #getResourcePatternResolver}以扩展模式解析.
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

	private boolean validating = true;


	public AbstractXmlApplicationContext() {
	}

	/**
	 * @param parent 父级上下文
	 */
	public AbstractXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 设置是否使用XML验证. 默认{@code true}.
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}


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
	 * @param reader 此上下文使用的bean定义读取器
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
		reader.setValidating(this.validating);
	}

	/**
	 * 使用给定的XmlBeanDefinitionReader加载bean定义.
	 * <p>bean工厂的生命周期由{@link #refreshBeanFactory}方法处理; 因此, 这个方法只是加载和/或注册bean定义.
	 * 
	 * @param reader 要使用的XmlBeanDefinitionReader
	 * 
	 * @throws BeansException 在bean注册错误的情况下
	 * @throws IOException 如果找不到所需的XML文档
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		Resource[] configResources = getConfigResources();
		if (configResources != null) {
			reader.loadBeanDefinitions(configResources);
		}
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			reader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * 返回一个Resource对象数组, 引用应该构建此上下文的XML bean定义文件.
	 * <p>默认实现返回 {@code null}. 子类可以覆盖它以提供预构建的Resource对象, 而不是位置字符串.
	 * 
	 * @return Resource对象数组, 或{@code null}
	 */
	protected Resource[] getConfigResources() {
		return null;
	}
}
