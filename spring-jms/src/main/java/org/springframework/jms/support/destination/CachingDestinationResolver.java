package org.springframework.jms.support.destination;

/**
 * DestinationResolver接口的扩展, 公开了清除缓存的方法.
 */
public interface CachingDestinationResolver extends DestinationResolver {

	/**
	 * 从缓存中删除具有给定名称的目标 (如果首先由此解析器缓存).
	 * <p>如果对指定目标的访问失败, 则调用此方法, 假设JMS Destination对象可能已变为无效.
	 * 
	 * @param destinationName 目标的名称
	 */
	void removeFromCache(String destinationName);

	/**
	 * 清除整个目标缓存.
	 * <p>在一般JMS提供者失败的情况下调用.
	 */
	void clearCache();

}
