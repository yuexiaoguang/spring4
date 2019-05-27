package org.springframework.beans.factory.support;

import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 实现{@link DisposableBean}和{@link Runnable}接口的适配器, 在给定的bean实例上执行各种销毁步骤:
 * <ul>
 * <li>DestructionAwareBeanPostProcessors;
 * <li>实现DisposableBean本身的bean;
 * <li>在bean定义上指定的自定义destroy方法.
 * </ul>
 */
@SuppressWarnings("serial")
class DisposableBeanAdapter implements DisposableBean, Runnable, Serializable {

	private static final String CLOSE_METHOD_NAME = "close";

	private static final String SHUTDOWN_METHOD_NAME = "shutdown";

	private static final Log logger = LogFactory.getLog(DisposableBeanAdapter.class);

	private static Class<?> closeableInterface;

	static {
		try {
			closeableInterface = ClassUtils.forName("java.lang.AutoCloseable",
					DisposableBeanAdapter.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			closeableInterface = Closeable.class;
		}
	}


	private final Object bean;

	private final String beanName;

	private final boolean invokeDisposableBean;

	private final boolean nonPublicAccessAllowed;

	private final AccessControlContext acc;

	private String destroyMethodName;

	private transient Method destroyMethod;

	private List<DestructionAwareBeanPostProcessor> beanPostProcessors;


	/**
	 * @param bean bean实例 (never {@code null})
	 * @param beanName bean的名称
	 * @param beanDefinition 合并的bean定义
	 * @param postProcessors BeanPostProcessor列表 (可能是 DestructionAwareBeanPostProcessor)
	 */
	public DisposableBeanAdapter(Object bean, String beanName, RootBeanDefinition beanDefinition,
			List<BeanPostProcessor> postProcessors, AccessControlContext acc) {

		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = beanName;
		this.invokeDisposableBean =
				(this.bean instanceof DisposableBean && !beanDefinition.isExternallyManagedDestroyMethod("destroy"));
		this.nonPublicAccessAllowed = beanDefinition.isNonPublicAccessAllowed();
		this.acc = acc;
		String destroyMethodName = inferDestroyMethodIfNecessary(bean, beanDefinition);
		if (destroyMethodName != null && !(this.invokeDisposableBean && "destroy".equals(destroyMethodName)) &&
				!beanDefinition.isExternallyManagedDestroyMethod(destroyMethodName)) {
			this.destroyMethodName = destroyMethodName;
			this.destroyMethod = determineDestroyMethod();
			if (this.destroyMethod == null) {
				if (beanDefinition.isEnforceDestroyMethod()) {
					throw new BeanDefinitionValidationException("Couldn't find a destroy method named '" +
							destroyMethodName + "' on bean with name '" + beanName + "'");
				}
			}
			else {
				Class<?>[] paramTypes = this.destroyMethod.getParameterTypes();
				if (paramTypes.length > 1) {
					throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
							beanName + "' has more than one parameter - not supported as destroy method");
				}
				else if (paramTypes.length == 1 && boolean.class != paramTypes[0]) {
					throw new BeanDefinitionValidationException("Method '" + destroyMethodName + "' of bean '" +
							beanName + "' has a non-boolean parameter - not supported as destroy method");
				}
			}
		}
		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
	}

	/**
	 * @param bean bean实例 (never {@code null})
	 * @param postProcessors BeanPostProcessor列表 (可能是 DestructionAwareBeanPostProcessor)
	 */
	public DisposableBeanAdapter(Object bean, List<BeanPostProcessor> postProcessors, AccessControlContext acc) {
		Assert.notNull(bean, "Disposable bean must not be null");
		this.bean = bean;
		this.beanName = null;
		this.invokeDisposableBean = (this.bean instanceof DisposableBean);
		this.nonPublicAccessAllowed = true;
		this.acc = acc;
		this.beanPostProcessors = filterPostProcessors(postProcessors, bean);
	}

	private DisposableBeanAdapter(Object bean, String beanName, boolean invokeDisposableBean,
			boolean nonPublicAccessAllowed, String destroyMethodName,
			List<DestructionAwareBeanPostProcessor> postProcessors) {

		this.bean = bean;
		this.beanName = beanName;
		this.invokeDisposableBean = invokeDisposableBean;
		this.nonPublicAccessAllowed = nonPublicAccessAllowed;
		this.acc = null;
		this.destroyMethodName = destroyMethodName;
		this.beanPostProcessors = postProcessors;
	}


