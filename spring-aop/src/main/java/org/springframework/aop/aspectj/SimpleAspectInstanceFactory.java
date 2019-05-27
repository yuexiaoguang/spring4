package org.springframework.aop.aspectj;

import org.springframework.aop.framework.AopConfigException;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * 为每个{@link #getAspectInstance()}调用创建指定切面类的新实例的{@link AspectInstanceFactory}实现.
 */
public class SimpleAspectInstanceFactory implements AspectInstanceFactory {

	private final Class<?> aspectClass;


	/**
	 * @param aspectClass 切面类
	 */
	public SimpleAspectInstanceFactory(Class<?> aspectClass) {
		Assert.notNull(aspectClass, "Aspect class must not be null");
		this.aspectClass = aspectClass;
	}


	/**
	 * 返回指定的切面类 (never {@code null}).
	 */
	public final Class<?> getAspectClass() {
		return this.aspectClass;
	}

	@Override
	public final Object getAspectInstance() {
		try {
			return this.aspectClass.newInstance();
		}
		catch (InstantiationException ex) {
			throw new AopConfigException(
					"Unable to instantiate aspect class: " + this.aspectClass.getName(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new AopConfigException(
					"Could not access aspect constructor: " + this.aspectClass.getName(), ex);
		}
	}

	@Override
	public ClassLoader getAspectClassLoader() {
		return this.aspectClass.getClassLoader();
	}

	/**
	 * 确定此工厂的切面实例的顺序, 
	 * 通过实现{@link org.springframework.core.Ordered}接口, 表示特定于实例的顺序, 或一个回调顺序.
	 */
	@Override
	public int getOrder() {
		return getOrderForAspectClass(this.aspectClass);
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
