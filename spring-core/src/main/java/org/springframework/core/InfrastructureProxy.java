package org.springframework.core;

/**
 * 由透明资源代理实现的接口, 需要被视为与底层资源相同,
 * 例如, 用于一致的查找键比较.
 * 请注意, 此接口确实意味着这种特殊语义, 并不构成通用mixin!
 *
 * <p>这些包装器将自动解包, 以便在
 * {@link org.springframework.transaction.support.TransactionSynchronizationManager}中进行Key比较.
 *
 * <p>只有完全透明的代理, e.g. 对于重定向或服务查找, 应该实现此接口.
 * 用新行为装饰目标对象的代理, 例如AOP代理, 在这里<i>不</i>符合条件!
 */
public interface InfrastructureProxy {

	/**
	 * 返回底层资源 (never {@code null}).
	 */
	Object getWrappedObject();

}
