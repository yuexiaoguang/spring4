package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.util.Assert;

/**
 * 用于编辑{@link MethodMatcher MethodMatchers}的静态方法.
 *
 * <p>可以静态地评估MethodMatcher (基于方法和目标类) 或需要动态进一步评估 (基于方法调用时的参数).
 */
public abstract class MethodMatchers {

	/**
	 * 并集.
	 * 
	 * @param mm1 the first MethodMatcher
	 * @param mm2 the second MethodMatcher
	 * 
	 * @return 并集的MethodMatcher
	 */
	public static MethodMatcher union(MethodMatcher mm1, MethodMatcher mm2) {
		return new UnionMethodMatcher(mm1, mm2);
	}

	/**
	 * Match all methods that <i>either</i> (or both) of the given MethodMatchers matches.
	 * 
	 * @param mm1 the first MethodMatcher
	 * @param cf1 the corresponding ClassFilter for the first MethodMatcher
	 * @param mm2 the second MethodMatcher
	 * @param cf2 the corresponding ClassFilter for the second MethodMatcher
	 * 
	 * @return a distinct MethodMatcher that matches all methods that either
	 * of the given MethodMatchers matches
	 */
	static MethodMatcher union(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
		return new ClassFilterAwareUnionMethodMatcher(mm1, cf1, mm2, cf2);
	}

	/**
	 * Match all methods that <i>both</i> of the given MethodMatchers match.
	 * 
	 * @param mm1 the first MethodMatcher
	 * @param mm2 the second MethodMatcher
	 * 
	 * @return a distinct MethodMatcher that matches all methods that both
	 * of the given MethodMatchers match
	 */
	public static MethodMatcher intersection(MethodMatcher mm1, MethodMatcher mm2) {
		return new IntersectionMethodMatcher(mm1, mm2);
	}

	/**
	 * 将给定的MethodMatcher应用于给定的Method, 支持一个{@link org.springframework.aop.IntroductionAwareMethodMatcher}.
	 * 
	 * @param mm 要应用的MethodMatcher (可能是 IntroductionAwareMethodMatcher)
	 * @param method 候选方法
	 * @param targetClass 目标类 (可能是 {@code null}, 在这种情况下, 候选类必须被视为方法的声明类)
	 * @param hasIntroductions {@code true} 如果我们所代表的对象是一个或多个引入的主题; 否则{@code false}
	 * 
	 * @return whether or not this method matches statically
	 */
	public static boolean matches(MethodMatcher mm, Method method, Class<?> targetClass, boolean hasIntroductions) {
		Assert.notNull(mm, "MethodMatcher must not be null");
		return ((mm instanceof IntroductionAwareMethodMatcher &&
				((IntroductionAwareMethodMatcher) mm).matches(method, targetClass, hasIntroductions)) ||
				mm.matches(method, targetClass));
	}


	/**
	 * MethodMatcher implementation for a union of two given MethodMatchers.
	 */
	@SuppressWarnings("serial")
	private static class UnionMethodMatcher implements IntroductionAwareMethodMatcher, Serializable {

		private final MethodMatcher mm1;

		private final MethodMatcher mm2;

		public UnionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			Assert.notNull(mm1, "First MethodMatcher must not be null");
			Assert.notNull(mm2, "Second MethodMatcher must not be null");
			this.mm1 = mm1;
			this.mm2 = mm2;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return (matchesClass1(targetClass) && MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions)) ||
					(matchesClass2(targetClass) && MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions));
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (matchesClass1(targetClass) && this.mm1.matches(method, targetClass)) ||
					(matchesClass2(targetClass) && this.mm2.matches(method, targetClass));
		}

		protected boolean matchesClass1(Class<?> targetClass) {
			return true;
		}

		protected boolean matchesClass2(Class<?> targetClass) {
			return true;
		}

		@Override
		public boolean isRuntime() {
			return this.mm1.isRuntime() || this.mm2.isRuntime();
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			return this.mm1.matches(method, targetClass, args) || this.mm2.matches(method, targetClass, args);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof UnionMethodMatcher)) {
				return false;
			}
			UnionMethodMatcher that = (UnionMethodMatcher) obj;
			return (this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2));
		}

		@Override
		public int hashCode() {
			int hashCode = 17;
			hashCode = 37 * hashCode + this.mm1.hashCode();
			hashCode = 37 * hashCode + this.mm2.hashCode();
			return hashCode;
		}
	}


	/**
	 * MethodMatcher implementation for a union of two given MethodMatchers,
	 * supporting an associated ClassFilter per MethodMatcher.
	 */
	@SuppressWarnings("serial")
	private static class ClassFilterAwareUnionMethodMatcher extends UnionMethodMatcher {

		private final ClassFilter cf1;

		private final ClassFilter cf2;

		public ClassFilterAwareUnionMethodMatcher(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
			super(mm1, mm2);
			this.cf1 = cf1;
			this.cf2 = cf2;
		}

		@Override
		protected boolean matchesClass1(Class<?> targetClass) {
			return this.cf1.matches(targetClass);
		}

		@Override
		protected boolean matchesClass2(Class<?> targetClass) {
			return this.cf2.matches(targetClass);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			ClassFilter otherCf1 = ClassFilter.TRUE;
			ClassFilter otherCf2 = ClassFilter.TRUE;
			if (other instanceof ClassFilterAwareUnionMethodMatcher) {
				ClassFilterAwareUnionMethodMatcher cfa = (ClassFilterAwareUnionMethodMatcher) other;
				otherCf1 = cfa.cf1;
				otherCf2 = cfa.cf2;
			}
			return (this.cf1.equals(otherCf1) && this.cf2.equals(otherCf2));
		}
	}


	/**
	 * MethodMatcher implementation for an intersection of two given MethodMatchers.
	 */
	@SuppressWarnings("serial")
	private static class IntersectionMethodMatcher implements IntroductionAwareMethodMatcher, Serializable {

		private final MethodMatcher mm1;

		private final MethodMatcher mm2;

		public IntersectionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			Assert.notNull(mm1, "First MethodMatcher must not be null");
			Assert.notNull(mm2, "Second MethodMatcher must not be null");
			this.mm1 = mm1;
			this.mm2 = mm2;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions) &&
					MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return this.mm1.matches(method, targetClass) && this.mm2.matches(method, targetClass);
		}

		@Override
		public boolean isRuntime() {
			return this.mm1.isRuntime() || this.mm2.isRuntime();
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			// 因为动态交叉可以由静态和动态部分组成, 必须避免在动态匹配器上调用3-arg匹配方法, 因为它可能是一个不受支持的操作.
			boolean aMatches = this.mm1.isRuntime() ?
					this.mm1.matches(method, targetClass, args) : this.mm1.matches(method, targetClass);
			boolean bMatches = this.mm2.isRuntime() ?
					this.mm2.matches(method, targetClass, args) : this.mm2.matches(method, targetClass);
			return aMatches && bMatches;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof IntersectionMethodMatcher)) {
				return false;
			}
			IntersectionMethodMatcher that = (IntersectionMethodMatcher) other;
			return (this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2));
		}

		@Override
		public int hashCode() {
			int hashCode = 17;
			hashCode = 37 * hashCode + this.mm1.hashCode();
			hashCode = 37 * hashCode + this.mm2.hashCode();
			return hashCode;
		}
	}

}
