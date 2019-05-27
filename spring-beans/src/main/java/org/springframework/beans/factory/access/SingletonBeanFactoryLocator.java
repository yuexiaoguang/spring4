package org.springframework.beans.factory.access;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

/**
 * <p>{@link BeanFactoryLocator}的键控单例实现, 访问共享的Spring {@link BeanFactory}实例.</p>
 *
 * <p>请参阅BeanFactoryLocator的javadoc中, 有关单例样式BeanFactoryLocator实现的适当用法的警告.
 * Spring团队的意见是, 除了（有时）少量的粘合代码之外, 不需要使用这个类和类似的类.
 * 过度使用会导致代码更加紧密耦合, 并且更难修改或测试.</p>
 *
 * <p>在这个实现中, BeanFactory是从一个或多个XML定义文件片段构建的, 作为资源访问.
 * 默认资源名称是 'classpath*:beanRefFactory.xml', 使用Spring标准的'classpath*:'前缀,
 * 确保如果类路径包含此文件的多个副本（可能每个组件jar中有一个）, 它们将被合并
 * 覆盖默认资源名称, 不要使用无参的 {@link #getInstance()} 方法,
 * 使用{@link #getInstance(String selector)}, 将'selector' 参数作为要搜索的资源名.</p>
 *
 * <p>这个'外部'BeanFactory的目的是创建并保存一个或多个'内部'BeanFactory或ApplicationContext实例的副本,
 * 并允许直接或通过别名获得这些.
 * 因此, 此类提供对一个或多个BeanFactories/ApplicationContexts的单例样式访问, 以及间接级别,
 * 允许多个代码片段, 这些代码片段无法以依赖注入方式工作, 以不同的名称引用和使用相同的目标BeanFactory/ApplicationContext实例.<p>
 *
 * <p>考虑一个示例应用场景:
 *
 * <ul>
 * <li>{@code com.mycompany.myapp.util.applicationContext.xml} -
 * ApplicationContext定义文件, 它定义'util'层的bean.
 * <li>{@code com.mycompany.myapp.dataaccess-applicationContext.xml} -
 * ApplicationContext定义文件, 用于定义“数据访问”层的bean. 依赖上面的.
 * <li>{@code com.mycompany.myapp.services.applicationContext.xml} -
 * ApplicationContext定义文件, 定义'服务'层的bean. 依赖上面的.
 * </ul>
 *
 * <p>在一个理想的情况下, 这些将组合起来创建一个ApplicationContext,
 * 或创建为三个分层ApplicationContexts, 在应用程序启动时的某个代码处 (也许是一个Servlet过滤器),
 * 应用程序中的所有其他代码将从该上下文中作为bean获取. 但是当第三方代码进入图片时, 可能会有问题.
 * 如果第三方代码需要创建用户类, 通常应该从 Spring BeanFactory/ApplicationContext获取,
 * 但只能处理 newInstance()样式对象的创建, 然后需要一些额外的工作来实际访问和使用 BeanFactory/ApplicationContext中的对象.
 * 一种解决方案是使第三方代码创建的类只是一个stub或代理, 它从BeanFactory/ApplicationContext获取真实的对象, 并委托给它.
 * 但是, stub通常不能在每次使用时创建BeanFactory, 根据其内部的内容, 这可能是一项昂贵的操作.
 * 另外, stub与BeanFactory/ApplicationContext定义资源的名称之间存在相当紧密的耦合.
 * 这是SingletonBeanFactoryLocator的用武之地. stub 可以获得单例 SingletonBeanFactoryLocator的实例, 并要求它提供适当的BeanFactory.
 * stub或另一段代码的后续调用（假设涉及相同的类加载器）将获得相同的实例.
 * 简单的别名机制允许通过适合用户（或描述）的名称来请求上下文. 部署者可以将别名与实际的上下文名称相匹配.
 *
 * <p>SingletonBeanFactoryLocator的另一个用途是请求加载/使用一个或多个 BeanFactories/ApplicationContexts.
 * 因为该定义可以包含一个或多个 BeanFactories/ApplicationContexts, 它们可以是独立的, 也可以是层次结构,
 * 如果它们设置为延迟初始化, 只有在实际要求使用时才会创建它们.
 *
 * <p>鉴于上述三个ApplicationContexts, 考虑最简单的SingletonBeanFactoryLocator使用场景,
 * 其中只有一个{@code beanRefFactory.xml}定义文件:
 *
 * <pre class="code">&lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
 *
 * &lt;beans>
 *
 *   &lt;bean id="com.mycompany.myapp"
 *         class="org.springframework.context.support.ClassPathXmlApplicationContext">
 *     &lt;constructor-arg>
 *       &lt;list>
 *         &lt;value>com/mycompany/myapp/util/applicationContext.xml&lt;/value>
 *         &lt;value>com/mycompany/myapp/dataaccess/applicationContext.xml&lt;/value>
 *         &lt;value>com/mycompany/myapp/dataaccess/services.xml&lt;/value>
 *       &lt;/list>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 *
 * &lt;/beans>
 * </pre>
 *
 * 客户端代码非常简单:
 *
 * <pre class="code">
 * BeanFactoryLocator bfl = SingletonBeanFactoryLocator.getInstance();
 * BeanFactoryReference bf = bfl.useBeanFactory("com.mycompany.myapp");
 * // now use some bean from factory
 * MyClass zed = bf.getFactory().getBean("mybean");
 * </pre>
 *
 * {@code beanRefFactory.xml}定义文件的另一个相对简单的变体可能是:
 *
 * <pre class="code">&lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
 *
 * &lt;beans>
 *
 *   &lt;bean id="com.mycompany.myapp.util" lazy-init="true"
 *         class="org.springframework.context.support.ClassPathXmlApplicationContext">
 *     &lt;constructor-arg>
 *       &lt;value>com/mycompany/myapp/util/applicationContext.xml&lt;/value>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 *
 *   &lt;!-- child of above -->
 *   &lt;bean id="com.mycompany.myapp.dataaccess" lazy-init="true"
 *         class="org.springframework.context.support.ClassPathXmlApplicationContext">
 *     &lt;constructor-arg>
 *       &lt;list>&lt;value>com/mycompany/myapp/dataaccess/applicationContext.xml&lt;/value>&lt;/list>
 *     &lt;/constructor-arg>
 *     &lt;constructor-arg>
 *       &lt;ref bean="com.mycompany.myapp.util"/>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 *
 *   &lt;!-- child of above -->
 *   &lt;bean id="com.mycompany.myapp.services" lazy-init="true"
 *         class="org.springframework.context.support.ClassPathXmlApplicationContext">
 *     &lt;constructor-arg>
 *       &lt;list>&lt;value>com/mycompany/myapp/dataaccess.services.xml&lt;/value>&lt;/value>
 *     &lt;/constructor-arg>
 *     &lt;constructor-arg>
 *       &lt;ref bean="com.mycompany.myapp.dataaccess"/>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 *
 *   &lt;!-- define an alias -->
 *   &lt;bean id="com.mycompany.myapp.mypackage"
 *         class="java.lang.String">
 *     &lt;constructor-arg>
 *       &lt;value>com.mycompany.myapp.services&lt;/value>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 *
 * &lt;/beans>
 * </pre>
 *
 * <p>在这个例子中, 创建了三个上下文的层次结构.
 * （潜在）优点是如果延迟标志设置为true, 只有在实际使用的情况下才会创建上下文.
 * 如果有一些代码只在某些时候需要, 这种机制可以节省一些资源.
 * 另外, 已创建最后一个上下文的别名.
 * 别名允许使用习语, 客户端代码请求具有id的上下文, 该id表示代码所在的包或模块,
 * 并且SingletonBeanFactoryLocator的实际定义文件将该id映射到实际上下文id.
 *
 * <p>最后一个例子更复杂, 每个模块都有一个{@code beanRefFactory.xml}.
 * 所有文件都自动组合以创建最终定义.
 *
 * <p>用于util模块的jar内的{@code beanRefFactory.xml}文件:
 *
 * <pre class="code">&lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
 *
 * &lt;beans>
 *   &lt;bean id="com.mycompany.myapp.util" lazy-init="true"
 *        class="org.springframework.context.support.ClassPathXmlApplicationContext">
 *     &lt;constructor-arg>
 *       &lt;value>com/mycompany/myapp/util/applicationContext.xml&lt;/value>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 * &lt;/beans>
 * </pre>
 *
 * 用于数据访问模块的jar内的{@code beanRefFactory.xml}文件:<br>
 *
 * <pre class="code">&lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
 *
 * &lt;beans>
 *   &lt;!-- child of util -->
 *   &lt;bean id="com.mycompany.myapp.dataaccess" lazy-init="true"
 *        class="org.springframework.context.support.ClassPathXmlApplicationContext">
 *     &lt;constructor-arg>
 *       &lt;list>&lt;value>com/mycompany/myapp/dataaccess/applicationContext.xml&lt;/value>&lt;/list>
 *     &lt;/constructor-arg>
 *     &lt;constructor-arg>
 *       &lt;ref bean="com.mycompany.myapp.util"/>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 * &lt;/beans>
 * </pre>
 *
 * 用于服务模块的jar内的{@code beanRefFactory.xml}文件:
 *
 * <pre class="code">&lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
 *
 * &lt;beans>
 *   &lt;!-- child of data-access -->
 *   &lt;bean id="com.mycompany.myapp.services" lazy-init="true"
 *        class="org.springframework.context.support.ClassPathXmlApplicationContext">
 *     &lt;constructor-arg>
 *       &lt;list>&lt;value>com/mycompany/myapp/dataaccess/services.xml&lt;/value>&lt;/list>
 *     &lt;/constructor-arg>
 *     &lt;constructor-arg>
 *       &lt;ref bean="com.mycompany.myapp.dataaccess"/>
 *     &lt;/constructor-arg>
 *   &lt;/bean>
 * &lt;/beans>
 * </pre>
 *
 * mypackage模块的jar内的{@code beanRefFactory.xml}文件.
 * 这不会创建任何自己的上下文, 但允许此模块通过已知的名称引用其它的模块:
 *
 * <pre class="code">&lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
 *
 * &lt;beans>
 *   &lt;!-- define an alias for "com.mycompany.myapp.services" -->
 *   &lt;alias name="com.mycompany.myapp.services" alias="com.mycompany.myapp.mypackage"/&gt;
 * &lt;/beans>
 * </pre>
 */
