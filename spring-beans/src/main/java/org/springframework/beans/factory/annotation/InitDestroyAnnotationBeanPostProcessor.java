package org.springframework.beans.factory.annotation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现, 调用被注解的初始化和销毁方法.
 * 允许使用Spring的 {@link org.springframework.beans.factory.InitializingBean}
 * 和{@link org.springframework.beans.factory.DisposableBean}回调接口的注解替代方法.
 *
 * <p>可以通过{@link #setInitAnnotationType "initAnnotationType"}和{@link #setDestroyAnnotationType "destroyAnnotationType"}属性,
 * 配置此后处理器检查的实际注解类型.
 * 可以使用任何自定义注解, 因为没有必需的注解属性.
 *
 * <p>初始化和销毁​​注解可以应用于任何可见性的方法:
 * public, package-protected, protected, or private.
 * 可以注解多个这样的方法, 但建议分别只注解一个单一的init方法和destroy方法.
 *
 * <p>Spring的 {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}
 * 支持 JSR-250 {@link javax.annotation.PostConstruct} 和 {@link javax.annotation.PreDestroy}注解作为初始化和销毁注解.
 * 此外, 它还支持注解驱动的命名bean注入的{@link javax.annotation.Resource}注解.
 */
@SuppressWarnings("serial")
public class InitDestroyAnnotationBeanPostProcessor
		implements DestructionAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor, PriorityOrdered, Serializable {

	protected transient Log logger = LogFactory.getLog(getClass());

	private Class<? extends Annotation> initAnnotationType;

	private Class<? extends Annotation> destroyAnnotationType;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private transient final Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache =
			new ConcurrentHashMap<Class<?>, LifecycleMetadata>(256);


	/**
	 * 指定要检查的init注解, 指示在配置bean之后调用的初始化方法.
	 * <p>可以使用任何自定义注解, 因为没有必需的注解属性.
	 * 没有默认值, 虽然典型的选择是JSR-250 {@link javax.annotation.PostConstruct}注解.
	 */
	public void setInitAnnotationType(Class<? extends Annotation> initAnnotationType) {
		this.initAnnotationType = initAnnotationType;
	}

	/**
	 * 指定要检查的destroy注解, 指示在关闭上下文时要调用的销毁方法.
	 * <p>可以使用任何自定义注解, 因为没有必需的注解属性.
	 * 没有默认值, 虽然典型的选择是JSR-250 {@link javax.annotation.PreDestroy}注解.
	 */
	public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
		this.destroyAnnotationType = destroyAnnotationType;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (beanType != null) {
			LifecycleMetadata metadata = findLifecycleMetadata(beanType);
			metadata.checkConfigMembers(beanDefinition);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
		try {
			metadata.invokeInitMethods(bean, beanName);
		}
		catch (InvocationTargetException ex) {
			throw new BeanCreationException(beanName, "Invocation of init method failed", ex.getTargetException());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Failed to invoke init method", ex);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
		try {
			metadata.invokeDestroyMethods(bean, beanName);
		}
		catch (InvocationTargetException ex) {
			String msg = "Invocation of destroy method failed on bean with name '" + beanName + "'";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex.getTargetException());
			}
			else {
				logger.warn(msg + ": " + ex.getTargetException());
			}
		}
		catch (Throwable ex) {
			logger.error("Failed to invoke destroy method on bean with name '" + beanName + "'", ex);
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return findLifecycleMetadata(bean.getClass()).hasDestroyMethods();
	}


	private LifecycleMetadata findLifecycleMetadata(Class<?> clazz) {
		if (this.lifecycleMetadataCache == null) {
			// Happens after deserialization, during destruction...
			return buildLifecycleMetadata(clazz);
		}
		// Quick check on the concurrent map first, with minimal locking.
		LifecycleMetadata metadata = this.lifecycleMetadataCache.get(clazz);
		if (metadata == null) {
			synchronized (this.lifecycleMetadataCache) {
				metadata = this.lifecycleMetadataCache.get(clazz);
				if (metadata == null) {
					metadata = buildLifecycleMetadata(clazz);
					this.lifecycleMetadataCache.put(clazz, metadata);
				}
				return metadata;
			}
		}
		return metadata;
	}

	private LifecycleMetadata buildLifecycleMetadata(final Class<?> clazz) {
		final boolean debug = logger.isDebugEnabled();
		LinkedList<LifecycleElement> initMethods = new LinkedList<LifecycleElement>();
		LinkedList<LifecycleElement> destroyMethods = new LinkedList<LifecycleElement>();
		Class<?> targetClass = clazz;

		do {
			final LinkedList<LifecycleElement> currInitMethods = new LinkedList<LifecycleElement>();
			final LinkedList<LifecycleElement> currDestroyMethods = new LinkedList<LifecycleElement>();

			ReflectionUtils.doWithLocalMethods(targetClass, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					if (initAnnotationType != null) {
						if (method.getAnnotation(initAnnotationType) != null) {
							LifecycleElement element = new LifecycleElement(method);
							currInitMethods.add(element);
							if (debug) {
								logger.debug("Found init method on class [" + clazz.getName() + "]: " + method);
							}
						}
					}
					if (destroyAnnotationType != null) {
						if (method.getAnnotation(destroyAnnotationType) != null) {
							currDestroyMethods.add(new LifecycleElement(method));
							if (debug) {
								logger.debug("Found destroy method on class [" + clazz.getName() + "]: " + method);
							}
						}
					}
				}
			});

			initMethods.addAll(0, currInitMethods);
			destroyMethods.addAll(currDestroyMethods);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return new LifecycleMetadata(clazz, initMethods, destroyMethods);
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.logger = LogFactory.getLog(getClass());
	}


	/**
	 * 有关被注解的init和destroy方法的信息.
	 */
	private class LifecycleMetadata {

		private final Class<?> targetClass;

		private final Collection<LifecycleElement> initMethods;

		private final Collection<LifecycleElement> destroyMethods;

		private volatile Set<LifecycleElement> checkedInitMethods;

		private volatile Set<LifecycleElement> checkedDestroyMethods;

		public LifecycleMetadata(Class<?> targetClass, Collection<LifecycleElement> initMethods,
				Collection<LifecycleElement> destroyMethods) {

			this.targetClass = targetClass;
			this.initMethods = initMethods;
			this.destroyMethods = destroyMethods;
		}

		public void checkConfigMembers(RootBeanDefinition beanDefinition) {
			Set<LifecycleElement> checkedInitMethods = new LinkedHashSet<LifecycleElement>(this.initMethods.size());
			for (LifecycleElement element : this.initMethods) {
				String methodIdentifier = element.getIdentifier();
				if (!beanDefinition.isExternallyManagedInitMethod(methodIdentifier)) {
					beanDefinition.registerExternallyManagedInitMethod(methodIdentifier);
					checkedInitMethods.add(element);
					if (logger.isDebugEnabled()) {
						logger.debug("Registered init method on class [" + this.targetClass.getName() + "]: " + element);
					}
				}
			}
			Set<LifecycleElement> checkedDestroyMethods = new LinkedHashSet<LifecycleElement>(this.destroyMethods.size());
			for (LifecycleElement element : this.destroyMethods) {
				String methodIdentifier = element.getIdentifier();
				if (!beanDefinition.isExternallyManagedDestroyMethod(methodIdentifier)) {
					beanDefinition.registerExternallyManagedDestroyMethod(methodIdentifier);
					checkedDestroyMethods.add(element);
					if (logger.isDebugEnabled()) {
						logger.debug("Registered destroy method on class [" + this.targetClass.getName() + "]: " + element);
					}
				}
			}
			this.checkedInitMethods = checkedInitMethods;
			this.checkedDestroyMethods = checkedDestroyMethods;
		}

		public void invokeInitMethods(Object target, String beanName) throws Throwable {
			Collection<LifecycleElement> initMethodsToIterate =
					(this.checkedInitMethods != null ? this.checkedInitMethods : this.initMethods);
			if (!initMethodsToIterate.isEmpty()) {
				boolean debug = logger.isDebugEnabled();
				for (LifecycleElement element : initMethodsToIterate) {
					if (debug) {
						logger.debug("Invoking init method on bean '" + beanName + "': " + element.getMethod());
					}
					element.invoke(target);
				}
			}
		}

		public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
			Collection<LifecycleElement> destroyMethodsToUse =
					(this.checkedDestroyMethods != null ? this.checkedDestroyMethods : this.destroyMethods);
			if (!destroyMethodsToUse.isEmpty()) {
				boolean debug = logger.isDebugEnabled();
				for (LifecycleElement element : destroyMethodsToUse) {
					if (debug) {
						logger.debug("Invoking destroy method on bean '" + beanName + "': " + element.getMethod());
					}
					element.invoke(target);
				}
			}
		}

		public boolean hasDestroyMethods() {
			Collection<LifecycleElement> destroyMethodsToUse =
					(this.checkedDestroyMethods != null ? this.checkedDestroyMethods : this.destroyMethods);
			return !destroyMethodsToUse.isEmpty();
		}
	}


	/**
	 * 被注解的方法的注入信息.
	 */
	private static class LifecycleElement {

		private final Method method;

		private final String identifier;

		public LifecycleElement(Method method) {
			if (method.getParameterTypes().length != 0) {
				throw new IllegalStateException("Lifecycle method annotation requires a no-arg method: " + method);
			}
			this.method = method;
			this.identifier = (Modifier.isPrivate(method.getModifiers()) ?
					ClassUtils.getQualifiedMethodName(method) : method.getName());
		}

		public Method getMethod() {
			return this.method;
		}

		public String getIdentifier() {
			return this.identifier;
		}

		public void invoke(Object target) throws Throwable {
			ReflectionUtils.makeAccessible(this.method);
			this.method.invoke(target, (Object[]) null);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof LifecycleElement)) {
				return false;
			}
			LifecycleElement otherElement = (LifecycleElement) other;
			return (this.identifier.equals(otherElement.identifier));
		}

		@Override
		public int hashCode() {
			return this.identifier.hashCode();
		}
	}
}
