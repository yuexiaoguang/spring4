package org.springframework.transaction.support;

import org.springframework.transaction.TransactionException;

/**
 * 指定基本事务执行操作的接口.
 * 由{@link TransactionTemplate}实现.
 * 不经常直接使用, 但是能够增强可测试性, 因为它很容易被模拟或存根.
 */
public interface TransactionOperations {

	/**
	 * 在事务中执行给定回调对象指定的操作.
	 * <p>允许返回在事务中创建的结果对象, 即域对象或域对象的集合.
	 * 回调抛出的RuntimeException被视为强制执行回滚的致命异常.
	 * 这种异常会传播到模板的调用者.
	 * 
	 * @param action 指定事务操作的回调对象
	 * 
	 * @return 回调返回的结果对象, 或{@code null}
	 * @throws TransactionException 在初始化, 回滚, 或系统错误的情况下
	 * @throws RuntimeException 如果由TransactionCallback抛出
	 */
	<T> T execute(TransactionCallback<T> action) throws TransactionException;

}
