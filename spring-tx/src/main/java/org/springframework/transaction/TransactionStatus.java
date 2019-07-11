package org.springframework.transaction;

import java.io.Flushable;

/**
 * 表示事务状态.
 *
 * <p>事务代码可以使用它来检索状态信息, 并以编程方式请求回滚 (而不是抛出导致隐式回滚的异常).
 *
 * <p>从SavepointManager界面派生, 以提供对保存点管理工具的访问.
 * 请注意, 只有在底层事务管理器支持的情况下, 保存点管理才可用.
 */
public interface TransactionStatus extends SavepointManager, Flushable {

	/**
	 * 返回当前事务是否为新事务 (否则参与现有事务, 或者可能不首先在实际事务中运行).
	 */
	boolean isNewTransaction();

	/**
	 * 返回此事务是否在内部携带保存点, 即基于保存点创建为嵌套事务.
	 * <p>此方法主要用于诊断目的, 与{@link #isNewTransaction()}一起使用.
	 * 对于自定义保存点的​​编程处理, 使用SavepointManager的操作.
	 */
	boolean hasSavepoint();

	/**
	 * 设置仅事务回滚. 这指示事务管理器事务的唯一可能结果就是回滚, 作为抛出会触发回滚的异常的替代方法.
	 * <p>这主要用于{@link org.springframework.transaction.support.TransactionTemplate}
	 * 或{@link org.springframework.transaction.interceptor.TransactionInterceptor}管理的事务,
	 * 其中实际的提交/回滚决策由容器决定.
	 */
	void setRollbackOnly();

	/**
	 * 返回事务是否已标记为仅回滚 (由应用程序或事务基础结构).
	 */
	boolean isRollbackOnly();

	/**
	 * 如果适用, 将底层会话刷新到数据存储区: 例如, 所有受影响的Hibernate/JPA会话.
	 * <p>这实际上只是一个提示, 如果底层事务管理器没有刷新概念, 则可能是无操作.
	 * 刷新信号可以应用于主资源或事务同步, 具体取决于底层资源.
	 */
	@Override
	void flush();

	/**
	 * 返回此事务是否已完成, 即是否已提交或回滚.
	 */
	boolean isCompleted();

}
