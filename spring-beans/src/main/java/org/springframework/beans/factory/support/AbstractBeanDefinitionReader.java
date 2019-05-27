package org.springframework.beans.factory.support;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * 实现{@link BeanDefinitionReader}接口的bean定义读取器的抽象基类.
 *
 * <p>提供常用属性, 例如要处理的bean工厂和用于加载bean类的类加载器.
 */
public abstract class AbstractBeanDefinitionReader implements EnvironmentCapable, BeanDefinitionReader {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final BeanDefinitionRegistry registry;

	private ResourceLoader resourceLoader;

	private ClassLoader beanClassLoader;

	private Environment environment;

	private BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();


	/**
	 * 为给定的bean工厂创建一个新的AbstractBeanDefinitionReader.
	 * <p>如果传入的bean工厂不仅实现了BeanDefinitionRegistry接口, 还实现了ResourceLoader接口, 那么它也将用作默认的ResourceLoader.
	 * 这通常是{@link org.springframework.context.ApplicationContext}实现的情况.
	 * <p>如果给定一个普通的 BeanDefinitionRegistry, 默认的ResourceLoader将会是一个
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
	 * <p>如果传入的bean工厂也实现了{@link EnvironmentCapable}, 则此读取器将使用其环境.
	 * 否则, 读取器将初始化并使用 {@link StandardEnvironment}.
	 * 所有ApplicationContext实现都是EnvironmentCapable, 而普通的BeanFactory实现则不是.
	 * 
	 * @param registry BeanFactory以BeanDefinitionRegistry的形式加载bean定义
	 */
	protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		// Determine ResourceLoader to use.
		if (this.registry instanceof ResourceLoader) {
			this.resourceLoader = (ResourceLoader) this.registry;
		}
		else {
			this.resourceLoader = new PathMatchingResourcePatternResolver();
		}

		// Inherit Environment if possible
		if (this.registry instanceof EnvironmentCapable) {
			this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
		}
		else {
			this.environment = new StandardEnvironment();
		}
	}


	public final BeanDefinitionRegistry getBeanFactory() {
		return this.registry;
	}

	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置用于资源位置的ResourceLoader.
	 * 如果指定ResourcePatternResolver, 则bean定义读取器将能够将资源模式解析为Resource数组.
	 * <p>默认值为PathMatchingResourcePatternResolver, 也可以通过ResourcePatternResolver接口解析资源模式.
	 * <p>将此值设置为{@code null}表示, 绝对资源加载不适用于此bean定义读取器.
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * 设置用于bean类的ClassLoader.
	 * <p>默认是 {@code null}, 这表明不是实时地加载bean类, 而是只是用类名注册bean定义, 相应的Classes稍后(或从不)解析.
	 */
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * 设置读取bean定义时要使用的Environment.
	 * 最常用于评估配置文件信息, 以确定应该读取哪些bean定义, 以及应该省略哪些bean定义.
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * 设置用于匿名bean的BeanNameGenerator (没有显式指定bean名称).
	 * <p>默认是 {@link DefaultBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new DefaultBeanNameGenerator());
	}

	@Override
	public BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}


	@Override
	public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
		Assert.notNull(resources, "Resource array must not be null");
		int counter = 0;
		for (Resource resource : resources) {
			counter += loadBeanDefinitions(resource);
		}
		return counter;
	}

	@Override
	public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(location, null);
	}

	/**
	 * 从指定的资源位置加载bean定义.
	 * <p>该位置也可以是位置模式, 前提是此bean定义读取器的ResourceLoader是ResourcePatternResolver.
	 * 
	 * @param location 要使用此bean定义读取器的ResourceLoader (或ResourcePatternResolver)加载的资源位置
	 * @param actualResources 在加载过程中已解析的实际Resource对象填充的Set.
	 * 可能是{@code null}, 以指示调用者对这些Resource对象不感兴趣.
	 * 
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(String location, Set<Resource> actualResources) throws BeanDefinitionStoreException {
		ResourceLoader resourceLoader = getResourceLoader();
		if (resourceLoader == null) {
			throw new BeanDefinitionStoreException(
					"Cannot import bean definitions from location [" + location + "]: no ResourceLoader available");
		}

		if (resourceLoader instanceof ResourcePatternResolver) {
			// Resource pattern matching available.
			try {
				Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
				int loadCount = loadBeanDefinitions(resources);
				if (actualResources != null) {
					for (Resource resource : resources) {
						actualResources.add(resource);
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + loadCount + " bean definitions from location pattern [" + location + "]");
				}
				return loadCount;
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException(
						"Could not resolve bean definition resource pattern [" + location + "]", ex);
			}
		}
		else {
			// Can only load single resources by absolute URL.
			Resource resource = resourceLoader.getResource(location);
			int loadCount = loadBeanDefinitions(resource);
			if (actualResources != null) {
				actualResources.add(resource);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + loadCount + " bean definitions from location [" + location + "]");
			}
			return loadCount;
		}
	}

	@Override
	public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
		Assert.notNull(locations, "Location array must not be null");
		int counter = 0;
		for (String location : locations) {
			counter += loadBeanDefinitions(location);
		}
		return counter;
	}

}