public class SingletonBeanFactoryLocator implements BeanFactoryLocator {

	private static final String DEFAULT_RESOURCE_LOCATION = "classpath*:beanRefFactory.xml";

	protected static final Log logger = LogFactory.getLog(SingletonBeanFactoryLocator.class);

	/** The keyed BeanFactory instances */
	private static final Map<String, BeanFactoryLocator> instances = new HashMap<String, BeanFactoryLocator>();


	/**
	 * 返回使用默认的 "classpath*:beanRefFactory.xml"的实例, 作为定义文件的名称.
	 * 通过使用此名称调用当前线程上下文ClassLoader的{@code getResources}方法返回的所有资源,
	 * 将组合在一起以创建BeanFactory定义集.
	 * 
	 * @return 相应的BeanFactoryLocator实例
	 * @throws BeansException 在工厂加载失败的情况下
	 */
	public static BeanFactoryLocator getInstance() throws BeansException {
		return getInstance(null);
	}

	/**
	 * 返回使用指定选择器的实例, 作为定义文件的名称.
	 * 如果名称带有 Spring 'classpath*:'前缀, 或者没有前缀, 都是一样的,
	 * 将使用此值调用当前线程上下文ClassLoader的{@code getResources}方法以获取具有该名称的所有资源.
	 * 然后将这些资源组合起来形成定义.
	 * 在名称使用Spring 'classpath:'前缀或标准URL前缀的情况下, 然后只会加载一个资源文件作为定义.
	 * 
	 * @param selector 将读取和组合以形成BeanFactoryLocator实例的定义的资源的名称.
	 * 任何此类文件都必须形成有效的BeanFactory定义.
	 * 
	 * @return 相应的BeanFactoryLocator实例
	 * @throws BeansException 在工厂加载失败的情况下
	 */
	public static BeanFactoryLocator getInstance(String selector) throws BeansException {
		String resourceLocation = selector;
		if (resourceLocation == null) {
			resourceLocation = DEFAULT_RESOURCE_LOCATION;
		}

		// 为了向后兼容, 如果没有其他前缀, 将'classpath*:'添加到选择器名称 (i.e. classpath*:, classpath:, or some URL prefix.
		if (!ResourcePatternUtils.isUrl(resourceLocation)) {
			resourceLocation = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourceLocation;
		}

		synchronized (instances) {
			if (logger.isTraceEnabled()) {
				logger.trace("SingletonBeanFactoryLocator.getInstance(): instances.hashCode=" +
						instances.hashCode() + ", instances=" + instances);
			}
			BeanFactoryLocator bfl = instances.get(resourceLocation);
			if (bfl == null) {
				bfl = new SingletonBeanFactoryLocator(resourceLocation);
				instances.put(resourceLocation, bfl);
			}
			return bfl;
		}
	}


