package org.springframework.util;

import java.util.Collection;
import java.util.Collections;

/**
 * 一个简单的实例过滤器, 用于检查给定实例基于includes和exclude元素的集合是否匹配.
 *
 * <p>子类可能希望覆盖{@link #match(Object, Object)}以提供自定义匹配算法.
 */
public class InstanceFilter<T> {

	private final Collection<? extends T> includes;

	private final Collection<? extends T> excludes;

	private final boolean matchIfEmpty;


	/**
	 * 基于包含/排除集合创建新实例.
	 * <p>如果某个元素与包含列表中的某个元素"匹配", 并且与排除列表中的某个元素不匹配, 则该元素将匹配.
	 * <p>子类可以重新定义匹配的含义. 默认情况, 如果元素根据{@link Object#equals(Object)}相等, 则元素与另一个元素匹配.
	 * <p>如果两个集合都为空, 则{@code matchIfEmpty}定义元素是否匹配.
	 * 
	 * @param includes 包含的集合
	 * @param excludes 排除的集合
	 * @param matchIfEmpty 如果包含和排除集合都为空, 则匹配结果
	 */
	public InstanceFilter(Collection<? extends T> includes,
			Collection<? extends T> excludes, boolean matchIfEmpty) {

		this.includes = (includes != null ? includes : Collections.<T>emptyList());
		this.excludes = (excludes != null ? excludes : Collections.<T>emptyList());
		this.matchIfEmpty = matchIfEmpty;
	}


	/**
	 * 确定指定的{code instance}是否与此过滤器匹配.
	 */
	public boolean match(T instance) {
		Assert.notNull(instance, "Instance to match must not be null");

		boolean includesSet = !this.includes.isEmpty();
		boolean excludesSet = !this.excludes.isEmpty();
		if (!includesSet && !excludesSet) {
			return this.matchIfEmpty;
		}

		boolean matchIncludes = match(instance, this.includes);
		boolean matchExcludes = match(instance, this.excludes);
		if (!includesSet) {
			return !matchExcludes;
		}
		if (!excludesSet) {
			return matchIncludes;
		}
		return matchIncludes && !matchExcludes;
	}

	/**
	 * 确定指定的{@code instance}是否等于指定的{@code candidate}.
	 * 
	 * @param instance 要处理的实例
	 * @param candidate 由此过滤器定义的候选者
	 * 
	 * @return {@code true}如果实例与候选者匹配
	 */
	protected boolean match(T instance, T candidate) {
		return instance.equals(candidate);
	}

	/**
	 * 确定指定的{@code instance}是否与其中一个候选匹配.
	 * <p>如果候选集合是 {@code null}, 则返回{@code false}.
	 * 
	 * @param instance 要检查的实例
	 * @param candidates 候选者
	 * 
	 * @return {@code true}如果实例匹配或者候选集合为空
	 */
	protected boolean match(T instance, Collection<? extends T> candidates) {
		for (T candidate : candidates) {
			if (match(instance, candidate)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(": includes=").append(this.includes);
		sb.append(", excludes=").append(this.excludes);
		sb.append(", matchIfEmpty=").append(this.matchIfEmpty);
		return sb.toString();
	}

}
