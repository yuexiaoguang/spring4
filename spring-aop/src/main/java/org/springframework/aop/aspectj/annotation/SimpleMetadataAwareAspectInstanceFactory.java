package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.SimpleAspectInstanceFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;

/**
 * {@link MetadataAwareAspectInstanceFactory}的实现, 为每个{@link #getAspectInstance()}调用创建指定切面类的新实例.
 */
public class SimpleMetadataAwareAspectInstanceFactory extends SimpleAspectInstanceFactory
		implements MetadataAwareAspectInstanceFactory {

	private final AspectMetadata metadata;


	/**
	 * @param aspectClass 切面类
	 * @param aspectName 切面名称
	 */
	public SimpleMetadataAwareAspectInstanceFactory(Class<?> aspectClass, String aspectName) {
		super(aspectClass);
		this.metadata = new AspectMetadata(aspectClass, aspectName);
	}


	@Override
	public final AspectMetadata getAspectMetadata() {
		return this.metadata;
	}

	@Override
	public Object getAspectCreationMutex() {
		return this;
	}

	@Override
	protected int getOrderForAspectClass(Class<?> aspectClass) {
		return OrderUtils.getOrder(aspectClass, Ordered.LOWEST_PRECEDENCE);
	}
}