	// We map BeanFactoryGroup objects by String keys, and by the definition object.
	private final Map<String, BeanFactoryGroup> bfgInstancesByKey = new HashMap<String, BeanFactoryGroup>();

	private final Map<BeanFactory, BeanFactoryGroup> bfgInstancesByObj = new HashMap<BeanFactory, BeanFactoryGroup>();

	private final String resourceLocation;


	/**
	 * 使用指定的名称作为定义文件的资源名称的构造函数.
	 * 
	 * @param resourceLocation 要使用的Spring资源位置 (URL 或"classpath:" / "classpath*:" pseudo URL)
	 */
	protected SingletonBeanFactoryLocator(String resourceLocation) {
		this.resourceLocation = resourceLocation;
	}

	@Override
	public BeanFactoryReference useBeanFactory(String factoryKey) throws BeansException {
		synchronized (this.bfgInstancesByKey) {
			BeanFactoryGroup bfg = this.bfgInstancesByKey.get(this.resourceLocation);

			if (bfg != null) {
				bfg.refCount++;
			}
			else {
				// This group definition doesn't exist, we need to try to load it.
				if (logger.isTraceEnabled()) {
					logger.trace("Factory group with resource name [" + this.resourceLocation +
							"] requested. Creating new instance.");
				}

				// Create the BeanFactory but don't initialize it.
				BeanFactory groupContext = createDefinition(this.resourceLocation, factoryKey);

				// Record its existence now, before instantiating any singletons.
				bfg = new BeanFactoryGroup();
				bfg.definition = groupContext;
				bfg.refCount = 1;
				this.bfgInstancesByKey.put(this.resourceLocation, bfg);
				this.bfgInstancesByObj.put(groupContext, bfg);

				// Now initialize the BeanFactory.
				// 这可能会导致重新调用此方法, 但是因为我们已经将BeanFactory添加到了映射中, 下次将找到它并简单地增加其引用计数.
				try {
					initializeDefinition(groupContext);
				}
				catch (BeansException ex) {
					this.bfgInstancesByKey.remove(this.resourceLocation);
					this.bfgInstancesByObj.remove(groupContext);
					throw new BootstrapException("Unable to initialize group definition. " +
							"Group resource name [" + this.resourceLocation + "], factory key [" + factoryKey + "]", ex);
				}
			}

			try {
				BeanFactory beanFactory;
				if (factoryKey != null) {
					beanFactory = bfg.definition.getBean(factoryKey, BeanFactory.class);
				}
				else {
					beanFactory = bfg.definition.getBean(BeanFactory.class);
				}
				return new CountingBeanFactoryReference(beanFactory, bfg.definition);
			}
			catch (BeansException ex) {
				throw new BootstrapException("Unable to return specified BeanFactory instance: factory key [" +
						factoryKey + "], from group with resource name [" + this.resourceLocation + "]", ex);
			}

		}
	}

