package org.springframework.beans.factory.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;

/**
 * 需要处理{@link org.springframework.beans.factory.FactoryBean}实例的单例注册表的支持基类,
 * 与{@link DefaultSingletonBeanRegistry}的单例管理集成.
 *
 * <p>作为{@link AbstractBeanFactory}的基类.
 */
public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

	/** FactoryBeans创建的单例对象的缓存: FactoryBean name --> object */
	private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<String, Object>(16);


	/**
	 * 确定给定FactoryBean的类型.
	 * 
	 * @param factoryBean 要检查的FactoryBean实例
	 * 
	 * @return FactoryBean的对象类型, 或{@code null}如果尚未确定类型
	 */
	protected Class<?> getTypeForFactoryBean(final FactoryBean<?> factoryBean) {
		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
					@Override
					public Class<?> run() {
						return factoryBean.getObjectType();
					}
				}, getAccessControlContext());
			}
			else {
				return factoryBean.getObjectType();
			}
		}
		catch (Throwable ex) {
			// Thrown from the FactoryBean's getObjectType implementation.
			logger.warn("FactoryBean threw exception from getObjectType, despite the contract saying " +
					"that it should return null if the type of its object cannot be determined yet", ex);
			return null;
		}
	}

	/**
	 * 获取要从给定FactoryBean公开的对象, 如果在缓存形式中可用. 以最小的同步快速检查.
	 * 
	 * @param beanName bean的名称
	 * 
	 * @return 从FactoryBean获取的对象, 或{@code null}
	 */
	protected Object getCachedObjectForFactoryBean(String beanName) {
		Object object = this.factoryBeanObjectCache.get(beanName);
		return (object != NULL_OBJECT ? object : null);
	}

	/**
	 * 获取要从给定FactoryBean公开的对象.
	 * 
	 * @param factory FactoryBean实例
	 * @param beanName bean的名称
	 * @param shouldPostProcess bean是否需要进行后处理
	 * 
	 * @return 从FactoryBean获取的对象
	 * @throws BeanCreationException 如果FactoryBean对象创建失败
	 */
	protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
		if (factory.isSingleton() && containsSingleton(beanName)) {
			synchronized (getSingletonMutex()) {
				Object object = this.factoryBeanObjectCache.get(beanName);
				if (object == null) {
					object = doGetObjectFromFactoryBean(factory, beanName);
					// 只进行后处理和存储, 如果在上面的 getObject() 调用期间没有放在那里
					// (e.g. 因为自定义getBean调用触发了循环引用处理)
					Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
					if (alreadyThere != null) {
						object = alreadyThere;
					}
					else {
						if (object != null && shouldPostProcess) {
							if (isSingletonCurrentlyInCreation(beanName)) {
								// 暂时返回非后处理对象, 而不是存储它..
								return object;
							}
							beforeSingletonCreation(beanName);
							try {
								object = postProcessObjectFromFactoryBean(object, beanName);
							}
							catch (Throwable ex) {
								throw new BeanCreationException(beanName,
										"Post-processing of FactoryBean's singleton object failed", ex);
							}
							finally {
								afterSingletonCreation(beanName);
							}
						}
						if (containsSingleton(beanName)) {
							this.factoryBeanObjectCache.put(beanName, (object != null ? object : NULL_OBJECT));
						}
					}
				}
				return (object != NULL_OBJECT ? object : null);
			}
		}
		else {
			Object object = doGetObjectFromFactoryBean(factory, beanName);
			if (object != null && shouldPostProcess) {
				try {
					object = postProcessObjectFromFactoryBean(object, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
				}
			}
			return object;
		}
	}

	/**
	 * 获取要从给定FactoryBean公开的对象.
	 * 
	 * @param factory FactoryBean实例
	 * @param beanName bean的名称
	 * 
	 * @return 从FactoryBean获取的对象
	 * @throws BeanCreationException 如果FactoryBean对象创建失败
	 */
	private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
			throws BeanCreationException {

		Object object;
		try {
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
								return factory.getObject();
							}
						}, acc);
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
				object = factory.getObject();
			}
		}
		catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		// 不接受null值, 因为 FactoryBean 尚未完全初始化: Many FactoryBeans just return null then.
		if (object == null && isSingletonCurrentlyInCreation(beanName)) {
			throw new BeanCurrentlyInCreationException(
					beanName, "FactoryBean which is currently in creation returned null from getObject");
		}
		return object;
	}

	/**
	 * 对从FactoryBean获取的给定对象进行后处理.
	 * 生成的对象将暴露给bean引用.
	 * <p>默认实现只是按原样返回给定的对象.
	 * 子类可以覆盖它,例如, 应用后处理器.
	 * 
	 * @param object 从FactoryBean获取的对象.
	 * @param beanName bean的名称
	 * 
	 * @return 要公开的对象
	 * @throws org.springframework.beans.BeansException 如果任何后处理失败
	 */
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) throws BeansException {
		return object;
	}

	/**
	 * 如果可能, 获取给定bean的FactoryBean.
	 * 
	 * @param beanName bean的名称
	 * @param beanInstance 相应的bean实例
	 * 
	 * @return bean实例
	 * @throws BeansException 如果给定的bean不能作为FactoryBean暴露
	 */
	protected FactoryBean<?> getFactoryBean(String beanName, Object beanInstance) throws BeansException {
		if (!(beanInstance instanceof FactoryBean)) {
			throw new BeanCreationException(beanName,
					"Bean instance of type [" + beanInstance.getClass() + "] is not a FactoryBean");
		}
		return (FactoryBean<?>) beanInstance;
	}

	/**
	 * 重写以清除FactoryBean对象缓存.
	 */
	@Override
	protected void removeSingleton(String beanName) {
		synchronized (getSingletonMutex()) {
			super.removeSingleton(beanName);
			this.factoryBeanObjectCache.remove(beanName);
		}
	}

	/**
	 * 重写以清除FactoryBean对象缓存.
	 */
	@Override
	protected void clearSingletonCache() {
		synchronized (getSingletonMutex()) {
			super.clearSingletonCache();
			this.factoryBeanObjectCache.clear();
		}
	}

	/**
	 * 返回此Bean工厂的安全上下文.
	 * 如果设置了安全管理器, 则将使用此方法返回的安全上下文的特权, 执行与用户代码的交互.
	 */
	protected AccessControlContext getAccessControlContext() {
		return AccessController.getContext();
	}

}
