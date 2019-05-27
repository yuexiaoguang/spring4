package org.springframework.aop.framework;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.core.DecoratingProxy;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * AOP代理工厂的实用方法.
 * 主要用于AOP框架内部使用.
 *
 * <p>有关不依赖于AOP框架内部的通用AOP实用程序方法的集合，请参阅{@link org.springframework.aop.support.AopUtils}.
 */
public abstract class AopProxyUtils {

	/**
	 * 获取给定代理后面的单例目标对象.
	 * 
	 * @param candidate 要检查的（潜在）代理
	 * 
	 * @return {@link SingletonTargetSource}中管理的单例目标对象,
	 * 或{@code null} (不是代理, 不是现有的单例目标)
	 * @since 4.3.8
	 */
	public static Object getSingletonTarget(Object candidate) {
		if (candidate instanceof Advised) {
			TargetSource targetSource = ((Advised) candidate).getTargetSource();
			if (targetSource instanceof SingletonTargetSource) {
				return ((SingletonTargetSource) targetSource).getTarget();
			}
		}
		return null;
	}

	/**
	 * 确定给定bean实例的最终目标类, 不仅遍历顶级代理, 还遍历任意数量的嵌套代理;
	 * 尽可能没有副作用, 即, 仅适用于单例目标.
	 * 
	 * @param candidate 要检查的实例 (可能是AOP代理)
	 * 
	 * @return 最终目标类 (或者给定对象的普通类作为后备; 永远不会是{@code null})
	 */
	public static Class<?> ultimateTargetClass(Object candidate) {
		Assert.notNull(candidate, "Candidate object must not be null");
		Object current = candidate;
		Class<?> result = null;
		while (current instanceof TargetClassAware) {
			result = ((TargetClassAware) current).getTargetClass();
			current = getSingletonTarget(current);
		}
		if (result == null) {
			result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
		}
		return result;
	}

	/**
	 * 确定要为给定AOP配置代理的完整接口集.
	 * <p>这将始终添加{@link Advised}接口, 除非AdvisedSupport的{@link AdvisedSupport#setOpaque "opaque"}标志已启用.
	 * 总是添加{@link org.springframework.aop.SpringProxy}标记接口.
	 * 
	 * @param advised 代理配置
	 * 
	 * @return 代理的完整接口集
	 */
	public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
		return completeProxiedInterfaces(advised, false);
	}

	/**
	 * 确定要为给定AOP配置代理的完整接口集.
	 * <p>这将始终添加{@link Advised}接口, 除非AdvisedSupport的{@link AdvisedSupport#setOpaque "opaque"}标志已启用.
	 * 总是添加{@link org.springframework.aop.SpringProxy}标记接口.
	 * 
	 * @param advised 代理配置
	 * @param decoratingProxy 是否公开{@link DecoratingProxy}接口
	 * 
	 * @return 代理的完整接口集
	 * @since 4.3
	 */
	static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
		Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
		if (specifiedInterfaces.length == 0) {
			// No user-specified interfaces: check whether target class is an interface.
			Class<?> targetClass = advised.getTargetClass();
			if (targetClass != null) {
				if (targetClass.isInterface()) {
					advised.setInterfaces(targetClass);
				}
				else if (Proxy.isProxyClass(targetClass)) {
					advised.setInterfaces(targetClass.getInterfaces());
				}
				specifiedInterfaces = advised.getProxiedInterfaces();
			}
		}
		boolean addSpringProxy = !advised.isInterfaceProxied(SpringProxy.class);
		boolean addAdvised = !advised.isOpaque() && !advised.isInterfaceProxied(Advised.class);
		boolean addDecoratingProxy = (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class));
		int nonUserIfcCount = 0;
		if (addSpringProxy) {
			nonUserIfcCount++;
		}
		if (addAdvised) {
			nonUserIfcCount++;
		}
		if (addDecoratingProxy) {
			nonUserIfcCount++;
		}
		Class<?>[] proxiedInterfaces = new Class<?>[specifiedInterfaces.length + nonUserIfcCount];
		System.arraycopy(specifiedInterfaces, 0, proxiedInterfaces, 0, specifiedInterfaces.length);
		int index = specifiedInterfaces.length;
		if (addSpringProxy) {
			proxiedInterfaces[index] = SpringProxy.class;
			index++;
		}
		if (addAdvised) {
			proxiedInterfaces[index] = Advised.class;
			index++;
		}
		if (addDecoratingProxy) {
			proxiedInterfaces[index] = DecoratingProxy.class;
		}
		return proxiedInterfaces;
	}

	/**
	 * 提取给定代理实现的用户指定的接口, 即代理实现的所有非Advised接口.
	 * 
	 * @param proxy 要分析的代理 (通常是JDK动态代理)
	 * 
	 * @return 代理实现的所有用户指定的接口, 按原始顺序 (never {@code null} or empty)
	 */
	public static Class<?>[] proxiedUserInterfaces(Object proxy) {
		Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
		int nonUserIfcCount = 0;
		if (proxy instanceof SpringProxy) {
			nonUserIfcCount++;
		}
		if (proxy instanceof Advised) {
			nonUserIfcCount++;
		}
		if (proxy instanceof DecoratingProxy) {
			nonUserIfcCount++;
		}
		Class<?>[] userInterfaces = new Class<?>[proxyInterfaces.length - nonUserIfcCount];
		System.arraycopy(proxyInterfaces, 0, userInterfaces, 0, userInterfaces.length);
		Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
		return userInterfaces;
	}

	/**
	 * 检查给定AdvisedSupport对象后面的代理的相等性.
	 * 与AdvisedSupport对象的相等性不同: 相反，接口，切面和目标源的相等性.
	 */
	public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
		return (a == b ||
				(equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
	}

	/**
	 * 检查给定AdvisedSupport对象后面的代理接口的相等性.
	 */
	public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
		return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
	}

	/**
	 * 检查给定AdvisedSupport对象后面的切面的相等性.
	 */
	public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
		return Arrays.equals(a.getAdvisors(), b.getAdvisors());
	}


	/**
	 * 使给定参数适应给定方法中的目标签名,
	 *  如果有必要: 特别是, 如果给定的vararg参数数组与方法中声明的vararg参数的数组类型不匹配.
	 * 
	 * @param method 目标方法
	 * @param arguments 给定的参数
	 * 
	 * @return 克隆的参数数组, 或原始的, 如果不需要适应
	 * @since 4.2.3
	 */
	static Object[] adaptArgumentsIfNecessary(Method method, Object... arguments) {
		if (method.isVarArgs() && !ObjectUtils.isEmpty(arguments)) {
			Class<?>[] paramTypes = method.getParameterTypes();
			if (paramTypes.length == arguments.length) {
				int varargIndex = paramTypes.length - 1;
				Class<?> varargType = paramTypes[varargIndex];
				if (varargType.isArray()) {
					Object varargArray = arguments[varargIndex];
					if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
						Object[] newArguments = new Object[arguments.length];
						System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
						Class<?> targetElementType = varargType.getComponentType();
						int varargLength = Array.getLength(varargArray);
						Object newVarargArray = Array.newInstance(targetElementType, varargLength);
						System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
						newArguments[varargIndex] = newVarargArray;
						return newArguments;
					}
				}
			}
		}
		return arguments;
	}
}
