package org.springframework.aop.framework;

/**
 * 由能够基于{@link AdvisedSupport}配置对象创建AOP代理的工厂实现的接口.
 *
 * <p>代理应遵守以下规范:
 * <ul>
 * <li>它们应该实现配置指示的应该代理的所有接口.
 * <li>它们应该实现 {@link Advised}接口.
 * <li>应该实现equals方法来比较代理接口，增强和目标.
 * <li>如果所有切面和目标都是可序列化的，它们应该是可序列化的.
 * <li>如果切面和目标是线程安全的，它们应该是线程安全的.
 * </ul>
 *
 * <p>代理可能会也可能不会允许进行增强更改.
 * 如果他们不允许改变增强 (例如, 因为配置已冻结), 代理应该在尝试修改增强时抛出{@link AopConfigException}.
 */
public interface AopProxyFactory {

	/**
	 * 为给定的AOP配置创建{@link AopProxy}.
	 * 
	 * @param config AdvisedSupport对象形式的AOP配置
	 * 
	 * @return 相应的 AOP代理
	 * @throws AopConfigException 配置无效
	 */
	AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException;

}
