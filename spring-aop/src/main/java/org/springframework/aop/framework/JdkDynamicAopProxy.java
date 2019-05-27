package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Spring AOP框架的基于JDK的{@link AopProxy}实现, 基于JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>创建动态代理, 实现AopProxy公开的接口. 动态代理不能用于代理类中定义的方法, 而不是接口.
 *
 * <p>应通过代理工厂获取此类对象, 由{@link AdvisedSupport}类配置. 该类是Spring的AOP框架内部的，不需要由客户端代码直接使用.
 *
 * <p>如果底层（目标）类是线程安全的，使用此类创建的代理将是线程安全的.
 *
 * <p>只要所有Advisor（包括Advice和Pointcut）和TargetSource都可序列化，代理就是可序列化的.
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = 5531744639992436476L;


	/*
	 * NOTE: 可以通过将“invoke”重构为模板方法来避免此类与CGLIB代理之间的代码重复.
	 * 但是, 与复制粘贴解决方案相比, 此方法至少增加了10％的性能开销, 所以我们牺牲了优雅的表现.
	 * (有一个很好的测试套件，以确保不同的代理行为相同 :-) 还可以更轻松地利用每个类中的次要优化.
	 */

	/** 使用静态Log来避免序列化问题 */
	private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

	/** 用于配置此代理的配置 */
	private final AdvisedSupport advised;

	/**
	 * 是否在代理接口上定义了{@link #equals}方法?
	 */
	private boolean equalsDefined;

	/**
	 * 是否在代理接口上定义了{@link #hashCode}方法?
	 */
	private boolean hashCodeDefined;


	/**
	 * @param config AOP配置
	 * 
	 * @throws AopConfigException 如果配置无效. 在这种情况下, 尝试抛出一个信息异常, 而不是让一个神秘的失败发生.
	 */
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;
	}


	@Override
	public Object getProxy() {
		return getProxy(ClassUtils.getDefaultClassLoader());
	}

	@Override
	public Object getProxy(ClassLoader classLoader) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating JDK dynamic proxy: target source is " + this.advised.getTargetSource());
		}
		Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
		findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
		return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
	}

	/**
	 * 查找可在所提供的接口集上定义的任何{@link #equals}或{@link #hashCode}方法.
	 * 
	 * @param proxiedInterfaces 要反射的接口
	 */
	private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
		for (Class<?> proxiedInterface : proxiedInterfaces) {
			Method[] methods = proxiedInterface.getDeclaredMethods();
			for (Method method : methods) {
				if (AopUtils.isEqualsMethod(method)) {
					this.equalsDefined = true;
				}
				if (AopUtils.isHashCodeMethod(method)) {
					this.hashCodeDefined = true;
				}
				if (this.equalsDefined && this.hashCodeDefined) {
					return;
				}
			}
		}
	}


	/**
	 * {@code InvocationHandler.invoke}的实现.
	 * <p>调用者将看到目标引发的异常, 除非hook方法抛出异常.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		MethodInvocation invocation;
		Object oldProxy = null;
		boolean setProxyContext = false;

		TargetSource targetSource = this.advised.targetSource;
		Class<?> targetClass = null;
		Object target = null;

		try {
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// 目标没有实现 equals(Object) 方法.
				return equals(args[0]);
			}
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// 目标没有实现 hashCode() 方法.
				return hashCode();
			}
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// 只是声明了 getDecoratedClass() -> 调度到代理配置.
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// 使用代理配置在ProxyConfig上进行服务调用...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			Object retVal;

			if (this.advised.exposeProxy) {
				// 让调用可用.
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// 可能是 null. 尽可能缩短“拥有”目标的时间, 如果它来自一个池.
			target = targetSource.getTarget();
			if (target != null) {
				targetClass = target.getClass();
			}

			// 获取此方法的拦截链.
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// 检查是否有增强. 如果没有, 可以回避对目标的直接反射调用, 并避免创建一个MethodInvocation.
			if (chain.isEmpty()) {
				// 可以跳过创建 MethodInvocation: 只是直接调用目标
				// 请注意, 最终的调用者必须是一个InvokerInterceptor, 所以我们知道它只对目标进行反射操作, 没有热交换或花哨的代理.
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
			else {
				// 需要创建一个方法调用...
				invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// 通过拦截器链进入连接点.
				retVal = invocation.proceed();
			}

			// Massage return value if necessary.
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: 它返回 "this", 并且该方法的返回类型是类型兼容的.
				// 请注意，如果目标在另一个返回的对象中设置对自身的引用，我们将无法帮助您.
				retVal = proxy;
			}
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			return retVal;
		}
		finally {
			if (target != null && !targetSource.isStatic()) {
				// 必须来自TargetSource.
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
				// 恢复旧代理.
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}


	/**
	 * 相等意味着接口，切面和TargetSource是相同的.
	 * <p>比较对象可以是JdkDynamicAopProxy实例本身, 也可以是包装JdkDynamicAopProxy实例的动态代理.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			otherProxy = (JdkDynamicAopProxy) other;
		}
		else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = (JdkDynamicAopProxy) ih;
		}
		else {
			// 不是有效的比较...
			return false;
		}

		// 如果到了这里, otherProxy是另一个AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * 代理使用TargetSource的哈希码.
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}
}
