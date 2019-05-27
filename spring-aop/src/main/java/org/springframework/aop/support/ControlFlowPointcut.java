package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 用于简单的 <b>cflow</b>形式的切点的切点和方法匹配器.
 * 请注意, 评估此类切点比评估正常切点慢10-15倍, 但在某些情况下它们很有用.
 */
@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

	private Class<?> clazz;

	private String methodName;

	private volatile int evaluations;


	/**
	 * 构造一个新的切点, 匹配该类下面的所有控制流.
	 * 
	 * @param clazz the clazz
	 */
	public ControlFlowPointcut(Class<?> clazz) {
		this(clazz, null);
	}

	/**
	 * 构造一个新的切点, 匹配给定类中给定方法下面的所有调用. 如果没有给出方法名称, 匹配给定类下面的所有控制流.
	 * 
	 * @param clazz the clazz
	 * @param methodName 方法的名称 (may be {@code null})
	 */
	public ControlFlowPointcut(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		this.clazz = clazz;
		this.methodName = methodName;
	}


	/**
	 * 子类可以覆盖它以进行更大的过滤 (和执行).
	 */
	@Override
	public boolean matches(Class<?> clazz) {
		return true;
	}

	/**
	 * 如果可以过滤掉一些候选类，则子类可以覆盖它.
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

	@Override
	public boolean isRuntime() {
		return true;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		this.evaluations++;

		for (StackTraceElement element : new Throwable().getStackTrace()) {
			if (element.getClassName().equals(this.clazz.getName()) &&
					(this.methodName == null || element.getMethodName().equals(this.methodName))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 为了优化，知道我们已经触发了多少次是有用的.
	 */
	public int getEvaluations() {
		return this.evaluations;
	}


	@Override
	public ClassFilter getClassFilter() {
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControlFlowPointcut)) {
			return false;
		}
		ControlFlowPointcut that = (ControlFlowPointcut) other;
		return (this.clazz.equals(that.clazz)) && ObjectUtils.nullSafeEquals(that.methodName, this.methodName);
	}

	@Override
	public int hashCode() {
		int code = this.clazz.hashCode();
		if (this.methodName != null) {
			code = 37 * code + this.methodName.hashCode();
		}
		return code;
	}

}
