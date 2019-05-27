package org.springframework.beans.factory;

/**
 * 需要在{@link BeanFactory}设置所有属性后做出反应的bea​​n需要实现的接口:
 * e.g. 执行自定义初始化, 或仅仅检查是否已设置所有强制性属性.
 *
 * <p>实现{@code InitializingBean}的替代方法是指定自定义init方法, 例如在XML bean定义中.
 * 有关所有Bean生命周期方法的列表, see the {@link BeanFactory BeanFactory javadocs}.
 */
public interface InitializingBean {

	/**
	 * 在设置所有bean属性并满足{@link BeanFactoryAware}, {@code ApplicationContextAware}等之后,
	 * 由包含{@code BeanFactory}调用.
	 * <p>此方法允许bean实例在设置所有bean属性时, 执行其整体配置和最终初始化的验证.
	 * 
	 * @throws Exception 如果配置错误 (例如未能设置基本属性), 或者由于任何其他原因导致初始化失败
	 */
	void afterPropertiesSet() throws Exception;

}
