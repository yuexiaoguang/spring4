package org.springframework.aop.aspectj;

import java.io.Serializable;

import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * 由指定的单例对象支持的{@link AspectInstanceFactory}的实现, 为每个{@link #getAspectInstance()}调用返回相同的实例.
 */
@SuppressWarnings("serial")
public class SingletonAspectInstanceFactory implements AspectInstanceFactory, Serializable {

	private final Object aspectInstance;


	/**
	 * @param aspectInstance 单例切面实例
	 */
	public SingletonAspectInstanceFactory(Object aspectInstance) {
		Assert.notNull(aspectInstance, "Aspect instance must not be null");
		this.aspectInstance = aspectInstance;
	}


	@Override
	public final Object getAspectInstance() {
		return this.aspectInstance;
	}

	@Override
	public ClassLoader getAspectClassLoader() {
		return this.aspectInstance.getClass().getClassLoader();
	}

	/**
	 * 确定此工厂的切面实例的顺序, 
	 * 通过实现{@link org.springframework.core.Ordered}接口, 表示特定于实例的顺序, 或一个回调顺序.
	 */
	@Override
	public int getOrder() {
		if (this.aspectInstance instanceof Ordered) {
			return ((Ordered) this.aspectInstance).getOrder();
		}
		return getOrderForAspectClass(this.aspectInstance.getClass());
	}

	/**
	 * 在切面实例没有通过实现{@link org.springframework.core.Ordered}接口, 表示特定于实例的顺序的情况下, 确定回调顺序.
	 * <p>默认实现只返回 {@code Ordered.LOWEST_PRECEDENCE}.
	 * 
	 * @param aspectClass 切面类
	 */
	protected int getOrderForAspectClass(Class<?> aspectClass) {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
