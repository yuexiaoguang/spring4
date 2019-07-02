package org.springframework.test.context.transaction;

import org.springframework.core.style.ToStringCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * 用于配置事务测试的配置属性.
 *
 * @deprecated 从Spring Framework 4.2开始, 该类已被正式弃用,
 * 并且在删除{@code @TransactionConfiguration}时将被删除.
 */
@Deprecated
public class TransactionConfigurationAttributes {

	private final String transactionManagerName;

	private final boolean defaultRollback;


	public TransactionConfigurationAttributes() {
		this("", true);
	}

	/**
	 * @param transactionManagerName 用于驱动<em>测试管理的事务</em>的{@link PlatformTransactionManager}的bean名称
	 * @param defaultRollback 是否应默认回滚<em>测试管理的事务</em>
	 */
	public TransactionConfigurationAttributes(String transactionManagerName, boolean defaultRollback) {
		Assert.notNull(transactionManagerName, "transactionManagerName must not be null");
		this.transactionManagerName = transactionManagerName;
		this.defaultRollback = defaultRollback;
	}


	/**
	 * 获取用于驱动<em>测试管理的事务</em>的{@link PlatformTransactionManager}的bean名称.
	 */
	public final String getTransactionManagerName() {
		return this.transactionManagerName;
	}

	/**
	 * 是否应默认回滚<em>测试管理的事务</em>.
	 * 
	 * @return <em>默认回滚</em>标志
	 */
	public final boolean isDefaultRollback() {
		return this.defaultRollback;
	}


	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("transactionManagerName", this.transactionManagerName)
				.append("defaultRollback", this.defaultRollback)
				.toString();
	}

}
