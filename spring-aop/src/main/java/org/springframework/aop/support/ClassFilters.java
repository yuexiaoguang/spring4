package org.springframework.aop.support;

import java.io.Serializable;

import org.springframework.aop.ClassFilter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 组合 {@link ClassFilter ClassFilters}的静态工具方法.
 */
public abstract class ClassFilters {

	/**
	 * 匹配给定ClassFilters中的任何一个（或两个都匹配）.
	 * 
	 * @param cf1 第一个ClassFilter
	 * @param cf2 第二个ClassFilter
	 * 
	 * @return 一个独特的ClassFilter，它匹配任何一个给定ClassFilter匹配的类
	 */
	public static ClassFilter union(ClassFilter cf1, ClassFilter cf2) {
		Assert.notNull(cf1, "First ClassFilter must not be null");
		Assert.notNull(cf2, "Second ClassFilter must not be null");
		return new UnionClassFilter(new ClassFilter[] {cf1, cf2});
	}

	/**
	 * 匹配给定ClassFilters中的任何一个（或都匹配）.
	 * 
	 * @param classFilters 要匹配的ClassFilters
	 * 
	 * @return 一个独特的ClassFilter，它匹配任何一个给定ClassFilter匹配的类
	 */
	public static ClassFilter union(ClassFilter[] classFilters) {
		Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
		return new UnionClassFilter(classFilters);
	}

	/**
	 * 匹配所有给定的ClassFilters.
	 * 
	 * @param cf1 第一个ClassFilter
	 * @param cf2 第二个ClassFilter
	 * 
	 * @return 一个独特的ClassFilter，它匹配给定ClassFilter都匹配的类
	 */
	public static ClassFilter intersection(ClassFilter cf1, ClassFilter cf2) {
		Assert.notNull(cf1, "First ClassFilter must not be null");
		Assert.notNull(cf2, "Second ClassFilter must not be null");
		return new IntersectionClassFilter(new ClassFilter[] {cf1, cf2});
	}

	/**
	 * 匹配所有给定的ClassFilters.
	 * 
	 * @param classFilters 要匹配的ClassFilters
	 * 
	 * @return 一个独特的ClassFilter，它匹配给定ClassFilter都匹配的类
	 */
	public static ClassFilter intersection(ClassFilter[] classFilters) {
		Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
		return new IntersectionClassFilter(classFilters);
	}


	/**
	 * 给定ClassFilters的并集的ClassFilter实现.
	 */
	@SuppressWarnings("serial")
	private static class UnionClassFilter implements ClassFilter, Serializable {

		private ClassFilter[] filters;

		public UnionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			for (ClassFilter filter : this.filters) {
				if (filter.matches(clazz)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof UnionClassFilter &&
					ObjectUtils.nullSafeEquals(this.filters, ((UnionClassFilter) other).filters)));
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.filters);
		}
	}


	/**
	 * 给定ClassFilters的交集的ClassFilter实现.
	 */
	@SuppressWarnings("serial")
	private static class IntersectionClassFilter implements ClassFilter, Serializable {

		private ClassFilter[] filters;

		public IntersectionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			for (ClassFilter filter : this.filters) {
				if (!filter.matches(clazz)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof IntersectionClassFilter &&
					ObjectUtils.nullSafeEquals(this.filters, ((IntersectionClassFilter) other).filters)));
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.filters);
		}
	}

}
