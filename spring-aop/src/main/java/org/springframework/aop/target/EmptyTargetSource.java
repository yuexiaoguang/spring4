package org.springframework.aop.target;

import java.io.Serializable;

import org.springframework.aop.TargetSource;
import org.springframework.util.ObjectUtils;

/**
 * 当没有目标时, 默认的 {@code TargetSource} (或者只是已知的目标类), 而且行为仅由接口和切面提供.
 */
public class EmptyTargetSource implements TargetSource, Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = 3680494563553489691L;


	//---------------------------------------------------------------------
	// Static factory methods
	//---------------------------------------------------------------------

	/**
	 * {@link EmptyTargetSource}的单例.
	 */
	public static final EmptyTargetSource INSTANCE = new EmptyTargetSource(null, true);


	/**
	 * 返回给定目标类的EmptyTargetSource.
	 * 
	 * @param targetClass 目标Class (may be {@code null})
	 */
	public static EmptyTargetSource forClass(Class<?> targetClass) {
		return forClass(targetClass, true);
	}

	/**
	 * 返回给定目标类的EmptyTargetSource.
	 * 
	 * @param targetClass 目标Class (may be {@code null})
	 * @param isStatic 是否应将TargetSource标记为静态
	 */
	public static EmptyTargetSource forClass(Class<?> targetClass, boolean isStatic) {
		return (targetClass == null && isStatic ? INSTANCE : new EmptyTargetSource(targetClass, isStatic));
	}


	//---------------------------------------------------------------------
	// Instance implementation
	//---------------------------------------------------------------------

	private final Class<?> targetClass;

	private final boolean isStatic;


	/**
	 * @param targetClass 要公开的目标类 (may be {@code null})
	 * @param isStatic 是否应将TargetSource标记为静态
	 */
	private EmptyTargetSource(Class<?> targetClass, boolean isStatic) {
		this.targetClass = targetClass;
		this.isStatic = isStatic;
	}

	/**
	 * 始终返回指定的目标类, 或{@code null}.
	 */
	@Override
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * 总是返回 {@code true}.
	 */
	@Override
	public boolean isStatic() {
		return this.isStatic;
	}

	/**
	 * 总是返回 {@code null}.
	 */
	@Override
	public Object getTarget() {
		return null;
	}

	/**
	 * 什么都不释放.
	 */
	@Override
	public void releaseTarget(Object target) {
	}


	/**
	 * 如果没有目标类, 则返回反序列化的规范实例, 从而保护单例模式.
	 */
	private Object readResolve() {
		return (this.targetClass == null && this.isStatic ? INSTANCE : this);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EmptyTargetSource)) {
			return false;
		}
		EmptyTargetSource otherTs = (EmptyTargetSource) other;
		return (ObjectUtils.nullSafeEquals(this.targetClass, otherTs.targetClass) && this.isStatic == otherTs.isStatic);
	}

	@Override
	public int hashCode() {
		return EmptyTargetSource.class.hashCode() * 13 + ObjectUtils.nullSafeHashCode(this.targetClass);
	}

	@Override
	public String toString() {
		return "EmptyTargetSource: " +
				(this.targetClass != null ? "target class [" + this.targetClass.getName() + "]" : "no target class") +
				", " + (this.isStatic ? "static" : "dynamic");
	}

}