	/**
	 * 如果给定beanDefinition的“destroyMethodName”属性的当前值是{@link AbstractBeanDefinition#INFER_METHOD}, 则尝试推断出destroy方法.
	 * 候选方法目前仅限于名称为“close”或“shutdown”的public, 无参数的方法(无论是在本地声明还是继承).
	 * 如果没有找到这样的方法, 给定的BeanDefinition的“destroyMethodName”将更新为null, 否则设置为推断方法的名称.
	 * 此常量作为 {@code @Bean#destroyMethod}属性的默认值, 常量的值也可以在XML中的 {@code <bean destroy-method="">} 或 {@code <beans default-destroy-method="">}属性上使用.
	 * <p>还处理 {@link java.io.Closeable}和{@link java.lang.AutoCloseable}接口, 反射调用bean实现的 "close"方法.
	 */
	private String inferDestroyMethodIfNecessary(Object bean, RootBeanDefinition beanDefinition) {
		String destroyMethodName = beanDefinition.getDestroyMethodName();
		if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName) ||
				(destroyMethodName == null && closeableInterface.isInstance(bean))) {
			// 仅在bean未明确实现DisposableBean的情况下, 执行destroy方法推理或Closeable检测
			if (!(bean instanceof DisposableBean)) {
				try {
					return bean.getClass().getMethod(CLOSE_METHOD_NAME).getName();
				}
				catch (NoSuchMethodException ex) {
					try {
						return bean.getClass().getMethod(SHUTDOWN_METHOD_NAME).getName();
					}
					catch (NoSuchMethodException ex2) {
						// no candidate destroy method found
					}
				}
			}
			return null;
		}
		return (StringUtils.hasLength(destroyMethodName) ? destroyMethodName : null);
	}

	/**
	 * 在List中搜索所有的DestructionAwareBeanPostProcessors.
	 * 
	 * @param processors 要搜索的List
	 * 
	 * @return 已过滤的DestructionAwareBeanPostProcessors列表
	 */
	private List<DestructionAwareBeanPostProcessor> filterPostProcessors(List<BeanPostProcessor> processors, Object bean) {
		List<DestructionAwareBeanPostProcessor> filteredPostProcessors = null;
		if (!CollectionUtils.isEmpty(processors)) {
			filteredPostProcessors = new ArrayList<DestructionAwareBeanPostProcessor>(processors.size());
			for (BeanPostProcessor processor : processors) {
				if (processor instanceof DestructionAwareBeanPostProcessor) {
					DestructionAwareBeanPostProcessor dabpp = (DestructionAwareBeanPostProcessor) processor;
					try {
						if (dabpp.requiresDestruction(bean)) {
							filteredPostProcessors.add(dabpp);
						}
					}
					catch (AbstractMethodError err) {
						// A pre-4.3 third-party DestructionAwareBeanPostProcessor...
						// 从5.0开始, 可以让requiresDestruction成为一个返回true的Java 8默认方法.
						filteredPostProcessors.add(dabpp);
					}
				}
			}
		}
		return filteredPostProcessors;
	}


	@Override
	public void run() {
		destroy();
	}

	@Override
	public void destroy() {
		if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
			for (DestructionAwareBeanPostProcessor processor : this.beanPostProcessors) {
				processor.postProcessBeforeDestruction(this.bean, this.beanName);
			}
		}

		if (this.invokeDisposableBean) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking destroy() on bean with name '" + this.beanName + "'");
			}
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
							((DisposableBean) bean).destroy();
							return null;
						}
					}, acc);
				}
				else {
					((DisposableBean) bean).destroy();
				}
			}
			catch (Throwable ex) {
				String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
				if (logger.isDebugEnabled()) {
					logger.warn(msg, ex);
				}
				else {
					logger.warn(msg + ": " + ex);
				}
			}
		}

		if (this.destroyMethod != null) {
			invokeCustomDestroyMethod(this.destroyMethod);
		}
		else if (this.destroyMethodName != null) {
			Method methodToCall = determineDestroyMethod();
			if (methodToCall != null) {
				invokeCustomDestroyMethod(methodToCall);
			}
		}
	}


	private Method determineDestroyMethod() {
		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(new PrivilegedAction<Method>() {
					@Override
					public Method run() {
						return findDestroyMethod();
					}
				});
			}
			else {
				return findDestroyMethod();
			}
		}
		catch (IllegalArgumentException ex) {
			throw new BeanDefinitionValidationException("Could not find unique destroy method on bean with name '" +
					this.beanName + ": " + ex.getMessage());
		}
	}

	private Method findDestroyMethod() {
		return (this.nonPublicAccessAllowed ?
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass(), this.destroyMethodName) :
				BeanUtils.findMethodWithMinimalParameters(this.bean.getClass().getMethods(), this.destroyMethodName));
	}

	/**
	 * 在给定的bean上调用指定的自定义destroy方法.
	 * <p>如果找到, 此实现将调用no-arg方法, 否则检查具有单个boolean参数的方法 (传入 "true", 假设是 "force"参数), 否则记录一个错误.
	 */
	private void invokeCustomDestroyMethod(final Method destroyMethod) {
		Class<?>[] paramTypes = destroyMethod.getParameterTypes();
		final Object[] args = new Object[paramTypes.length];
		if (paramTypes.length == 1) {
			args[0] = Boolean.TRUE;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'");
		}
		try {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						ReflectionUtils.makeAccessible(destroyMethod);
						return null;
					}
				});
				try {
					AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
							destroyMethod.invoke(bean, args);
							return null;
						}
					}, acc);
				}
				catch (PrivilegedActionException pax) {
					throw (InvocationTargetException) pax.getException();
				}
			}
			else {
				ReflectionUtils.makeAccessible(destroyMethod);
				destroyMethod.invoke(bean, args);
			}
		}
		catch (InvocationTargetException ex) {
			String msg = "Invocation of destroy method '" + this.destroyMethodName +
					"' failed on bean with name '" + this.beanName + "'";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex.getTargetException());
			}
			else {
				logger.warn(msg + ": " + ex.getTargetException());
			}
		}
		catch (Throwable ex) {
			logger.error("Couldn't invoke destroy method '" + this.destroyMethodName +
					"' on bean with name '" + this.beanName + "'", ex);
		}
	}


	/**
	 * 序列化此类状态的副本, 过滤掉不可序列化的BeanPostProcessors.
	 */
	protected Object writeReplace() {
		List<DestructionAwareBeanPostProcessor> serializablePostProcessors = null;
		if (this.beanPostProcessors != null) {
			serializablePostProcessors = new ArrayList<DestructionAwareBeanPostProcessor>();
			for (DestructionAwareBeanPostProcessor postProcessor : this.beanPostProcessors) {
				if (postProcessor instanceof Serializable) {
					serializablePostProcessors.add(postProcessor);
				}
			}
		}
		return new DisposableBeanAdapter(this.bean, this.beanName, this.invokeDisposableBean,
				this.nonPublicAccessAllowed, this.destroyMethodName, serializablePostProcessors);
	}


	/**
	 * 检查给定的bean是否有任何类型的destroy方法来调用.
	 * 
	 * @param bean bean实例
	 * @param beanDefinition 相应的bean定义
	 */
	public static boolean hasDestroyMethod(Object bean, RootBeanDefinition beanDefinition) {
		if (bean instanceof DisposableBean || closeableInterface.isInstance(bean)) {
			return true;
		}
		String destroyMethodName = beanDefinition.getDestroyMethodName();
		if (AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName)) {
			return (ClassUtils.hasMethod(bean.getClass(), CLOSE_METHOD_NAME) ||
					ClassUtils.hasMethod(bean.getClass(), SHUTDOWN_METHOD_NAME));
		}
		return StringUtils.hasLength(destroyMethodName);
	}

	/**
	 * 检查给定的bean是否具有适用于它的销毁感知后处理器.
	 * 
	 * @param bean bean实例
	 * @param postProcessors 后处理器候选
	 */
	public static boolean hasApplicableProcessors(Object bean, List<BeanPostProcessor> postProcessors) {
		if (!CollectionUtils.isEmpty(postProcessors)) {
			for (BeanPostProcessor processor : postProcessors) {
				if (processor instanceof DestructionAwareBeanPostProcessor) {
					DestructionAwareBeanPostProcessor dabpp = (DestructionAwareBeanPostProcessor) processor;
					try {
						if (dabpp.requiresDestruction(bean)) {
							return true;
						}
					}
					catch (AbstractMethodError err) {
						// A pre-4.3 third-party DestructionAwareBeanPostProcessor...
						// 从5.0开始, 可以让requiresDestruction成为一个返回true的Java 8默认方法.
						return true;
					}
				}
			}
		}
		return false;
	}

}
