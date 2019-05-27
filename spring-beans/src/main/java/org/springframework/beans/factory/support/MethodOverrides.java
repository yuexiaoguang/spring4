package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 一组方法覆盖, 确定Spring IoC容器在运行时将覆盖托管对象上的哪些方法.
 *
 * <p>目前支持的{@link MethodOverride}变体是 {@link LookupOverride}和{@link ReplaceOverride}.
 */
public class MethodOverrides {

	private final Set<MethodOverride> overrides =
			Collections.synchronizedSet(new LinkedHashSet<MethodOverride>(0));

	private volatile boolean modified = false;


	public MethodOverrides() {
	}

	/**
	 * 深克隆构造函数.
	 */
	public MethodOverrides(MethodOverrides other) {
		addOverrides(other);
	}


	/**
	 * 将所有给定的MethodOverride复制到此对象中.
	 */
	public void addOverrides(MethodOverrides other) {
		if (other != null) {
			this.modified = true;
			this.overrides.addAll(other.overrides);
		}
	}

	public void addOverride(MethodOverride override) {
		this.modified = true;
		this.overrides.add(override);
	}

	/**
	 * 返回此对象包含的所有方法覆盖.
	 */
	public Set<MethodOverride> getOverrides() {
		this.modified = true;
		return this.overrides;
	}

	public boolean isEmpty() {
		return (!this.modified || this.overrides.isEmpty());
	}

	/**
	 * 返回给定方法的覆盖.
	 * 
	 * @param method 要检查覆盖的方法
	 * 
	 * @return 方法覆盖, 或{@code null}
	 */
	public MethodOverride getOverride(Method method) {
		if (!this.modified) {
			return null;
		}
		synchronized (this.overrides) {
			MethodOverride match = null;
			for (MethodOverride candidate : this.overrides) {
				if (candidate.matches(method)) {
					match = candidate;
				}
			}
			return match;
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverrides)) {
			return false;
		}
		MethodOverrides that = (MethodOverrides) other;
		return this.overrides.equals(that.overrides);

	}

	@Override
	public int hashCode() {
		return this.overrides.hashCode();
	}

}
