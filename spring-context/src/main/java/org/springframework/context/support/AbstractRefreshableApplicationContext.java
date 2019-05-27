package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;

/**
 * {@link org.springframework.context.ApplicationContext}实现的基类, 应该支持多次调用 {@link #refresh()},
 * 每次都创建一个新的内部bean工厂实例.
 * 通常 (但不一定), 这样的上下文将由一组配置位置驱动以从中加载bean定义.
 *
 * <p>由子类实现的唯一方法是 {@link #loadBeanDefinitions}, 每次刷新都会调用它.
 * 一个具体的实现应该将bean定义加载到给定的 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory},
 * 通常委托给一个或多个特定的bean定义读取器.
 *
 * <p><b>请注意, WebApplicationContexts有一个类似的基类.</b>
 * {@link org.springframework.web.context.support.AbstractRefreshableWebApplicationContext} 提供相同的子类策略,
 * 但另外预先实现了Web环境的所有上下文功能.
 * 还有一种预定义的方法来接收Web上下文的配置位置.
 *
 * <p>这个基类的具体独立子类, 以特定的bean定义格式读取, 是{@link ClassPathXmlApplicationContext}
 * 和{@link FileSystemXmlApplicationContext}, 它们都来自通用的 {@link AbstractXmlApplicationContext}基类;
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}
 * 支持带{@code @Configuration}注解的类作为bean定义的来源.
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	private Boolean allowBeanDefinitionOverriding;

	private Boolean allowCircularReferences;

	/** 此上下文的Bean工厂 */
	private DefaultListableBeanFactory beanFactory;

	/** 内部BeanFactory的同步监视器 */
	private final Object beanFactoryMonitor = new Object();


	public AbstractRefreshableApplicationContext() {
	}

	/**
	 * @param parent 父级上下文
	 */
	public AbstractRefreshableApplicationContext(ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 设置是否允许通过注册具有相同名称的其他定义来覆盖bean定义, 自动替换前者.
	 * 如果不允许, 则抛出异常. 默认 "true".
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * 设置是否允许bean之间的循环引用 - 并自动尝试解析它们.
	 * <p>默认是 "true". 将其关闭以在遇到循环引用时抛出异常, 完全禁止它们.
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}


	/**
	 * 此实现执行此上下文的底层bean工厂的实际刷新, 关闭先前的bean工厂, 并初始化上下文生命周期的下一阶段的新bean工厂.
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		if (hasBeanFactory()) {
			destroyBeans();
			closeBeanFactory();
		}
		try {
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			beanFactory.setSerializationId(getId());
			customizeBeanFactory(beanFactory);
			loadBeanDefinitions(beanFactory);
			synchronized (this.beanFactoryMonitor) {
				this.beanFactory = beanFactory;
			}
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		synchronized (this.beanFactoryMonitor) {
			if (this.beanFactory != null)
				this.beanFactory.setSerializationId(null);
		}
		super.cancelRefresh(ex);
	}

	@Override
	protected final void closeBeanFactory() {
		synchronized (this.beanFactoryMonitor) {
			this.beanFactory.setSerializationId(null);
			this.beanFactory = null;
		}
	}

	/**
	 * 确定此上下文当前是否包含bean工厂, i.e. 已经刷新至少一次但尚未关闭.
	 */
	protected final boolean hasBeanFactory() {
		synchronized (this.beanFactoryMonitor) {
			return (this.beanFactory != null);
		}
	}

	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		synchronized (this.beanFactoryMonitor) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("BeanFactory not initialized or already closed - " +
						"call 'refresh' before accessing beans via the ApplicationContext");
			}
			return this.beanFactory;
		}
	}

	/**
	 * 重写将其变为无操作: 
	 * 使用AbstractRefreshableApplicationContext, {@link #getBeanFactory()} 无论如何都为活动上下文提供强大的断言.
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * 为此上下文创建内部bean工厂. 为每个 {@link #refresh()}尝试调用.
	 * <p>默认实现使用此上下文的父级的{@linkplain #getInternalParentBeanFactory() 内部bean工厂}
	 * 创建{@link org.springframework.beans.factory.support.DefaultListableBeanFactory}作为父级bean工厂.
	 * 可以在子类中重写, 例如自定义 DefaultListableBeanFactory的设置.
	 * 
	 * @return 这个上下文的bean工厂
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * 自定义此上下文使用的内部bean工厂.
	 * 为每个{@link #refresh()} 尝试调用.
	 * <p>默认实现应用此上下文的 {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
	 * 和 {@linkplain #setAllowCircularReferences "allowCircularReferences"}设置.
	 * 可以在子类中重写以自定义任何 {@link DefaultListableBeanFactory}的设置.
	 * 
	 * @param beanFactory 为此上下文创建的新bean工厂
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		if (this.allowBeanDefinitionOverriding != null) {
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.allowCircularReferences != null) {
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}

	/**
	 * 通常通过委托给一个或多个bean定义读取器, 将bean定义加载到给定的bean工厂中.
	 * 
	 * @param beanFactory 将bean定义加载到的bean工厂
	 * 
	 * @throws BeansException 如果解析bean定义失败
	 * @throws IOException 如果加载bean定义文件失败
	 */
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws BeansException, IOException;

}
