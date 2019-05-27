package org.springframework.beans.factory.xml;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

/**
 * {@link DefaultListableBeanFactory}的便捷扩展, 它从XML文档中读取bean定义.
 * 委托给{@link XmlBeanDefinitionReader}下面; 实际上相当于使用带有DefaultListableBeanFactory的XmlBeanDefinitionReader.
 *
 * <p>所需XML文档的结构, 元素和属性名称在此类中进行了硬编码.
 * (当然, 如果需要, 可以运行转换来生成这种格式).
 * "beans" 不需要是XML文档的根元素: 该类将解析XML文件中的所有bean定义元素.
 *
 * <p>此类使用{@link DefaultListableBeanFactory}超类注册每个bean定义, 并依赖于后者对{@link BeanFactory}接口的实现.
 * 它支持单例, 原型和对这些bean中的任何一种的引用.
 * 有关选项和配置样式的详细信息, 请参阅{@code "spring-beans-3.x.xsd"} (或历史上 {@code "spring-beans-2.0.dtd"}).
 *
 * <p><b>对于高级需求, 请考虑将 {@link DefaultListableBeanFactory}与 {@link XmlBeanDefinitionReader}一起使用.</b>
 * 后者允许从多个XML资源中读取, 并且在其实际的XML解析行为中具有高度可配置性.
 *
 * @deprecated as of Spring 3.1 in favor of {@link DefaultListableBeanFactory} and {@link XmlBeanDefinitionReader}
 */
@Deprecated
@SuppressWarnings({"serial", "all"})
public class XmlBeanFactory extends DefaultListableBeanFactory {

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);


	/**
	 * 使用给定的资源创建一个新的XmlBeanFactory, 该资源必须可以使用DOM进行解析.
	 * 
	 * @param resource 从中加载bean定义的XML资源
	 * 
	 * @throws BeansException 在加载或解析错误的情况下
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * 使用给定的输入流创建一个新的XmlBeanFactory, 它必须可以使用DOM进行解析.
	 * 
	 * @param resource 从中加载bean定义的XML资源
	 * @param parentBeanFactory 父级bean工厂
	 * 
	 * @throws BeansException 在加载或解析错误的情况下
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
		super(parentBeanFactory);
		this.reader.loadBeanDefinitions(resource);
	}

}
