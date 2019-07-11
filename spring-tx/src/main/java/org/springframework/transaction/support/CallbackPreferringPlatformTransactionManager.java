package org.springframework.transaction.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}接口的扩展,
 * 公开在事务中执行给定回调的方法.
 *
 * <p>此接口的实现者自动表示对程序化{@code getTransaction}, {@code commit} 和 {@code rollback}调用的回调首选.
 * 调用代码可以检查给定的事务管理器是否实现了此接口, 以选择准备回调, 而不是显式事务划分控制.
 *
 * <p>Spring的{@link TransactionTemplate}
 * 和{@link org.springframework.transaction.interceptor.TransactionInterceptor}
 * 自动检测并使用此PlatformTransactionManager变体.
 */
public interface CallbackPreferringPlatformTransactionManager extends PlatformTransactionManager {

	/**
	 * 在事务中执行给定回调对象指定的操作.
	 * <p>允许返回在事务中创建的结果对象, 即域对象或域对象的集合.
	 * 回调抛出的RuntimeException被视为强制执行回滚的致命异常.
	 * 这种异常会传播到模板的调用者.
	 * 
	 * @param definition 用于包装回调的事务的定义
	 * @param callback 指定事务操作的回调对象
	 * 
	 * @return 回调返回的结果对象, 或{@code null}
	 * @throws TransactionException 在初始化, 回滚或系统错误的情况下
	 * @throws RuntimeException 如果由TransactionCallback抛出
	 */
	<T> T execute(TransactionDefinition definition, TransactionCallback<T> callback)
			throws TransactionException;

}
