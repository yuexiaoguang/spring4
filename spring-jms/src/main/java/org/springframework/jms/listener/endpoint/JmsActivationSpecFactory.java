package org.springframework.jms.listener.endpoint;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

/**
 * 用于基于已配置的{@link JmsActivationSpecConfig}对象创建JCA 1.5 ActivationSpec对象的策略接口.
 *
 * <p>JCA 1.5 ActivationSpec对象通常是JavaBeans, 但遗憾的是特定于提供者.
 * 此策略接口允许插入任何基于JCA的JMS提供者, 根据常见的JMS配置设置创建相应的ActivationSpec对象.
 */
public interface JmsActivationSpecFactory {

	/**
	 * 根据给定的{@link JmsActivationSpecConfig}对象创建JCA 1.5 ActivationSpec对象.
	 * 
	 * @param adapter 使用的ResourceAdapter
	 * @param config 保存常见的JMS设置的配置的对象
	 * 
	 * @return 特定于提供者的JCA ActivationSpec对象, 表示相同的设置
	 */
	ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config);

}
