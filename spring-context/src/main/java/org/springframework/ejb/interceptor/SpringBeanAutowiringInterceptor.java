package org.springframework.ejb.interceptor;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.InvocationContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;

/**
 * 符合EJB3的拦截器类, 它将Spring bean注入到使用{@code @Autowired}注解的字段和方法中.
 * 在构造后以及钝化bean激活后执行注入.
 *
 * <p>通过EJB会话Bean或消息驱动Bean类中的 {@code @Interceptors}注解,
 * 或通过EJB部署描述符中的 {@code interceptor-binding} XML元素应用.
 *
 * <p>委托给Spring的 {@link AutowiredAnnotationBeanPostProcessor}下面,
 * 允许通过覆盖 {@link #configureBeanPostProcessor}模板方法来自定义其特定设置.
 *
 * <p>从中获取Spring bean的实际BeanFactory由 {@link #getBeanFactory}模板方法决定.
 * 默认实现获取 Spring {@link ContextSingletonBeanFactoryLocator},
 * 从默认资源位置 <strong>classpath*:beanRefContext.xml</strong>初始化, 并获取在那里定义的单个ApplicationContext.
 *
 * <p><b>NOTE: 如果EJB类加载器中有多个共享的ApplicationContext定义,
 * 需要覆盖 {@link #getBeanFactoryLocatorKey} 方法并为每个自动装配的EJB提供特定的定位器Key.</b>
 * 或者, 覆盖 {@link #getBeanFactory} 模板方法并显式获取目标工厂.
 *
 * <p><b>WARNING: 不要在同一部署单元中将Spring管理的bean和EJB3会话bean定义为相同的bean.</b>
 * 特别是, 在将{@code <context:component-scan>}功能与基于Spring的EJB3会话bean的部署结合使用时要小心:
 * 使用适当的包限制, 确保EJB3会话bean也不会被自动检测为Spring管理的bean.
 */
public class SpringBeanAutowiringInterceptor {

	/*
	 * 为每个目标对象保留BeanFactoryReference, 以允许在池化目标bean上使用共享的拦截器实例.
	 * 这对于EJB3会话Bean和消息驱动Bean来说并不是绝对必要的, 其中拦截器实例是根据每个目标bean实例创建的.
	 * 它只是防止将来在共享场景中使用拦截器.
	 */
	private final Map<Object, BeanFactoryReference> beanFactoryReferences =
			new WeakHashMap<Object, BeanFactoryReference>();


	/**
	 * 在构建之后以及钝化之后自动装配目标bean.
	 * 
	 * @param invocationContext EJB3调用上下文
	 */
	@PostConstruct
	@PostActivate
	public void autowireBean(InvocationContext invocationContext) {
		doAutowireBean(invocationContext.getTarget());
		try {
			invocationContext.proceed();
		}
		catch (RuntimeException ex) {
			doReleaseBean(invocationContext.getTarget());
			throw ex;
		}
		catch (Error err) {
			doReleaseBean(invocationContext.getTarget());
			throw err;
		}
		catch (Exception ex) {
			doReleaseBean(invocationContext.getTarget());
			// 无法在WebSphere上声明受检异常 - 因此需要包装.
			throw new EJBException(ex);
		}
	}

	/**
	 * 实际上在构造/钝化之后自动装配目标bean.
	 * 
	 * @param target 要自动装配的目标bean
	 */
	protected void doAutowireBean(Object target) {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		configureBeanPostProcessor(bpp, target);
		bpp.setBeanFactory(getBeanFactory(target));
		bpp.processInjection(target);
	}

	/**
	 * 配置用于自动装配的{@link AutowiredAnnotationBeanPostProcessor}的模板方法.
	 * 
	 * @param processor 要配置的AutowiredAnnotationBeanPostProcessor
	 * @param target 使用此处理器自动装配的目标bean
	 */
	protected void configureBeanPostProcessor(AutowiredAnnotationBeanPostProcessor processor, Object target) {
	}

	/**
	 * 确定用于自动装配给定的目标bean的BeanFactory.
	 * 
	 * @param target 要自动装配的目标bean
	 * @return 要使用的BeanFactory (never {@code null})
	 */
	protected BeanFactory getBeanFactory(Object target) {
		BeanFactory factory = getBeanFactoryReference(target).getFactory();
		if (factory instanceof ApplicationContext) {
			factory = ((ApplicationContext) factory).getAutowireCapableBeanFactory();
		}
		return factory;
	}

	/**
	 * 确定给定目标bean的BeanFactoryReference.
	 * <p>默认实现委托给 {@link #getBeanFactoryLocator} 和 {@link #getBeanFactoryLocatorKey}.
	 * 
	 * @param target 要自动装配的目标bean
	 * 
	 * @return 要使用的BeanFactoryReference (never {@code null})
	 */
	protected BeanFactoryReference getBeanFactoryReference(Object target) {
		String key = getBeanFactoryLocatorKey(target);
		BeanFactoryReference ref = getBeanFactoryLocator(target).useBeanFactory(key);
		this.beanFactoryReferences.put(target, ref);
		return ref;
	}

	/**
	 * 确定要从中获取BeanFactoryReference的BeanFactoryLocator.
	 * <p>默认实现暴露了Spring的默认 {@link ContextSingletonBeanFactoryLocator}.
	 * 
	 * @param target 要自动装配的目标bean
	 * 
	 * @return 要使用的BeanFactoryLocator (never {@code null})
	 */
	protected BeanFactoryLocator getBeanFactoryLocator(Object target) {
		return ContextSingletonBeanFactoryLocator.getInstance();
	}

	/**
	 * 确定要使用的BeanFactoryLocator键.
	 * 这通常表示<strong>classpath*:beanRefContext.xml</strong>资源文件中ApplicationContext定义的bean名称.
	 * <p>默认是 {@code null}, 指示定位器中定义的单个ApplicationContext.
	 * 如果有多个共享的ApplicationContext定义可用, 则必须覆盖此项.
	 * 
	 * @param target 要自动装配的目标bean
	 * 
	 * @return 要使用的BeanFactoryLocator键 (或{@code null}用于引用定位器中定义的单个ApplicationContext)
	 */
	protected String getBeanFactoryLocatorKey(Object target) {
		return null;
	}


	/**
	 * 释放已用于自动装配目标bean的工厂.
	 * 
	 * @param invocationContext EJB3调用上下文
	 */
	@PreDestroy
	@PrePassivate
	public void releaseBean(InvocationContext invocationContext) {
		doReleaseBean(invocationContext.getTarget());
		try {
			invocationContext.proceed();
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			// 无法在WebSphere上声明受检异常 - 因此需要包装.
			throw new EJBException(ex);
		}
	}

	/**
	 * 实际释放给定目标bean的BeanFactoryReference.
	 * 
	 * @param target 要释放的目标bean
	 */
	protected void doReleaseBean(Object target) {
		BeanFactoryReference ref = this.beanFactoryReferences.remove(target);
		if (ref != null) {
			ref.release();
		}
	}

}
