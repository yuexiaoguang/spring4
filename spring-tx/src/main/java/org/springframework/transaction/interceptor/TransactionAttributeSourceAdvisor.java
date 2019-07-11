package org.springframework.transaction.interceptor;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;

/**
 * 由{@link TransactionAttributeSource}驱动的顾问, 用于仅为事务性方法包含{@link TransactionInterceptor}.
 *
 * <p>因为AOP框架缓存增强计算, 这通常比让TransactionInterceptor运行并发现自己没有工作要做更快.
 */
@SuppressWarnings("serial")
public class TransactionAttributeSourceAdvisor extends AbstractPointcutAdvisor {

	private TransactionInterceptor transactionInterceptor;

	private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
		@Override
		protected TransactionAttributeSource getTransactionAttributeSource() {
			return (transactionInterceptor != null ? transactionInterceptor.getTransactionAttributeSource() : null);
		}
	};


	public TransactionAttributeSourceAdvisor() {
	}

	/**
	 * @param interceptor 用于此顾问的事务拦截器
	 */
	public TransactionAttributeSourceAdvisor(TransactionInterceptor interceptor) {
		setTransactionInterceptor(interceptor);
	}


	/**
	 * 设置要用于此顾问的事务拦截器.
	 */
	public void setTransactionInterceptor(TransactionInterceptor interceptor) {
		this.transactionInterceptor = interceptor;
	}

	/**
	 * 设置用于此切点的{@link ClassFilter}.
	 * 默认{@link ClassFilter#TRUE}.
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}


	@Override
	public Advice getAdvice() {
		return this.transactionInterceptor;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
