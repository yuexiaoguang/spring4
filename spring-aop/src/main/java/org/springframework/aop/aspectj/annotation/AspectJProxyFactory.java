package org.springframework.aop.aspectj.annotation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.ProxyCreatorSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 基于AspectJ的代理工厂, 允许以编程方式构建包含AspectJ切面的代理 (代码风格以及Java 5注解风格).
 */
@SuppressWarnings("serial")
public class AspectJProxyFactory extends ProxyCreatorSupport {

	/** 单例切面实例的缓存 */
	private static final Map<Class<?>, Object> aspectCache = new ConcurrentHashMap<Class<?>, Object>();

	private final AspectJAdvisorFactory aspectFactory = new ReflectiveAspectJAdvisorFactory();


	public AspectJProxyFactory() {
	}

	/**
	 * <p>将代理给定目标实现的所有接口.
	 * @param target 要代理的目标对象
	 */
	public AspectJProxyFactory(Object target) {
		Assert.notNull(target, "Target object must not be null");
		setInterfaces(ClassUtils.getAllInterfaces(target));
		setTarget(target);
	}

	/**
	 * 没有目标, 只有接口. 必须添加拦截器.
	 */
	public AspectJProxyFactory(Class<?>... interfaces) {
		setInterfaces(interfaces);
	}


	/**
	 * 将提供的切面实例添加到链中. 提供的切面实例的类型必须是单例切面.
	 * 使用此方法时，不会遵循真正的单例生命周期 - 调用者负责管理以这种方式添加的切面的生命周期.
	 * 
	 * @param aspectInstance AspectJ切面实例
	 */
	public void addAspect(Object aspectInstance) {
		Class<?> aspectClass = aspectInstance.getClass();
		String aspectName = aspectClass.getName();
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
		if (am.getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON) {
			throw new IllegalArgumentException(
					"Aspect class [" + aspectClass.getName() + "] does not define a singleton aspect");
		}
		addAdvisorsFromAspectInstanceFactory(
				new SingletonMetadataAwareAspectInstanceFactory(aspectInstance, aspectName));
	}

	/**
	 * 将提供的类型的一个切面添加到增强链的末尾.
	 * 
	 * @param aspectClass AspectJ切面类
	 */
	public void addAspect(Class<?> aspectClass) {
		String aspectName = aspectClass.getName();
		AspectMetadata am = createAspectMetadata(aspectClass, aspectName);
		MetadataAwareAspectInstanceFactory instanceFactory = createAspectInstanceFactory(am, aspectClass, aspectName);
		addAdvisorsFromAspectInstanceFactory(instanceFactory);
	}


	/**
	 * 从提供的{@link MetadataAwareAspectInstanceFactory}添加所有的{@link Advisor Advisors}到当前链.
	 * 如果需要，公开{@link Advisor Advisors}用于特殊目的.
	 */
	private void addAdvisorsFromAspectInstanceFactory(MetadataAwareAspectInstanceFactory instanceFactory) {
		List<Advisor> advisors = this.aspectFactory.getAdvisors(instanceFactory);
		advisors = AopUtils.findAdvisorsThatCanApply(advisors, getTargetClass());
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(advisors);
		AnnotationAwareOrderComparator.sort(advisors);
		addAdvisors(advisors);
	}

	/**
	 * 为提供的切面类型创建{@link AspectMetadata}实例.
	 */
	private AspectMetadata createAspectMetadata(Class<?> aspectClass, String aspectName) {
		AspectMetadata am = new AspectMetadata(aspectClass, aspectName);
		if (!am.getAjType().isAspect()) {
			throw new IllegalArgumentException("Class [" + aspectClass.getName() + "] is not a valid aspect type");
		}
		return am;
	}

	/**
	 * 为提供的切面类型创建一个{@link MetadataAwareAspectInstanceFactory}.
	 * 如果切面类型没有per子句, 那么将返回一个{@link SingletonMetadataAwareAspectInstanceFactory},
	 * 否则返回一个{@link PrototypeAspectInstanceFactory}.
	 */
	private MetadataAwareAspectInstanceFactory createAspectInstanceFactory(
			AspectMetadata am, Class<?> aspectClass, String aspectName) {

		MetadataAwareAspectInstanceFactory instanceFactory;
		if (am.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
			// 创建共享切面实例.
			Object instance = getSingletonAspectInstance(aspectClass);
			instanceFactory = new SingletonMetadataAwareAspectInstanceFactory(instance, aspectName);
		}
		else {
			// 为独立的切面实例创建工厂.
			instanceFactory = new SimpleMetadataAwareAspectInstanceFactory(aspectClass, aspectName);
		}
		return instanceFactory;
	}

	/**
	 * 获取所提供的切面类型的单例切面实例. 如果在实例高速缓存中找不到实例，则创建实例.
	 */
	private Object getSingletonAspectInstance(Class<?> aspectClass) {
		// 快速检查, 不用锁...
		Object instance = aspectCache.get(aspectClass);
		if (instance == null) {
			synchronized (aspectCache) {
				// 为了安全, 使用完全锁检查...
				instance = aspectCache.get(aspectClass);
				if (instance == null) {
					try {
						instance = aspectClass.newInstance();
						aspectCache.put(aspectClass, instance);
					}
					catch (InstantiationException ex) {
						throw new AopConfigException(
								"Unable to instantiate aspect class: " + aspectClass.getName(), ex);
					}
					catch (IllegalAccessException ex) {
						throw new AopConfigException(
								"Could not access aspect constructor: " + aspectClass.getName(), ex);
					}
				}
			}
		}
		return instance;
	}


	/**
	 * 根据此工厂中的设置创建新代理.
	 * <p>可以反复调用. 如果我们添加或删除了接口，效果会有所不同. 可以添加和删除拦截器.
	 * <p>使用默认的类加载器: 通常, 线程上下文类加载器 (如果需要代理创建).
	 * 
	 * @return the new proxy
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxy() {
		return (T) createAopProxy().getProxy();
	}

	/**
	 * 根据此工厂中的设置创建新代理.
	 * <p>可以反复调用. 如果我们添加或删除了接口，效果会有所不同. 可以添加和删除拦截器.
	 * <p>使用给定的类加载器 (如果需要代理创建).
	 * 
	 * @param classLoader 用于创建代理的类加载器
	 * 
	 * @return 新代理
	 */
	@SuppressWarnings("unchecked")
	public <T> T getProxy(ClassLoader classLoader) {
		return (T) createAopProxy().getProxy(classLoader);
	}

}
