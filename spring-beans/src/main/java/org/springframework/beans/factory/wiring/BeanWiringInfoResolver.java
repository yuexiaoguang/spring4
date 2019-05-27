package org.springframework.beans.factory.wiring;

/**
 * 在给定新实例化的bean对象的情况下, 由对象实现的策略接口可以解析bean名称信息.
 * 在此接口上调用{@link #resolveWiringInfo}方法, 将由相关具体切面的AspectJ切点驱动.
 *
 * <p>元数据解析策略可以插拔.
 * 一个很好的默认值是{@link ClassNameBeanWiringInfoResolver}, 它使用完全限定的类名作为bean名称.
 */
public interface BeanWiringInfoResolver {

	/**
	 * 解析给定bean实例的BeanWiringInfo.
	 * 
	 * @param beanInstance 要解析信息的bean实例
	 * 
	 * @return BeanWiringInfo, 或{@code null}
	 */
	BeanWiringInfo resolveWiringInfo(Object beanInstance);

}
