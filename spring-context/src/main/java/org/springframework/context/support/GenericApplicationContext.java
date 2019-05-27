package org.springframework.context.support;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * 保存单个内部{@link org.springframework.beans.factory.support.DefaultListableBeanFactory}实例的通用ApplicationContext实现,
 * 并不假设特定的bean定义格式.
 * 实现{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}接口,
 * 以允许将任何bean定义读取器应用于它.
 *
 * <p>典型用法是通过{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}接口注册各种bean定义,
 * 然后调用 {@link #refresh()} 来初始化具有应用程序上下文语义的bean
 * (处理{@link org.springframework.context.ApplicationContextAware},
 * 自动检测 {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors}, etc).
 *
 * <p>与为每次刷新创建新的内部BeanFactory实例的其他ApplicationContext实现相反,
 * 此上下文的内部BeanFactory从一开始就可用, 以便能够在其上注册bean定义. {@link #refresh()} 只能调用一次.
 *
 * <p>用例:
 *
 * <pre class="code">
 * GenericApplicationContext ctx = new GenericApplicationContext();
 * XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
 * xmlReader.loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
 * PropertiesBeanDefinitionReader propReader = new PropertiesBeanDefinitionReader(ctx);
 * propReader.loadBeanDefinitions(new ClassPathResource("otherBeans.properties"));
 * ctx.refresh();
 *
 * MyBean myBean = (MyBean) ctx.getBean("myBean");
 * ...</pre>
 *
 * 对于XML bean定义的典型情况, 简单实用
 * {@link ClassPathXmlApplicationContext} 或 {@link FileSystemXmlApplicationContext},
 * 更容易设置 - 但不太灵活, 因为只可以使用XML bean定义的标准资源位置, 而不是混合的任意bean定义格式.
 * Web环境中的等效项是 {@link org.springframework.web.context.support.XmlWebApplicationContext}.
 *
 * <p>对于以可刷新的方式读取特殊bean定义格式的自定义应用程序上下文实现, 考虑从{@link AbstractRefreshableApplicationContext}基类派生.
 */
public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

	private final DefaultListableBeanFactory beanFactory;

	private ResourceLoader resourceLoader;

	private boolean customClassLoader = false;

	private final AtomicBoolean refreshed = new AtomicBoolean();


	public GenericApplicationContext() {
		this.beanFactory = new DefaultListableBeanFactory();
	}

	/**
	 * @param beanFactory 用于此上下文的DefaultListableBeanFactory实例
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * @param parent 父级应用程序上下文
	 */
	public GenericApplicationContext(ApplicationContext parent) {
		this();
		setParent(parent);
	}

	/**
	 * @param beanFactory 用于此上下文的DefaultListableBeanFactory实例
	 * @param parent 父级应用程序上下文
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory, ApplicationContext parent) {
		this(beanFactory);
		setParent(parent);
	}


	/**
	 * 设置此应用程序上下文的父级, 还相应地设置内部BeanFactory的父级.
	 */
	@Override
	public void setParent(ApplicationContext parent) {
		super.setParent(parent);
		this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * 设置是否允许通过注册具有相同名称的其他定义, 来覆盖bean定义, 自动替换前者.
	 * 如果不允许, 将抛出异常. 默认是 "true".
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.beanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
	}

	/**
	 * 设置是否允许bean之间的循环引用 - 并自动尝试解析它们.
	 * <p>默认是 "true". 将其关闭以在遇到循环引用时抛出异常, 完全禁止它们.
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.beanFactory.setAllowCircularReferences(allowCircularReferences);
	}

	/**
	 * 设置用于此上下文的ResourceLoader.
	 * 如果设置了, 则上下文会将所有的{@code getResource}调用委托给给定的ResourceLoader.
	 * 如果未设置, 则将应用默认资源加载.
	 * <p>指定自定义ResourceLoader的主要原因是以特定方式解析资源路径 (没有URL前缀).
	 * 默认行为是解析类路径位置等路径.
	 * 要将资源路径解析为文件系统位置, 请在此处指定FileSystemResourceLoader.
	 * <p>还可以传入一个完整的ResourcePatternResolver, 它将被上下文自动检测并用于{@code getResources}调用.
	 * 否则, 将应用默认资源模式匹配.
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	//---------------------------------------------------------------------
	// ResourceLoader / ResourcePatternResolver override if necessary
	//---------------------------------------------------------------------

	/**
	 * 如果设置, 则此实现委托给此上下文的ResourceLoader; 否则回退到默认的超类行为.
	 */
	@Override
	public Resource getResource(String location) {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getResource(location);
		}
		return super.getResource(location);
	}

	/**
	 * 如果它实现了ResourcePatternResolver接口, 则该实现委托给该上下文的ResourceLoader; 否则回退到默认的超类行为.
	 */
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		if (this.resourceLoader instanceof ResourcePatternResolver) {
			return ((ResourcePatternResolver) this.resourceLoader).getResources(locationPattern);
		}
		return super.getResources(locationPattern);
	}

	@Override
	public void setClassLoader(ClassLoader classLoader) {
		super.setClassLoader(classLoader);
		this.customClassLoader = true;
	}

	@Override
	public ClassLoader getClassLoader() {
		if (this.resourceLoader != null && !this.customClassLoader) {
			return this.resourceLoader.getClassLoader();
		}
		return super.getClassLoader();
	}


	//---------------------------------------------------------------------
	// Implementations of AbstractApplicationContext's template methods
	//---------------------------------------------------------------------

	/**
	 * 什么都不做: 持有单个内部BeanFactory, 并依赖调用者通过public方法(或BeanFactory的)注册bean.
	 */
	@Override
	protected final void refreshBeanFactory() throws IllegalStateException {
		if (!this.refreshed.compareAndSet(false, true)) {
			throw new IllegalStateException(
					"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
		}
		this.beanFactory.setSerializationId(getId());
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		this.beanFactory.setSerializationId(null);
		super.cancelRefresh(ex);
	}

	/**
	 * 没什么可做的: 持有一个永远不会被释放的内部BeanFactory.
	 */
	@Override
	protected final void closeBeanFactory() {
		this.beanFactory.setSerializationId(null);
	}

	/**
	 * 返回此上下文持有的单个内部BeanFactory (as ConfigurableListableBeanFactory).
	 */
	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 返回此上下文的底层bean工厂, 可用于注册bean定义.
	 * <p><b>NOTE:</b> 需要调用 {@link #refresh()} 来初始化bean工厂及其包含的应用程序上下文语义bean
	 * (自动检测 BeanFactoryPostProcessors, etc).
	 * 
	 * @return 内部 bean 工厂 (as DefaultListableBeanFactory)
	 */
	public final DefaultListableBeanFactory getDefaultListableBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		assertBeanFactoryActive();
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		this.beanFactory.removeBeanDefinition(beanName);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return this.beanFactory.getBeanDefinition(beanName);
	}

	@Override
	public boolean isBeanNameInUse(String beanName) {
		return this.beanFactory.isBeanNameInUse(beanName);
	}

	@Override
	public void registerAlias(String beanName, String alias) {
		this.beanFactory.registerAlias(beanName, alias);
	}

	@Override
	public void removeAlias(String alias) {
		this.beanFactory.removeAlias(alias);
	}

	@Override
	public boolean isAlias(String beanName) {
		return this.beanFactory.isAlias(beanName);
	}

}
