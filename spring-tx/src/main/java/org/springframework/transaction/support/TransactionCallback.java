package org.springframework.transaction.support;

import org.springframework.transaction.TransactionStatus;

/**
 * 事务代码的回调接口.
 * 与{@link TransactionTemplate}的{@code execute}方法一起使用, 通常作为方法实现中的匿名类.
 *
 * <p>通常用于将对没有事务的数据访问服务的各种调用组合成具有事务划分的更高级服务方法.
 * 作为替代方案, 考虑使用声明式事务划分 (e.g. 通过Spring的{@link org.springframework.transaction.annotation.Transactional}注解).
 */
public interface TransactionCallback<T> {

	/**
	 * 在事务上下文中由{@link TransactionTemplate#execute}调用.
	 * 不需要关心事务本身, 尽管它可以通过给定的状态对象检索和影响当前事务的状态, e.g. 设置仅回滚.
	 * <p>允许返回在事务中创建的结果对象, i.e. 域对象或域对象的集合.
	 * 回调抛出的RuntimeException被视为强制执行回滚的应用程序异常.
	 * 任何此类异常都将传播到模板的调用者, 除非回滚有问题, 在这种情况下将抛出TransactionException.
	 * 
	 * @param status 关联的事务状态
	 * 
	 * @return 结果对象, 或{@code null}
	 */
	T doInTransaction(TransactionStatus status);

}
