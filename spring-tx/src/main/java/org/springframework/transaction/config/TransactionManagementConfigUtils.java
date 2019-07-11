package org.springframework.transaction.config;

/**
 * 跨子包进行内部共享的配置常量.
 */
public abstract class TransactionManagementConfigUtils {

	/**
	 * 内部管理的事务顾问的bean名称 (在mode == PROXY时使用).
	 */
	public static final String TRANSACTION_ADVISOR_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionAdvisor";

	/**
	 * 内部管理的事务切面的bean名称 (在mode == ASPECTJ时使用).
	 */
	public static final String TRANSACTION_ASPECT_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionAspect";

	/**
	 * AspectJ事务管理切面的类名.
	 */
	public static final String TRANSACTION_ASPECT_CLASS_NAME =
			"org.springframework.transaction.aspectj.AnnotationTransactionAspect";

	/**
	 * AspectJ事务管理 @{@code Configuration}类的名称.
	 */
	public static final String TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration";

	/**
	 * 内部管理的TransactionalEventListenerFactory的bean名称.
	 */
	public static final String TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionalEventListenerFactory";

}
