package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;

/**
 * 用于匹配getter、setter和静态方法的切点常量, 用于操作和评估切入点.
 * 这些方法对于使用union和intersection方法组合切点特别有用.
 */
public abstract class Pointcuts {

	/** 匹配任何类中的所有bean属性setter的切点 */
	public static final Pointcut SETTERS = SetterPointcut.INSTANCE;

	/** 匹配任何类中的所有bean属性getter的切点 */
	public static final Pointcut GETTERS = GetterPointcut.INSTANCE;


	/**
	 * Match all methods that <b>either</b> (or both) of the given pointcuts matches.
	 * 
	 * @param pc1 the first Pointcut
	 * @param pc2 the second Pointcut
	 * 
	 * @return a distinct Pointcut that matches all methods that either
	 * of the given Pointcuts matches
	 */
	public static Pointcut union(Pointcut pc1, Pointcut pc2) {
		return new ComposablePointcut(pc1).union(pc2);
	}

	/**
	 * Match all methods that <b>both</b> the given pointcuts match.
	 * 
	 * @param pc1 the first Pointcut
	 * @param pc2 the second Pointcut
	 * 
	 * @return a distinct Pointcut that matches all methods that both
	 * of the given Pointcuts match
	 */
	public static Pointcut intersection(Pointcut pc1, Pointcut pc2) {
		return new ComposablePointcut(pc1).intersection(pc2);
	}

	/**
	 * 对切点匹配执行最便宜的检查.
	 * 
	 * @param pointcut 要匹配的切点
	 * @param method 要匹配的方法
	 * @param targetClass 目标类
	 * @param args 方法的参数
	 * 
	 * @return 是否存在运行时匹配
	 */
	public static boolean matches(Pointcut pointcut, Method method, Class<?> targetClass, Object... args) {
		Assert.notNull(pointcut, "Pointcut must not be null");
		if (pointcut == Pointcut.TRUE) {
			return true;
		}
		if (pointcut.getClassFilter().matches(targetClass)) {
			// 只检查它是否超过了第一个障碍.
			MethodMatcher mm = pointcut.getMethodMatcher();
			if (mm.matches(method, targetClass)) {
				// 要额外的运行时（参数）检查.
				return (!mm.isRuntime() || mm.matches(method, targetClass, args));
			}
		}
		return false;
	}


	/**
	 * 与bean属性Setter匹配的切点实现.
	 */
	@SuppressWarnings("serial")
	private static class SetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

		public static final SetterPointcut INSTANCE = new SetterPointcut();

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (method.getName().startsWith("set") &&
					method.getParameterTypes().length == 1 &&
					method.getReturnType() == Void.TYPE);
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}


	/**
	 * 与bean属性Getter匹配的切点实现.
	 */
	@SuppressWarnings("serial")
	private static class GetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

		public static final GetterPointcut INSTANCE = new GetterPointcut();

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (method.getName().startsWith("get") &&
					method.getParameterTypes().length == 0);
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}

}
