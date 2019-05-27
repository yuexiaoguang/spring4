package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * 使{@link MetadataAwareAspectInstanceFactory}仅实例化一次的装饰器.
 */
@SuppressWarnings("serial")
public class LazySingletonAspectInstanceFactoryDecorator implements MetadataAwareAspectInstanceFactory, Serializable {

	private final MetadataAwareAspectInstanceFactory maaif;

	private volatile Object materialized;


	/**
	 * 为给定的AspectInstanceFactory创建一个新的延迟初始化的装饰器.
	 * 
	 * @param maaif 要装饰的MetadataAwareAspectInstanceFactory
	 */
	public LazySingletonAspectInstanceFactoryDecorator(MetadataAwareAspectInstanceFactory maaif) {
		Assert.notNull(maaif, "AspectInstanceFactory must not be null");
		this.maaif = maaif;
	}


	@Override
	public Object getAspectInstance() {
		if (this.materialized == null) {
			Object mutex = this.maaif.getAspectCreationMutex();
			if (mutex == null) {
				this.materialized = this.maaif.getAspectInstance();
			}
			else {
				synchronized (mutex) {
					if (this.materialized == null) {
						this.materialized = this.maaif.getAspectInstance();
					}
				}
			}
		}
		return this.materialized;
	}

	public boolean isMaterialized() {
		return (this.materialized != null);
	}

	@Override
	public ClassLoader getAspectClassLoader() {
		return this.maaif.getAspectClassLoader();
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.maaif.getAspectMetadata();
	}

	@Override
	public Object getAspectCreationMutex() {
		return this.maaif.getAspectCreationMutex();
	}

	@Override
	public int getOrder() {
		return this.maaif.getOrder();
	}


	@Override
	public String toString() {
		return "LazySingletonAspectInstanceFactoryDecorator: decorating " + this.maaif;
	}

}
