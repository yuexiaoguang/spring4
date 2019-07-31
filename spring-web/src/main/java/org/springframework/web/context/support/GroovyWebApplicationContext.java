package org.springframework.web.context.support;

import java.io.IOException;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * {@link org.springframework.web.context.WebApplicationContext}实现,
 * 它从Groovy bean定义脚本和/或XML文件获取其配置, 如
 * an {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader}所理解的.
 * 这基本上相当于Web环境的
 * {@link org.springframework.context.support.GenericGroovyApplicationContext}.
 *
 * <p>默认情况下, 配置将从"/WEB-INF/applicationContext.groovy"获取根上下文,
 * 而"/WEB-INF/test-servlet.groovy"获取具有命名空间"test-servlet"的上下文
 * (对于具有servlet-name "test"的DispatcherServlet实例).
 *
 * <p>配置位置默认值可以通过{@link org.springframework.web.context.ContextLoader}的"contextConfigLocation" context-param
 * 和{@link org.springframework.web.servlet.FrameworkServlet}的servlet init-param覆盖.
 * 配置位置可以表示"/WEB-INF/context.groovy"等具体文件, 也可以表示"/WEB-INF/*-context.groovy"等Ant样式模式
 * (有关模式详细信息, 请参阅{@link org.springframework.util.PathMatcher} javadoc).
 * 请注意".xml"文件将被解析为XML内容; 所有其他类型的资源将被解析为Groovy脚本.
 *
 * <p>Note: 如果有多个配置位置, 以后的bean定义将覆盖先前加载的文件中定义的那些.
 * 这可以用来通过额外的Groovy脚本故意覆盖某些bean定义.
 *
 * <p><b>对于读取不同bean定义格式的WebApplicationContext, 创建{@link AbstractRefreshableWebApplicationContext}的类似子类.</b>
 * 这样的上下文实现可以指定为ContextLoader的"contextClass" context-param 或FrameworkServlet的"contextClass" init-param.
 */
public class GroovyWebApplicationContext extends AbstractRefreshableWebApplicationContext implements GroovyObject {

	/** 根上下文的默认配置位置 */
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.groovy";

	/** 用于构建命名空间的配置位置的默认前缀 */
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

	/** 用于构建命名空间的配置位置的默认后缀 */
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".groovy";


	private final BeanWrapper contextWrapper = new BeanWrapperImpl(this);

	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());


	/**
	 * 通过GroovyBeanDefinitionReader加载bean定义.
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// Create a new XmlBeanDefinitionReader for the given BeanFactory.
		GroovyBeanDefinitionReader beanDefinitionReader = new GroovyBeanDefinitionReader(beanFactory);

		// 使用此上下文的资源加载环境, 配置bean定义读取器.
		beanDefinitionReader.setEnvironment(getEnvironment());
		beanDefinitionReader.setResourceLoader(this);

		// 允许子类提供读取器的自定义初始化, 然后继续实际加载bean定义.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文的bean定义的bean定义读取器. 默认实现为空.
	 * <p>可以在子类中重写.
	 * 
	 * @param beanDefinitionReader 此上下文使用的bean定义读取器
	 */
	protected void initBeanDefinitionReader(GroovyBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的GroovyBeanDefinitionReader加载bean定义.
	 * <p>bean工厂的生命周期由refreshBeanFactory方法处理; 因此, 这个方法只是加载和/或注册bean定义.
	 * <p>委托给ResourcePatternResolver将位置模式解析为Resource实例.
	 * 
	 * @throws IOException 如果找不到所需的Groovy脚本或XML文件
	 */
	protected void loadBeanDefinitions(GroovyBeanDefinitionReader reader) throws IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				reader.loadBeanDefinitions(configLocation);
			}
		}
	}

	/**
	 * 对于具有名称空间"test-servlet"的上下文, 根上下文的默认位置是"/WEB-INF/applicationContext.groovy",
	 * 和"/WEB-INF/test-servlet.groovy"
	 * (比如一个带有servlet-name "test"的DispatcherServlet实例).
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


	// Implementation of the GroovyObject interface

	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

	public MetaClass getMetaClass() {
		return this.metaClass;
	}

	public Object invokeMethod(String name, Object args) {
		return this.metaClass.invokeMethod(this, name, args);
	}

	public void setProperty(String property, Object newValue) {
		this.metaClass.setProperty(this, property, newValue);
	}

	public Object getProperty(String property) {
		if (containsBean(property)) {
			return getBean(property);
		}
		else if (this.contextWrapper.isReadableProperty(property)) {
			return this.contextWrapper.getPropertyValue(property);
		}
		throw new NoSuchBeanDefinitionException(property);
	}

}
