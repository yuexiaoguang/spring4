package org.springframework.transaction.interceptor;

import org.springframework.transaction.TransactionDefinition;

/**
 * 此接口将{@code rollbackOn}规范添加到{@link TransactionDefinition}.
 * 由于自定义{@code rollbackOn}仅适用于AOP, 因此该类驻留在AOP事务包中.
 */
public interface TransactionAttribute extends TransactionDefinition {

	/**
	 * 返回与此事务属性关联的限定符值.
	 * <p>这可以用于选择相应的事务管理器来处理该特定事务.
	 */
	String getQualifier();

	/**
	 * 是否在给定的异常上回滚?
	 * 
	 * @param ex 要评估的异常
	 * 
	 * @return 是否执行回滚
	 */
	boolean rollbackOn(Throwable ex);

}
