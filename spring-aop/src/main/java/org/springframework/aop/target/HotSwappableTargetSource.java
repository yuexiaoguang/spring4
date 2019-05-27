package org.springframework.aop.target;

import java.io.Serializable;

import org.springframework.aop.TargetSource;
import org.springframework.util.Assert;

/**
 * 缓存本地目标对象的{@link org.springframework.aop.TargetSource}实现, 但允许在应用程序运行时交换目标.
 *
 * <p>如果在Spring IoC容器中配置此类的对象, 使用构造函数注入.
 *
 * <p>如果目标可序列化，则此TargetSource是可序列化的.
 */
public class HotSwappableTargetSource implements TargetSource, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = 7497929212653839187L;


	/** 当前目标对象 */
	private Object target;


	/**
	 * @param initialTarget 初始目标对象
	 */
	public HotSwappableTargetSource(Object initialTarget) {
		Assert.notNull(initialTarget, "Target object must not be null");
		this.target = initialTarget;
	}


	/**
	 * 返回当前目标对象的类型.
	 * <p>返回的类型通常应该在所有目标对象上保持不变.
	 */
	@Override
	public synchronized Class<?> getTargetClass() {
		return this.target.getClass();
	}

	@Override
	public final boolean isStatic() {
		return false;
	}

	@Override
	public synchronized Object getTarget() {
		return this.target;
	}

	@Override
	public void releaseTarget(Object target) {
		// nothing to do
	}


	/**
	 * 交换目标, 返回旧的目标对象.
	 * 
	 * @param newTarget 新的目标对象
	 * 
	 * @return 旧的目标对象
	 * @throws IllegalArgumentException 如果新的目标对象无效
	 */
	public synchronized Object swap(Object newTarget) throws IllegalArgumentException {
		Assert.notNull(newTarget, "Target object must not be null");
		Object old = this.target;
		this.target = newTarget;
		return old;
	}


	/**
	 * 如果当前目标对象相等，则两个HotSwappableTargetSources相等.
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof HotSwappableTargetSource &&
				this.target.equals(((HotSwappableTargetSource) other).target)));
	}

	@Override
	public int hashCode() {
		return HotSwappableTargetSource.class.hashCode();
	}

	@Override
	public String toString() {
		return "HotSwappableTargetSource for target: " + this.target;
	}
}
