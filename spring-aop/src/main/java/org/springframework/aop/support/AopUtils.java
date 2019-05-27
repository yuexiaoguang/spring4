package org.springframework.aop.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.aop.Advisor;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * AOP支持代码的实用方法.
 *
 * <p>主要供Spring内部使用.
 *
 * <p>有关特定于框架的AOP实用程序方法的集合，请参阅{@link org.springframework.aop.framework.AopProxyUtils}，
 * 这些方法依赖于Spring的AOP框架实现的内部.
 */
public abstract class AopUtils {

	/**
	 * 检查给定对象是JDK动态代理还是CGLIB代理.
	 * <p>此方法还检查给定对象是否是{@link SpringProxy}的实例.
	 * 
	 * @param object 要检查的对象
	 */
	public static boolean isAopProxy(Object object) {
		return (object instanceof SpringProxy &&
				(Proxy.isProxyClass(object.getClass()) || ClassUtils.isCglibProxyClass(object.getClass())));
	}

	/**
	 * 检查给定对象是否是JDK动态代理.
	 * <p>此方法超出了{@link Proxy#isProxyClass(Class)}的实现, 另外检查给定对象是否是{@link SpringProxy}的实例.
	 * 
	 * @param object 要检查的对象
	 */
	public static boolean isJdkDynamicProxy(Object object) {
		return (object instanceof SpringProxy && Proxy.isProxyClass(object.getClass()));
	}

	/**
	 * 检查给定对象是否为CGLIB代理.
	 * <p>此方法超出了{@link ClassUtils#isCglibProxy(Object)}的实现, 另外检查给定对象是否是{@link SpringProxy}的实例.
	 * 
	 * @param object 要检查的对象
	 */
	public static boolean isCglibProxy(Object object) {
		return (object instanceof SpringProxy && ClassUtils.isCglibProxy(object));
	}

	/**
	 * 确定给定bean实例的目标类，该实例可能是AOP代理.
	 * <p>返回AOP代理的目标类，否则返回普通类.
	 * 
	 * @param candidate 要检查的实例 (可能是AOP代理)
	 * 
	 * @return 目标类 (或者给定对象的普通类作为后备; never {@code null})
	 */
	public static Class<?> getTargetClass(Object candidate) {
		Assert.notNull(candidate, "Candidate object must not be null");
		Class<?> result = null;
		if (candidate instanceof TargetClassAware) {
			result = ((TargetClassAware) candidate).getTargetClass();
		}
		if (result == null) {
			result = (isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
		}
		return result;
	}

	/**
	 * 在目标类型上选择一个可调用方法: 无论是实际暴露在目标类型上的给定方法本身,
	 * 或者是目标类型的接口之一或目标类型本身上的相应方法.
	 * 
	 * @param method 要检查的方法
	 * @param targetType 要搜索的目标类型 (通常是AOP代理)
	 * 
	 * @return 目标类型的相应可调用方法
	 * @throws IllegalStateException 如果给定的方法在给定的目标类型上不可调用 (通常是由于代理不匹配)
	 * @since 4.3
	 */
	public static Method selectInvocableMethod(Method method, Class<?> targetType) {
		Method methodToUse = MethodIntrospector.selectInvocableMethod(method, targetType);
		if (Modifier.isPrivate(methodToUse.getModifiers()) && !Modifier.isStatic(methodToUse.getModifiers()) &&
				SpringProxy.class.isAssignableFrom(targetType)) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' found on proxy for target class '%s' but cannot " +
					"be delegated to target bean. Switch its visibility to package or protected.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
		return methodToUse;
	}

	/**
	 * 确定给定方法是否为 "equals"方法.
	 */
	public static boolean isEqualsMethod(Method method) {
		return ReflectionUtils.isEqualsMethod(method);
	}

	/**
	 * 确定给定方法是否为 "hashCode"方法.
	 */
	public static boolean isHashCodeMethod(Method method) {
		return ReflectionUtils.isHashCodeMethod(method);
	}

	/**
	 * 确定给定方法是否为 "toString"方法.
	 */
	public static boolean isToStringMethod(Method method) {
		return ReflectionUtils.isToStringMethod(method);
	}

	/**
	 * 确定给定方法是否为 "finalize"方法.
	 */
	public static boolean isFinalizeMethod(Method method) {
		return (method != null && method.getName().equals("finalize") &&
				method.getParameterTypes().length == 0);
	}

	/**
	 * 给定一个可能来自接口的方法，以及当前AOP调用中使用的目标类，找到相应的目标方法（如果有）.
	 * E.g. 方法可能是 {@code IFoo.bar()}, 而且目标类可能是 {@code DefaultFoo}.
	 * 在这种情况下, 方法可能是 {@code DefaultFoo.bar()}. 这样可以找到该方法的属性.
	 * <p><b>NOTE:</b> 与 {@link org.springframework.util.ClassUtils#getMostSpecificMethod}相反,
	 * 此方法解析Java 5桥接方法,  以便从<i>原始</i>方法定义中检索属性.
	 * 
	 * @param method 要调用的方法, 可能来自一个接口
	 * @param targetClass 当前调用的目标类.  可能是{@code null}, 甚至可能没有实现该方法.
	 * 
	 * @return 具体的目标方法, 或原始方法, 如果{@code targetClass}没有实现它, 或是 {@code null}
	 */
	public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
		Method resolvedMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		// 如果我们正在处理具有泛型参数的方法, 找到原始方法.
		return BridgeMethodResolver.findBridgedMethod(resolvedMethod);
	}

