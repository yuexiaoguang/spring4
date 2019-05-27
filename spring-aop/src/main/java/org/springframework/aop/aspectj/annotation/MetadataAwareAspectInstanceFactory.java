package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.AspectInstanceFactory;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory}的子接口, 
 * 返回与AspectJ注解的类关联的{@link AspectMetadata}.
 *
 * <p>理想的情况下, AspectInstanceFactory将包含此方法本身, 但是因为AspectMetadata仅使用Java-5 {@link org.aspectj.lang.reflect.AjType},
 * 需要拆分这个子接口.
 */
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

	/**
	 * 返回此工厂切面的AspectJ AspectMetadata.
	 * 
	 * @return 切面元数据
	 */
	AspectMetadata getAspectMetadata();

	/**
	 * 为这个工厂返回最好的创建互斥锁.
	 * 
	 * @return 互斥对象 (可能是{@code null}, 因为没有使用互斥锁)
	 * @since 4.3
	 */
	Object getAspectCreationMutex();

}