	/**
	 * 实际上以BeanFactory的形式创建定义, 给定一个支持标准Spring资源前缀的资源名称 ('classpath:', 'classpath*:', etc.)
	 * 这被拆分为一个单独的方法, 以便子类可以覆盖使用的实际类型 (例如 ApplicationContext).
	 * <p>默认实现简单构建一个 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory},
	 * 并使用一个 {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}填充它.
	 * <p>此方法不应实例化任何单例.
	 * 该函数由 {@link #initializeDefinition initializeDefinition()}执行, 如果这个方法被重写, 也应该被重写.
	 * 
	 * @param resourceLocation 此工厂组的资源位置
	 * @param factoryKey 要获得的工厂的bean名称
	 * 
	 * @return 相应的BeanFactory引用
	 */
	protected BeanFactory createDefinition(String resourceLocation, String factoryKey) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

		try {
			Resource[] configResources = resourcePatternResolver.getResources(resourceLocation);
			if (configResources.length == 0) {
				throw new FatalBeanException("Unable to find resource for specified definition. " +
						"Group resource name [" + this.resourceLocation + "], factory key [" + factoryKey + "]");
			}
			reader.loadBeanDefinitions(configResources);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Error accessing bean definition resource [" + this.resourceLocation + "]", ex);
		}
		catch (BeanDefinitionStoreException ex) {
			throw new FatalBeanException("Unable to load group definition: " +
					"group resource name [" + this.resourceLocation + "], factory key [" + factoryKey + "]", ex);
		}