	/**
	 * 给定的切点是否适用于给定的类?
	 * <p>这是一个重要的测试, 因为它可以用来优化类的切点.
	 * 
	 * @param pc 要检查的静态或动态切点
	 * @param targetClass 要测试的类
	 * 
	 * @return 切点是否适用于任何方法
	 */
	public static boolean canApply(Pointcut pc, Class<?> targetClass) {
		return canApply(pc, targetClass, false);
	}

	/**
	 * 给定的切点是否适用于给定的类?
	 * <p>这是一个重要的测试, 因为它可以用来优化类的切点.
	 * 
	 * @param pc 要检查的静态或动态切点
	 * @param targetClass 要测试的类
	 * @param hasIntroductions 此bean的切面链是否包含任何引入
	 * 
	 * @return 切点是否适用于任何方法
	 */
	public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
		Assert.notNull(pc, "Pointcut must not be null");
		if (!pc.getClassFilter().matches(targetClass)) {
			return false;
		}

		MethodMatcher methodMatcher = pc.getMethodMatcher();
		if (methodMatcher == MethodMatcher.TRUE) {
			// 如果我们仍然匹配任何方法，则无需迭代方法...
			return true;
		}

		IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
		if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
			introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
		}

		Set<Class<?>> classes = new LinkedHashSet<Class<?>>(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
		classes.add(targetClass);
		for (Class<?> clazz : classes) {
			Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
			for (Method method : methods) {
				if ((introductionAwareMethodMatcher != null &&
						introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions)) ||
						methodMatcher.matches(method, targetClass)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 给定的切面是否适用于给定的类?
	 * 这是一项重要的测试，因为它可以用来优化类的切面.
	 * 
	 * @param advisor 要检查的切面
	 * @param targetClass 正在测试的类
	 * 
	 * @return 切点是否适用于任何方法
	 */
	public static boolean canApply(Advisor advisor, Class<?> targetClass) {
		return canApply(advisor, targetClass, false);
	}

	/**
	 * 给定的切面是否适用于给定的类?
	 * <p>这是一项重要的测试，因为它可以用来优化类的切面.
	 * 此版本还考虑了引入 (for IntroductionAwareMethodMatchers).
	 * 
	 * @param advisor 要检查的切面
	 * @param targetClass 正在测试的类
	 * @param hasIntroductions 此bean的切面链是否包含任何引入
	 * 
	 * @return 切点是否适用于任何方法
	 */
	public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
		if (advisor instanceof IntroductionAdvisor) {
			return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
		}
		else if (advisor instanceof PointcutAdvisor) {
			PointcutAdvisor pca = (PointcutAdvisor) advisor;
			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
		}
		else {
			// 它没有切点，所以我们认为它适用.
			return true;
		}
	}

	/**
	 * 确定适用于给定类的{@code candidateAdvisors}列表的子列表.
	 * 
	 * @param candidateAdvisors 要进行评估的Advisor
	 * @param clazz 目标类
	 * 
	 * @return 可以应用于给定类的对象的Advisor子列表 (可能是传入列表的原样)
	 */
	public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
		if (candidateAdvisors.isEmpty()) {
			return candidateAdvisors;
		}
		List<Advisor> eligibleAdvisors = new LinkedList<Advisor>();
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
				eligibleAdvisors.add(candidate);
			}
		}
		boolean hasIntroductions = !eligibleAdvisors.isEmpty();
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor) {
				// already processed
				continue;
			}
			if (canApply(candidate, clazz, hasIntroductions)) {
				eligibleAdvisors.add(candidate);
			}
		}
		return eligibleAdvisors;
	}

	/**
	 * 通过反射调用给定的目标, 作为AOP方法调用的一部分.
	 * 
	 * @param target 目标对象
	 * @param method 要调用的方法
	 * @param args 方法的参数
	 * 
	 * @return 调用结果
	 * @throws Throwable 如果目标方法抛出异常
	 * @throws org.springframework.aop.AopInvocationException 如果出现反射错误
	 */
	public static Object invokeJoinpointUsingReflection(Object target, Method method, Object[] args)
			throws Throwable {

		// 使用反射来调用该方法.
		try {
			ReflectionUtils.makeAccessible(method);
			return method.invoke(target, args);
		}
		catch (InvocationTargetException ex) {
			// 调用的方法抛出了一个受检异常.
			// 我们必须重新抛出它. 客户端不会看到拦截器.
			throw ex.getTargetException();
		}
		catch (IllegalArgumentException ex) {
			throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
					method + "] on target [" + target + "]", ex);
		}
		catch (IllegalAccessException ex) {
			throw new AopInvocationException("Could not access method [" + method + "]", ex);
		}
	}

}
