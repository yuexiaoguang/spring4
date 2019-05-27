package org.springframework.aop.support;

import java.io.Serializable;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;

/**
 * 用于构建切点. 所有的方法返回ComposablePointcut, 所以我们可以使用简洁的习语:
 *
 * {@code
 * Pointcut pc = new ComposablePointcut().union(classFilter).intersection(methodMatcher).intersection(pointcut);
 * }
 */
public class ComposablePointcut implements Pointcut, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -2743223737633663832L;

	private ClassFilter classFilter;

	private MethodMatcher methodMatcher;


	public ComposablePointcut() {
		this.classFilter = ClassFilter.TRUE;
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * @param pointcut 原始Pointcut
	 */
	public ComposablePointcut(Pointcut pointcut) {
		Assert.notNull(pointcut, "Pointcut must not be null");
		this.classFilter = pointcut.getClassFilter();
		this.methodMatcher = pointcut.getMethodMatcher();
	}

	/**
	 * @param classFilter 要使用的ClassFilter
	 */
	public ComposablePointcut(ClassFilter classFilter) {
		Assert.notNull(classFilter, "ClassFilter must not be null");
		this.classFilter = classFilter;
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * @param methodMatcher 要使用的MethodMatcher
	 */
	public ComposablePointcut(MethodMatcher methodMatcher) {
		Assert.notNull(methodMatcher, "MethodMatcher must not be null");
		this.classFilter = ClassFilter.TRUE;
		this.methodMatcher = methodMatcher;
	}

	/**
	 * @param classFilter 要使用的ClassFilter
	 * @param methodMatcher 要使用的MethodMatcher
	 */
	public ComposablePointcut(ClassFilter classFilter, MethodMatcher methodMatcher) {
		Assert.notNull(classFilter, "ClassFilter must not be null");
		Assert.notNull(methodMatcher, "MethodMatcher must not be null");
		this.classFilter = classFilter;
		this.methodMatcher = methodMatcher;
	}


	/**
	 * 应用一个并集.
	 * 
	 * @param other 要联合的ClassFilter
	 * 
	 * @return 组合切点 (对于调用链)
	 */
	public ComposablePointcut union(ClassFilter other) {
		this.classFilter = ClassFilters.union(this.classFilter, other);
		return this;
	}

	/**
	 * 应用一个交集.
	 * 
	 * @param other 要交的ClassFilter
	 * 
	 * @return 组合切点 (对于调用链)
	 */
	public ComposablePointcut intersection(ClassFilter other) {
		this.classFilter = ClassFilters.intersection(this.classFilter, other);
		return this;
	}

	/**
	 * 应用一个并集.
	 * 
	 * @param other 要并的MethodMatcher
	 * 
	 * @return 组合切点 (对于调用链)
	 */
	public ComposablePointcut union(MethodMatcher other) {
		this.methodMatcher = MethodMatchers.union(this.methodMatcher, other);
		return this;
	}

	/**
	 * 应用一个交集.
	 * 
	 * @param other 要交的 MethodMatcher
	 * 
	 * @return 组合切点 (对于调用链)
	 */
	public ComposablePointcut intersection(MethodMatcher other) {
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, other);
		return this;
	}

	/**
	 * 应用一个并集.
	 * <p>对于一个要并的 Pointcut, 只有当原始的ClassFilter（来自原始的Pointcut）匹配时，方法才会匹配.
	 * 来自不同切点的MethodMatchers和ClassFilters永远不会相互交错.
	 * 
	 * @param other 要并的Pointcut
	 * 
	 * @return 组合切点 (对于调用链)
	 */
	public ComposablePointcut union(Pointcut other) {
		this.methodMatcher = MethodMatchers.union(
				this.methodMatcher, this.classFilter, other.getMethodMatcher(), other.getClassFilter());
		this.classFilter = ClassFilters.union(this.classFilter, other.getClassFilter());
		return this;
	}

	/**
	 * 应用一个交集.
	 * 
	 * @param other 要交的Pointcut
	 * 
	 * @return 组合切点 (对于调用链)
	 */
	public ComposablePointcut intersection(Pointcut other) {
		this.classFilter = ClassFilters.intersection(this.classFilter, other.getClassFilter());
		this.methodMatcher = MethodMatchers.intersection(this.methodMatcher, other.getMethodMatcher());
		return this;
	}


	@Override
	public ClassFilter getClassFilter() {
		return this.classFilter;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this.methodMatcher;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ComposablePointcut)) {
			return false;
		}
		ComposablePointcut otherPointcut = (ComposablePointcut) other;
		return (this.classFilter.equals(otherPointcut.classFilter) &&
				this.methodMatcher.equals(otherPointcut.methodMatcher));
	}

	@Override
	public int hashCode() {
		return this.classFilter.hashCode() * 37 + this.methodMatcher.hashCode();
	}

	@Override
	public String toString() {
		return "ComposablePointcut: " + this.classFilter + ", " +this.methodMatcher;
	}

}
