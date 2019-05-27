package org.springframework.context.access;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

/**
 * <p>{@link org.springframework.beans.factory.access.SingletonBeanFactoryLocator}的变体,
 * 它将内部bean工厂引用创建为{@link org.springframework.context.ApplicationContext},
 * 而不是SingletonBeanFactoryLocator的简单BeanFactory.
 * 对于几乎所有的使用场景, 这都没有区别, 因为在ApplicationContext或BeanFactory中,
 * 你仍然可以自由定义BeanFactory或ApplicationContext实例.
 * 需要使用此类的主要原因是bean后处理
 * (或者bean引用定义本身需要其他ApplicationContext特定功能).
 *
 * <p><strong>Note:</strong> 此类使用<strong>classpath*:beanRefContext.xml</strong>作为Bean工厂引用定义文件的默认资源位置.
 * 同时与SingletonBeanFactoryLocator共享定义是不可能, 也不合法的.
 */
public class ContextSingletonBeanFactoryLocator extends SingletonBeanFactoryLocator {

	private static final String DEFAULT_RESOURCE_LOCATION = "classpath*:beanRefContext.xml";

	/** 单例 */
	private static final Map<String, BeanFactoryLocator> instances = new HashMap<String, BeanFactoryLocator>();


	/**
	 * 返回使用默认 "classpath*:beanRefContext.xml"的实例, 作为定义文件的名称.
	 * 当前线程的上下文类加载器的 {@code getResources}方法返回的所有资源将被合并以创建一个定义, 它只是一个BeanFactory.
	 * 
	 * @return 相应的BeanFactoryLocator实例
	 * @throws BeansException 在工厂加载失败的情况下
	 */
	public static BeanFactoryLocator getInstance() throws BeansException {
		return getInstance(null);
	}

	/**
	 * 返回使用指定选择器的实例, 作为定义文件的名称.
	 * 如果名称带有Spring "classpath*:"前缀, 或者没有前缀, 则视为相同,
	 * 将使用此值调用当前线程的上下文类加载器的 {@code getResources}方法, 以获取具有该名称的所有资源.
	 * 然后将这些资源组合起来形成定义.
	 * 如果名称使用Spring "classpath:"前缀, 或标准URL前缀, 则只会加载一个资源文件作为定义.
	 * 
	 * @param selector 将读取和组合的资源的位置, 以形成BeanFactoryLocator实例的定义.
	 * 任何此类文件都必须形成有效的ApplicationContext定义.
	 * 
	 * @return 相应的BeanFactoryLocator实例
	 * @throws BeansException 在工厂加载失败的情况下
	 */
	public static BeanFactoryLocator getInstance(String selector) throws BeansException {
		String resourceLocation = selector;
		if (resourceLocation == null) {
			resourceLocation = DEFAULT_RESOURCE_LOCATION;
		}

		// 为了向后兼容, 如果没有其他前缀, 将"classpath*:"添加到选择器名称 (i.e. "classpath*:", "classpath:", 或一些URL前缀).
		if (!ResourcePatternUtils.isUrl(resourceLocation)) {
			resourceLocation = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourceLocation;
		}

		synchronized (instances) {
			if (logger.isTraceEnabled()) {
				logger.trace("ContextSingletonBeanFactoryLocator.getInstance(): instances.hashCode=" +
						instances.hashCode() + ", instances=" + instances);
			}
			BeanFactoryLocator bfl = instances.get(resourceLocation);
			if (bfl == null) {
				bfl = new ContextSingletonBeanFactoryLocator(resourceLocation);
				instances.put(resourceLocation, bfl);
			}
			return bfl;
		}
	}


	/**
	 * 使用指定名称作为定义文件的资源名称的构造方法.
	 * 
	 * @param resourceLocation 要使用的Spring资源位置
	 * (URL 或 "classpath:" / "classpath*:" 伪URL)
	 */
	protected ContextSingletonBeanFactoryLocator(String resourceLocation) {
		super(resourceLocation);
	}

	/**
	 * 覆盖默认方法, 以将定义对象创建为ApplicationContext, 而不是默认BeanFactory.
	 * 这不会影响该定义实际可以加载的内容.
	 * <p>默认实现只是构建一个 {@link org.springframework.context.support.ClassPathXmlApplicationContext}.
	 */
	@Override
	protected BeanFactory createDefinition(String resourceLocation, String factoryKey) {
		return new ClassPathXmlApplicationContext(new String[] {resourceLocation}, false);
	}

	/**
	 * 覆盖默认方法以刷新ApplicationContext, 调用
	 * {@link ConfigurableApplicationContext#refresh ConfigurableApplicationContext.refresh()}.
	 */
	@Override
	protected void initializeDefinition(BeanFactory groupDef) {
		if (groupDef instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) groupDef).refresh();
		}
	}

	/**
	 * 覆盖默认方法以对ApplicationContext进行操作, 调用
	 * {@link ConfigurableApplicationContext#refresh ConfigurableApplicationContext.close()}.
	 */
	@Override
	protected void destroyDefinition(BeanFactory groupDef, String selector) {
		if (groupDef instanceof ConfigurableApplicationContext) {
			if (logger.isTraceEnabled()) {
				logger.trace("Context group with selector '" + selector +
						"' being released, as there are no more references to it");
			}
			((ConfigurableApplicationContext) groupDef).close();
		}
	}

}
