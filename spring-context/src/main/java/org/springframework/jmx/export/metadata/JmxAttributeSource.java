package org.springframework.jmx.export.metadata;

import java.lang.reflect.Method;

/**
 * {@code MetadataMBeanInfoAssembler}使用的接口, 从托管资源的类中读取source级元数据.
 */
public interface JmxAttributeSource {

	/**
	 * 如果提供的{@code Class}具有适当的元数据, 则实现应返回{@code ManagedResource}的实例.
	 * 否则应该返回{@code null}.
	 * 
	 * @param clazz 从中读取属性数据的类
	 * 
	 * @return 属性, 或{@code null}如果未找到
	 * @throws InvalidMetadataException 无效的属性
	 */
	ManagedResource getManagedResource(Class<?> clazz) throws InvalidMetadataException;

	/**
	 * 如果提供的{@code Method}具有相应的元数据, 则实现应返回{@code ManagedAttribute}的实例.
	 * 否则应该返回{@code null}.
	 * 
	 * @param method 从中读取属性数据的方法
	 * 
	 * @return 属性, 或{@code null}如果未找到
	 * @throws InvalidMetadataException 无效的属性
	 */
	ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException;

	/**
	 * 如果提供的{@code Method}具有相应的元数据, 则实现应返回{@code ManagedMetric}的实例.
	 * 否则应该返回{@code null}.
	 * 
	 * @param method 从中读取属性数据的方法
	 * 
	 * @return 指标, 或{@code null}如果未找到
	 * @throws InvalidMetadataException 无效的属性
	 */
	ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException;

	/**
	 * 如果提供的{@code Method}具有相应的元数据, 则实现应返回{@code ManagedOperation}的实例.
	 * 否则应该返回{@code null}.
	 * 
	 * @param method 从中读取属性数据的方法
	 * 
	 * @return 属性, 或{@code null}如果未找到
	 * @throws InvalidMetadataException 无效的属性
	 */
	ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException;

	/**
	 * 如果提供的{@code Method}具有相应的元数据, 则实现应返回{@code ManagedOperationParameter}数组.
	 * 否则应该返回一个空数组, 如果没有找到元数据.
	 * 
	 * @param method 从中读取元数据的{@code Method}
	 * 
	 * @return 参数信息.
	 * @throws InvalidMetadataException 无效的属性
	 */
	ManagedOperationParameter[] getManagedOperationParameters(Method method) throws InvalidMetadataException;

	/**
	 * 如果提供的{@code Class}具有相应的元数据, 则实现应返回{@link ManagedNotification ManagedNotifications}数组.
	 * 否则应该返回一个空数组.
	 * 
	 * @param clazz 从中读取元数据的{@code Class}
	 * 
	 * @return 通知信息
	 * @throws InvalidMetadataException 无效的元数据
	 */
	ManagedNotification[] getManagedNotifications(Class<?> clazz) throws InvalidMetadataException;

}
