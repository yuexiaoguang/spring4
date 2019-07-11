package org.springframework.transaction.interceptor;

import org.springframework.aop.SpringProxy;

/**
 * 用于手动创建的事务代理的标记接口.
 *
 * <p>{@link TransactionAttributeSourcePointcut}将在AOP自动代理期间忽略此类现有事务代理,
 * 从而避免重新处理它们上的事务元数据.
 */
public interface TransactionalProxy extends SpringProxy {

}