		return factory;
	}

	/**
	 * 实例化单例并进行工厂的其他正常初始化.
	 * 重写{@link #createDefinition createDefinition()}的子类也应该重写这个方法.
	 * 
	 * @param groupDef {@link #createDefinition createDefinition()}返回的工厂
	 */
	protected void initializeDefinition(BeanFactory groupDef) {
		if (groupDef instanceof ConfigurableListableBeanFactory) {
			((ConfigurableListableBeanFactory) groupDef).preInstantiateSingletons();
		}
	}

	/**
	 * 在单独的方法中销毁定义, 因此子类可以与其他定义类型一起使用.
	 * 
	 * @param groupDef {@link #createDefinition createDefinition()}返回的工厂
	 * @param selector 此工厂组的资源位置
	 */
	protected void destroyDefinition(BeanFactory groupDef, String selector) {
		if (groupDef instanceof ConfigurableBeanFactory) {
			if (logger.isTraceEnabled()) {
				logger.trace("Factory group with selector '" + selector +
						"' being released, as there are no more references to it");
			}
			((ConfigurableBeanFactory) groupDef).destroySingletons();
		}
	}


	/**
	 * 使用此类跟踪BeanFactory实例.
	 */
	private static class BeanFactoryGroup {

		private BeanFactory definition;

		private int refCount = 0;
	}


	/**
	 * 此定位器的BeanFactoryReference实现.
	 */
	private class CountingBeanFactoryReference implements BeanFactoryReference {

		private BeanFactory beanFactory;

		private BeanFactory groupContextRef;

		public CountingBeanFactoryReference(BeanFactory beanFactory, BeanFactory groupContext) {
			this.beanFactory = beanFactory;
			this.groupContextRef = groupContext;
		}

		@Override
		public BeanFactory getFactory() {
			return this.beanFactory;
		}

		// 请注意, 多次调用release是合法的!
		@Override
		public void release() throws FatalBeanException {
			synchronized (bfgInstancesByKey) {
				BeanFactory savedRef = this.groupContextRef;
				if (savedRef != null) {
					this.groupContextRef = null;
					BeanFactoryGroup bfg = bfgInstancesByObj.get(savedRef);
					if (bfg != null) {
						bfg.refCount--;
						if (bfg.refCount == 0) {
							destroyDefinition(savedRef, resourceLocation);
							bfgInstancesByKey.remove(resourceLocation);
							bfgInstancesByObj.remove(savedRef);
						}
					}
					else {
						// This should be impossible.
						logger.warn("Tried to release a SingletonBeanFactoryLocator group definition " +
								"more times than it has actually been used. Resource name [" + resourceLocation + "]");
					}
				}
			}
		}
	}
}
