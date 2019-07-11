package org.springframework.transaction.jta;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * JTA的{@link org.springframework.transaction.PlatformTransactionManager}实现, 委托给后端JTA提供者.
 * 这通常用于委托给Java EE服务器的事务协调器, 但也可以使用嵌入在应用程序中的本地JTA提供者配置.
 *
 * <p>此事务管理器适用于处理分布式事务, i.e. 跨越多个资源的事务,
 * 以及用于控制应用程序服务器资源上的事务 (e.g. JNDI中可用的JDBC数据源).
 * 对于单个JDBC DataSource, DataSourceTransactionManager是完全足够的,
 * 对于使用Hibernate (包括事务缓存)访问单个资源, HibernateTransactionManager是合适的.
 *
 * <p><b>对于典型的JTA事务 (REQUIRED, SUPPORTS, MANDATORY, NEVER),
 * 只需要一个简单的JtaTransactionManager定义, 可以在所有Java EE服务器上移植.</b>
 * 这对应于JTA UserTransaction的功能, Java EE为其指定标准JNDI名称 ("java:comp/UserTransaction").
 * 无需为此类JTA用法配置特定于服务器的TransactionManager查找.
 *
 * <p><b>事务暂停 (REQUIRES_NEW, NOT_SUPPORTED) 仅在注册JTA TransactionManager时可用.</b>
 * 公共TransactionManager位置由JtaTransactionManager自动检测,
 * 前提是"autodetectTransactionManager"标志设置为"true" (默认情况下).
 *
 * <p>Note: Java EE不需要支持JTA TransactionManager接口.
 * 几乎所有Java EE服务器都公开它, 但是作为EE的扩展. 尽管TransactionManager接口是JTA的一部分, 但可能存在一些兼容性问题.
 * 因此, Spring提供了各种特定于供应商的PlatformTransactionManagers, 如果合适, 建议使用它们:
 * {@link WebLogicJtaTransactionManager} 和 {@link WebSphereUowTransactionManager}.
 * 对于所有其他Java EE服务器, 标准的JtaTransactionManager就足够了.
 *
 * <p>这个纯JtaTransactionManager类支持超时, 但不支持每个事务的隔离级别.
 * 自定义子类可以覆盖特定JTA扩展的{@link #doJtaBegin}方法, 以便提供此功能;
 * Spring包含用于WebLogic Server的相应{@link WebLogicJtaTransactionManager}类.
 * 用于特定Java EE事务协调器的此类适配器还可以公开用于监视的事务名称; 使用标准JTA, 将简单地忽略事务名称.
 *
 * <p><b>考虑使用Spring的{@code tx:jta-transaction-manager}配置元素,
 * 自动选择适当的JTA平台事务管理器 (自动检测WebLogic 和 WebSphere).</b>
 *
 * <p>除了标准的JTA UserTransaction句柄之外, JTA 1.1还添加了TransactionSynchronizationRegistry工具, 作为公共Java EE 5 API.
 * 从Spring 2.5开始, 这个JtaTransactionManager自动检测TransactionSynchronizationRegistry,
 * 并在参与现有JTA事务时使用它来注册Spring管理的同步 (e.g. 由EJB CMT控制).
 * 如果没有TransactionSynchronizationRegistry可用, 则将通过 (non-EE) JTA TransactionManager句柄注册此类同步.
 *
 * <p>这个类是可序列化的. 但是, 活动的同步不会在序列化后继续存在.
 */
@SuppressWarnings("serial")
public class JtaTransactionManager extends AbstractPlatformTransactionManager
		implements TransactionFactory, InitializingBean, Serializable {

	/**
	 * JTA UserTransaction的默认JNDI位置. 许多Java EE服务器还为那里的JTA TransactionManager接口提供支持.
	 */
	public static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";

	/**
	 * JTA TransactionManager的后备JNDI位置.
	 * 如果JTA UserTransaction未实现JTA TransactionManager接口, 则应用, 前提是"autodetectTransactionManager"标志为 "true".
	 */
	public static final String[] FALLBACK_TRANSACTION_MANAGER_NAMES =
			new String[] {"java:comp/TransactionManager", "java:appserver/TransactionManager",
					"java:pm/TransactionManager", "java:/TransactionManager"};

	/**
	 * JTA TransactionSynchronizationRegistry的标准Java EE 5 JNDI位置.
	 * 可用时自动检测.
	 */
	public static final String DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME =
			"java:comp/TransactionSynchronizationRegistry";


	private transient JndiTemplate jndiTemplate = new JndiTemplate();

	private transient UserTransaction userTransaction;

	private String userTransactionName;

	private boolean autodetectUserTransaction = true;

	private boolean cacheUserTransaction = true;

	private boolean userTransactionObtainedFromJndi = false;

	private transient TransactionManager transactionManager;

	private String transactionManagerName;

	private boolean autodetectTransactionManager = true;

	private transient TransactionSynchronizationRegistry transactionSynchronizationRegistry;

	private String transactionSynchronizationRegistryName;

	private boolean autodetectTransactionSynchronizationRegistry = true;

	private boolean allowCustomIsolationLevels = false;


	/**
	 * 调用{@code afterPropertiesSet}以激活配置.
	 */
	public JtaTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	/**
	 * @param userTransaction 用作直接引用的JTA UserTransaction
	 */
	public JtaTransactionManager(UserTransaction userTransaction) {
		this();
		Assert.notNull(userTransaction, "UserTransaction must not be null");
		this.userTransaction = userTransaction;
	}

	/**
	 * @param userTransaction 用作直接引用的JTA UserTransaction
	 * @param transactionManager 用作直接引用的JTA TransactionManager
	 */
	public JtaTransactionManager(UserTransaction userTransaction, TransactionManager transactionManager) {
		this();
		Assert.notNull(userTransaction, "UserTransaction must not be null");
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.userTransaction = userTransaction;
		this.transactionManager = transactionManager;
	}

	/**
	 * @param transactionManager 用作直接引用的JTA TransactionManager
	 */
	public JtaTransactionManager(TransactionManager transactionManager) {
		this();
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
		this.userTransaction = buildUserTransaction(transactionManager);
	}


	/**
	 * 设置用于JNDI查找的JndiTemplate.
	 * 如果未设置, 则使用默认值.
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		Assert.notNull(jndiTemplate, "JndiTemplate must not be null");
		this.jndiTemplate = jndiTemplate;
	}

	/**
	 * 返回用于JNDI查找的JndiTemplate.
	 */
	public JndiTemplate getJndiTemplate() {
		return this.jndiTemplate;
	}

	/**
	 * 设置用于JNDI查找的JNDI环境.
	 * 使用给定的环境设置创建JndiTemplate.
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * 返回用于JNDI查找的JNDI环境.
	 */
	public Properties getJndiEnvironment() {
		return this.jndiTemplate.getEnvironment();
	}


	/**
	 * 设置用作直接引用的JTA UserTransaction.
	 * <p>通常只用于本地JTA设置; 在Java EE环境中, 将始终从JNDI获取UserTransaction.
	 */
	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	/**
	 * 返回此事务管理器使用的JTA UserTransaction.
	 */
	public UserTransaction getUserTransaction() {
		return this.userTransaction;
	}

	/**
	 * 设置JTA UserTransaction的JNDI名称.
	 * <p>请注意, 如果未明确指定, UserTransaction将在Java EE默认位置"java:comp/UserTransaction"中自动检测.
	 */
	public void setUserTransactionName(String userTransactionName) {
		this.userTransactionName = userTransactionName;
	}

	/**
	 * 设置是否在Java EE指定的默认JNDI位置"java:comp/UserTransaction"中自动检测JTA UserTransaction.
	 * 如果没有找到, 将在没有UserTransaction的情况下继续.
	 * <p>默认"true", 除非已明确指定, 否则自动检测UserTransaction.
	 * 关闭此标志以允许JtaTransactionManager仅对TransactionManager进行操作, 尽管默认的UserTransaction可用.
	 */
	public void setAutodetectUserTransaction(boolean autodetectUserTransaction) {
		this.autodetectUserTransaction = autodetectUserTransaction;
	}

	/**
	 * 设置是否缓存从JNDI获取的JTA UserTransaction对象.
	 * <p>默认"true": UserTransaction查找仅在启动时发生, 为所有线程的所有事务重用相同的UserTransaction句柄.
	 * 这是提供共享UserTransaction对象的所有应用程序服务器的最有效选择 (典型情况).
	 * <p>关闭此标志可为每个事务强制重新查找UserTransaction.
	 * 这仅适用于为每个事务返回新UserTransaction的应用程序服务器, 保持状态绑定到UserTransaction对象本身而不是当前线程.
	 */
	public void setCacheUserTransaction(boolean cacheUserTransaction) {
		this.cacheUserTransaction = cacheUserTransaction;
	}

	/**
	 * 设置用作直接引用的JTA TransactionManager.
	 * <p>TransactionManager是暂停和恢复事务所必需的, 因为UserTransaction接口不支持这种事务.
	 * <p>请注意, 如果JTA UserTransaction对象也实现了JTA TransactionManager接口,
	 * 并且在各种已知的后备JNDI位置自动检测, 则将自动检测TransactionManager.
	 */
	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回此事务管理器使用的JTA TransactionManager.
	 */
	public TransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * 设置JTA TransactionManager的JNDI名称.
	 * <p>TransactionManager 是暂停和恢复事务所必需的, 因为UserTransaction接口不支持这种事务.
	 * <p>请注意, 如果JTA UserTransaction对象也实现了JTA TransactionManager接口,
	 * 并且在各种已知的后备JNDI位置自动检测, 则将自动检测TransactionManager.
	 */
	public void setTransactionManagerName(String transactionManagerName) {
		this.transactionManagerName = transactionManagerName;
	}

	/**
	 * 设置是否自动检测实现了JTA TransactionManager接口的JTA UserTransaction对象
	 * (i.e. TransactionManager的JNDI位置是"java:comp/UserTransaction", 与UserTransaction相同).
	 * 还检查后备JNDI位置"java:comp/TransactionManager"和"java:/TransactionManager".
	 * 如果没有找到, 将不使用TransactionManager, 并继续.
	 * <p>默认"true", 自动检测TransactionManager, 除非已明确指定.
	 * 可以关闭以故意忽略可用的TransactionManager, 例如当挂起/恢复已知问题时,
	 * 任何使用REQUIRES_NEW或NOT_SUPPORTED的尝试都会快速失败.
	 */
	public void setAutodetectTransactionManager(boolean autodetectTransactionManager) {
		this.autodetectTransactionManager = autodetectTransactionManager;
	}

	/**
	 * 设置直接引用的JTA 1.1 TransactionSynchronizationRegistry.
	 * <p>TransactionSynchronizationRegistry允许插入事务同步注册,
	 * 作为JTA TransactionManager API上常规注册方法的替代方法.
	 * 此外, 它是Java EE 5平台的官方部分, 与JTA TransactionManager本身形成对比.
	 * <p>请注意, TransactionSynchronizationRegistry将在JNDI中自动检测,
	 * 如果在那里实现, 也将从 UserTransaction/TransactionManager对象中自动检测.
	 */
	public void setTransactionSynchronizationRegistry(TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
		this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
	}

	/**
	 * 返回此事务管理器使用的JTA 1.1 TransactionSynchronizationRegistry.
	 */
	public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
		return this.transactionSynchronizationRegistry;
	}

	/**
	 * 设置JTA 1.1 TransactionSynchronizationRegistry的JNDI名称.
	 * <p>请注意, 如果未明确指定, 则将在Java EE 5默认位置"java:comp/TransactionSynchronizationRegistry"中
	 * 自动检测TransactionSynchronizationRegistry.
	 */
	public void setTransactionSynchronizationRegistryName(String transactionSynchronizationRegistryName) {
		this.transactionSynchronizationRegistryName = transactionSynchronizationRegistryName;
	}

	/**
	 * 设置是否在其默认JDNI位置("java:comp/TransactionSynchronizationRegistry")
	 * 自动检测JTA 1.1 TransactionSynchronizationRegistry对象, 如果还从JNDI获取UserTransaction,
	 * 以及是否回退到检查JTA UserTransaction/TransactionManager对象是否实现了JTA TransactionSynchronizationRegistry接口.
	 * <p>默认"true", 自动检测TransactionSynchronizationRegistry, 除非已明确指定.
	 * 可以关闭以将同步注册委托给常规JTA TransactionManager API.
	 */
	public void setAutodetectTransactionSynchronizationRegistry(boolean autodetectTransactionSynchronizationRegistry) {
		this.autodetectTransactionSynchronizationRegistry = autodetectTransactionSynchronizationRegistry;
	}

	/**
	 * 设置是否允许指定自定义隔离级别.
	 * <p>默认"false", 如果为事务指定了非默认隔离级别, 则抛出异常.
	 * 如果受影响的资源适配器检查线程绑定的事务上下文, 并单独应用指定的隔离级别 (e.g. 通过IsolationLevelDataSourceAdapter),
	 * 打开此标志.
	 */
	public void setAllowCustomIsolationLevels(boolean allowCustomIsolationLevels) {
		this.allowCustomIsolationLevels = allowCustomIsolationLevels;
	}


	/**
	 * 初始化UserTransaction以及TransactionManager句柄.
	 */
	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		initUserTransactionAndTransactionManager();
		checkUserTransactionAndTransactionManager();
		initTransactionSynchronizationRegistry();
	}

	/**
	 * 初始化UserTransaction以及TransactionManager句柄.
	 * 
	 * @throws TransactionSystemException 如果初始化失败
	 */
	protected void initUserTransactionAndTransactionManager() throws TransactionSystemException {
		if (this.userTransaction == null) {
			// 必要时从JNDI获取JTA UserTransaction.
			if (StringUtils.hasLength(this.userTransactionName)) {
				this.userTransaction = lookupUserTransaction(this.userTransactionName);
				this.userTransactionObtainedFromJndi = true;
			}
			else {
				this.userTransaction = retrieveUserTransaction();
				if (this.userTransaction == null && this.autodetectUserTransaction) {
					// 在其默认的JNDI位置自动检测UserTransaction.
					this.userTransaction = findUserTransaction();
				}
			}
		}

		if (this.transactionManager == null) {
			// 必要时从JNDI获取JTA TransactionManager.
			if (StringUtils.hasLength(this.transactionManagerName)) {
				this.transactionManager = lookupTransactionManager(this.transactionManagerName);
			}
			else {
				this.transactionManager = retrieveTransactionManager();
				if (this.transactionManager == null && this.autodetectTransactionManager) {
					// 自动检测实现了TransactionManager的UserTransaction对象, 否则检查后备JNDI位置.
					this.transactionManager = findTransactionManager(this.userTransaction);
				}
			}
		}

		// 如果只指定了JTA TransactionManager, 则为其创建UserTransaction句柄.
		if (this.userTransaction == null && this.transactionManager != null) {
			this.userTransaction = buildUserTransaction(this.transactionManager);
		}
	}

	/**
	 * 假设标准JTA要求, 请检查UserTransaction以及TransactionManager句柄.
	 * 
	 * @throws IllegalStateException 如果没有足够的句柄可用
	 */
	protected void checkUserTransactionAndTransactionManager() throws IllegalStateException {
		// 至少需要JTA UserTransaction.
		if (this.userTransaction != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Using JTA UserTransaction: " + this.userTransaction);
			}
		}
		else {
			throw new IllegalStateException("No JTA UserTransaction available - specify either " +
					"'userTransaction' or 'userTransactionName' or 'transactionManager' or 'transactionManagerName'");
		}

		// 对于事务暂停, JTA TransactionManager也是必需的.
		if (this.transactionManager != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Using JTA TransactionManager: " + this.transactionManager);
			}
		}
		else {
			logger.warn("No JTA TransactionManager found: transaction suspension not available");
		}
	}

	/**
	 * 初始化JTA 1.1 TransactionSynchronizationRegistry.
	 * <p>要在{@link #initUserTransactionAndTransactionManager()}之后调用,
	 * 因为它可能会检查UserTransaction和TransactionManager句柄.
	 * 
	 * @throws TransactionSystemException 如果初始化失败
	 */
	protected void initTransactionSynchronizationRegistry() {
		if (this.transactionSynchronizationRegistry == null) {
			// 如有必要, 从JNDI获取JTA TransactionSynchronizationRegistry.
			if (StringUtils.hasLength(this.transactionSynchronizationRegistryName)) {
				this.transactionSynchronizationRegistry =
						lookupTransactionSynchronizationRegistry(this.transactionSynchronizationRegistryName);
			}
			else {
				this.transactionSynchronizationRegistry = retrieveTransactionSynchronizationRegistry();
				if (this.transactionSynchronizationRegistry == null && this.autodetectTransactionSynchronizationRegistry) {
					// 在JNDI中自动检测, 并检查实现了TransactionSynchronizationRegistry的UserTransaction/TransactionManager对象.
					this.transactionSynchronizationRegistry =
							findTransactionSynchronizationRegistry(this.userTransaction, this.transactionManager);
				}
			}
		}

		if (this.transactionSynchronizationRegistry != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Using JTA TransactionSynchronizationRegistry: " + this.transactionSynchronizationRegistry);
			}
		}
	}


	/**
	 * 根据给定的TransactionManager构建UserTransaction句柄.
	 * 
	 * @param transactionManager TransactionManager
	 * 
	 * @return 相应的UserTransaction句柄
	 */
	protected UserTransaction buildUserTransaction(TransactionManager transactionManager) {
		if (transactionManager instanceof UserTransaction) {
			return (UserTransaction) transactionManager;
		}
		else {
			return new UserTransactionAdapter(transactionManager);
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找JTA UserTransaction.
	 * <p>如果没有设置直接的UserTransaction引用, 则由{@code afterPropertiesSet}调用.
	 * 可以在子类中重写以提供不同的UserTransaction对象.
	 * 
	 * @param userTransactionName UserTransaction的JNDI名称
	 * 
	 * @return UserTransaction对象
	 * @throws TransactionSystemException 如果JNDI查找失败
	 */
	protected UserTransaction lookupUserTransaction(String userTransactionName)
			throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving JTA UserTransaction from JNDI location [" + userTransactionName + "]");
			}
			return getJndiTemplate().lookup(userTransactionName, UserTransaction.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"JTA UserTransaction is not available at JNDI location [" + userTransactionName + "]", ex);
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找JTA TransactionManager.
	 * <p>如果没有设置直接的TransactionManager引用, 则由{@code afterPropertiesSet}调用.
	 * 可以在子类中重写以提供不同的TransactionManager对象.
	 * 
	 * @param transactionManagerName TransactionManager的JNDI名称
	 * 
	 * @return UserTransaction对象
	 * @throws TransactionSystemException 如果JNDI查找失败
	 */
	protected TransactionManager lookupTransactionManager(String transactionManagerName)
			throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving JTA TransactionManager from JNDI location [" + transactionManagerName + "]");
			}
			return getJndiTemplate().lookup(transactionManagerName, TransactionManager.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"JTA TransactionManager is not available at JNDI location [" + transactionManagerName + "]", ex);
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找JTA 1.1 TransactionSynchronizationRegistry.
	 * <p>可以在子类中重写以提供不同的TransactionManager对象.
	 * 
	 * @param registryName TransactionSynchronizationRegistry的JNDI名称
	 * 
	 * @return TransactionSynchronizationRegistry对象
	 * @throws TransactionSystemException 如果JNDI查找失败
	 */
	protected TransactionSynchronizationRegistry lookupTransactionSynchronizationRegistry(String registryName) throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving JTA TransactionSynchronizationRegistry from JNDI location [" + registryName + "]");
			}
			return getJndiTemplate().lookup(registryName, TransactionSynchronizationRegistry.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"JTA TransactionSynchronizationRegistry is not available at JNDI location [" + registryName + "]", ex);
		}
	}

	/**
	 * 允许子类以特定于供应商的方式检索JTA UserTransaction.
	 * 仅在未指定"userTransaction" 或 "userTransactionName"时调用.
	 * <p>默认实现只返回{@code null}.
	 * 
	 * @return 要使用的JTA UserTransaction句柄, 或{@code null}
	 * @throws TransactionSystemException 错误
	 */
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		return null;
	}

	/**
	 * 允许子类以特定于供应商的方式检索JTA TransactionManager.
	 * 仅在未指定"transactionManager"或"transactionManagerName"时调用.
	 * <p>默认实现只返回{@code null}.
	 * 
	 * @return 要使用的JTA TransactionManager句柄, 或{@code null}
	 * @throws TransactionSystemException
	 */
	protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
		return null;
	}

	/**
	 * 允许子类以特定于供应商的方式检索JTA 1.1 TransactionSynchronizationRegistry.
	 * <p>默认实现只返回{@code null}.
	 * 
	 * @return 要使用的JTA TransactionSynchronizationRegistry句柄, 或{@code null}
	 * @throws TransactionSystemException
	 */
	protected TransactionSynchronizationRegistry retrieveTransactionSynchronizationRegistry() throws TransactionSystemException {
		return null;
	}

	/**
	 * 通过默认的JNDI查找查找JTA UserTransaction: "java:comp/UserTransaction".
	 * 
	 * @return JTA UserTransaction引用, 或{@code null}
	 */
	protected UserTransaction findUserTransaction() {
		String jndiName = DEFAULT_USER_TRANSACTION_NAME;
		try {
			UserTransaction ut = getJndiTemplate().lookup(jndiName, UserTransaction.class);
			if (logger.isDebugEnabled()) {
				logger.debug("JTA UserTransaction found at default JNDI location [" + jndiName + "]");
			}
			this.userTransactionObtainedFromJndi = true;
			return ut;
		}
		catch (NamingException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No JTA UserTransaction found at default JNDI location [" + jndiName + "]", ex);
			}
			return null;
		}
	}

	/**
	 * 通过自动检测找到JTA TransactionManager: 检查UserTransaction对象是否实现了TransactionManager, 并检查后备JNDI位置.
	 * 
	 * @param ut JTA UserTransaction对象
	 * 
	 * @return JTA TransactionManager引用, 或{@code null}
	 */
	protected TransactionManager findTransactionManager(UserTransaction ut) {
		if (ut instanceof TransactionManager) {
			if (logger.isDebugEnabled()) {
				logger.debug("JTA UserTransaction object [" + ut + "] implements TransactionManager");
			}
			return (TransactionManager) ut;
		}

		// 检查后备JNDI位置.
		for (String jndiName : FALLBACK_TRANSACTION_MANAGER_NAMES) {
			try {
				TransactionManager tm = getJndiTemplate().lookup(jndiName, TransactionManager.class);
				if (logger.isDebugEnabled()) {
					logger.debug("JTA TransactionManager found at fallback JNDI location [" + jndiName + "]");
				}
				return tm;
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No JTA TransactionManager found at fallback JNDI location [" + jndiName + "]", ex);
				}
			}
		}

		// OK, 所以没有JTA TransactionManager可用...
		return null;
	}

	/**
	 * 通过自动检测查找JTA 1.1 TransactionSynchronizationRegistry:
	 * 检查UserTransaction对象或TransactionManager对象是否实现了它, 以及检查Java EE 5的标准JNDI位置.
	 * <p>默认实现返回{@code null}.
	 * 
	 * @param ut JTA UserTransaction对象
	 * @param tm JTA TransactionManager对象
	 * 
	 * @return 要使用的JTA TransactionSynchronizationRegistry句柄, 或{@code null}
	 * @throws TransactionSystemException
	 */
	protected TransactionSynchronizationRegistry findTransactionSynchronizationRegistry(UserTransaction ut, TransactionManager tm)
			throws TransactionSystemException {

		if (this.userTransactionObtainedFromJndi) {
			// 已经从JNDI获得UserTransaction, 因此TransactionSynchronizationRegistry也可能就在那里.
			String jndiName = DEFAULT_TRANSACTION_SYNCHRONIZATION_REGISTRY_NAME;
			try {
				TransactionSynchronizationRegistry tsr = getJndiTemplate().lookup(jndiName, TransactionSynchronizationRegistry.class);
				if (logger.isDebugEnabled()) {
					logger.debug("JTA TransactionSynchronizationRegistry found at default JNDI location [" + jndiName + "]");
				}
				return tsr;
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No JTA TransactionSynchronizationRegistry found at default JNDI location [" + jndiName + "]", ex);
				}
			}
		}
		// 检查UserTransaction或TransactionManager是否实现了它...
		if (ut instanceof TransactionSynchronizationRegistry) {
			return (TransactionSynchronizationRegistry) ut;
		}
		if (tm instanceof TransactionSynchronizationRegistry) {
			return (TransactionSynchronizationRegistry) tm;
		}
		// OK, 所以没有JTA 1.1 TransactionSynchronizationRegistry可用...
		return null;
	}


	/**
	 * 此实现返回JTA UserTransaction的JtaTransactionObject实例.
	 * <p>UserTransaction对象将重新查找当前事务, 或者将使用在启动时查找的缓存对象.
	 * 后者是默认值: 大多数应用程序服务器使用可以缓存的共享单例UserTransaction.
	 * 关闭"cacheUserTransaction"标志以对每个事务强制执行新的查找.
	 */
	@Override
	protected Object doGetTransaction() {
		UserTransaction ut = getUserTransaction();
		if (ut == null) {
			throw new CannotCreateTransactionException("No JTA UserTransaction available - " +
					"programmatic PlatformTransactionManager.getTransaction usage not supported");
		}
		if (!this.cacheUserTransaction) {
			ut = lookupUserTransaction(
					this.userTransactionName != null ? this.userTransactionName : DEFAULT_USER_TRANSACTION_NAME);
		}
		return doGetJtaTransaction(ut);
	}

	/**
	 * 获取给定当前UserTransaction的JTA事务对象.
	 * <p>子类可以覆盖它以提供JtaTransactionObject子类, 例如需要一些额外的JTA句柄.
	 * 
	 * @param ut 用于当前事务的UserTransaction句柄
	 * 
	 * @return 持有UserTransaction的JtaTransactionObject
	 */
	protected JtaTransactionObject doGetJtaTransaction(UserTransaction ut) {
		return new JtaTransactionObject(ut);
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			return (txObject.getUserTransaction().getStatus() != Status.STATUS_NO_TRANSACTION);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on getStatus", ex);
		}
	}

	/**
	 * 尽管存在已经存在的事务, 但该实现返回false以进一步调用doBegin.
	 * <p>JTA实现可能通过进一步的{@code UserTransaction.begin()}调用支持嵌套事务, 但从不支持保存点.
	 */
	@Override
	protected boolean useSavepointForNestedTransaction() {
		return false;
	}


	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			doJtaBegin(txObject, definition);
		}
		catch (NotSupportedException ex) {
			throw new NestedTransactionNotSupportedException(
					"JTA implementation does not support nested transactions", ex);
		}
		catch (UnsupportedOperationException ex) {
			throw new NestedTransactionNotSupportedException(
					"JTA implementation does not support nested transactions", ex);
		}
		catch (SystemException ex) {
			throw new CannotCreateTransactionException("JTA failure on begin", ex);
		}
	}

	/**
	 * 在JTA UserTransaction或TransactionManager上执行JTA开始.
	 * <p>此实现仅支持标准JTA功能:
	 * 也就是说, 没有每个事务的隔离级别和没有事务名称.
	 * 可以在子类中重写, 以用于特定的JTA实现.
	 * <p>在调用UserTransaction的{@code begin}方法之前调用{@code applyIsolationLevel}和{@code applyTimeout}.
	 * 
	 * @param txObject 包含UserTransaction的JtaTransactionObject
	 * @param definition TransactionDefinition实例, 描述传播行为, 隔离级别, 只读标志, 超时, 事务名称
	 * 
	 * @throws NotSupportedException 如果被JTA方法抛出
	 * @throws SystemException 如果被JTA方法抛出
	 */
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
			throws NotSupportedException, SystemException {

		applyIsolationLevel(txObject, definition.getIsolationLevel());
		int timeout = determineTimeout(definition);
		applyTimeout(txObject, timeout);
		txObject.getUserTransaction().begin();
	}

	/**
	 * 应用给定的事务隔离级别. 默认实现将为ISOLATION_DEFAULT以外的任何级别抛出异常.
	 * <p>要在特定JTA实现的子类中重写, 作为覆盖完整{@link #doJtaBegin}方法的替代方法.
	 * 
	 * @param txObject 包含UserTransaction的JtaTransactionObject
	 * @param isolationLevel 从事务定义中获取的隔离级别
	 * 
	 * @throws InvalidIsolationLevelException 如果无法应用给定的隔离级别
	 * @throws SystemException 如果被JTA实现抛出
	 */
	protected void applyIsolationLevel(JtaTransactionObject txObject, int isolationLevel)
			throws InvalidIsolationLevelException, SystemException {

		if (!this.allowCustomIsolationLevels && isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException(
					"JtaTransactionManager does not support custom isolation levels by default - " +
					"switch 'allowCustomIsolationLevels' to 'true'");
		}
	}

	/**
	 * 应用给定的事务超时. 默认实现将调用{@code UserTransaction.setTransactionTimeout}设置非默认超时值.
	 * 
	 * @param txObject 包含UserTransaction的JtaTransactionObject
	 * @param timeout 从事务定义中获取的超时值
	 * 
	 * @throws SystemException 如果被JTA实现抛出
	 */
	protected void applyTimeout(JtaTransactionObject txObject, int timeout) throws SystemException {
		if (timeout > TransactionDefinition.TIMEOUT_DEFAULT) {
			txObject.getUserTransaction().setTransactionTimeout(timeout);
			if (timeout > 0) {
				txObject.resetTransactionTimeout = true;
			}
		}
	}


	@Override
	protected Object doSuspend(Object transaction) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			return doJtaSuspend(txObject);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on suspend", ex);
		}
	}

	/**
	 * 在JTA TransactionManager上执行JTA暂停.
	 * <p>可以在子类中重写, 以用于特定的JTA实现.
	 * 
	 * @param txObject 包含UserTransaction的JtaTransactionObject
	 * 
	 * @return 暂停的JTA Transaction对象
	 * @throws SystemException 如果被JTA方法抛出
	 */
	protected Object doJtaSuspend(JtaTransactionObject txObject) throws SystemException {
		if (getTransactionManager() == null) {
			throw new TransactionSuspensionNotSupportedException(
					"JtaTransactionManager needs a JTA TransactionManager for suspending a transaction: " +
					"specify the 'transactionManager' or 'transactionManagerName' property");
		}
		return getTransactionManager().suspend();
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		try {
			doJtaResume(txObject, suspendedResources);
		}
		catch (InvalidTransactionException ex) {
			throw new IllegalTransactionStateException("Tried to resume invalid JTA transaction", ex);
		}
		catch (IllegalStateException ex) {
			throw new TransactionSystemException("Unexpected internal transaction state", ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on resume", ex);
		}
	}

	/**
	 * 在JTA TransactionManager上执行JTA恢复.
	 * <p>可以在子类中重写, 以用于特定的JTA实现.
	 * 
	 * @param txObject 包含UserTransaction的JtaTransactionObject
	 * @param suspendedTransaction 暂停的JTA Transaction对象
	 * 
	 * @throws InvalidTransactionException 如果被JTA方法抛出
	 * @throws SystemException 如果被JTA方法抛出
	 */
	protected void doJtaResume(JtaTransactionObject txObject, Object suspendedTransaction)
		throws InvalidTransactionException, SystemException {

		if (getTransactionManager() == null) {
			throw new TransactionSuspensionNotSupportedException(
					"JtaTransactionManager needs a JTA TransactionManager for suspending a transaction: " +
					"specify the 'transactionManager' or 'transactionManagerName' property");
		}
		getTransactionManager().resume((Transaction) suspendedTransaction);
	}


	/**
	 * 此实现返回"true": JTA提交将正确处理已在全局级别标记为仅回滚的事务.
	 */
	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
		try {
			int jtaStatus = txObject.getUserTransaction().getStatus();
			if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
				// 永远不应该发生... 之前会抛出异常, 导致回滚, 而不是提交调用.
				// 无论如何, 事务已经完全清理完毕.
				throw new UnexpectedRollbackException("JTA transaction already completed - probably rolled back");
			}
			if (jtaStatus == Status.STATUS_ROLLEDBACK) {
				// 只有在早期超时的情况下才会真正发生在JBoss 4.2上...
				// 清理事务所必需的显式回滚调用.
				// JBoss期望的IllegalStateException; 调用仍然有必要.
				try {
					txObject.getUserTransaction().rollback();
				}
				catch (IllegalStateException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Rollback failure with transaction already marked as rolled back: " + ex);
					}
				}
				throw new UnexpectedRollbackException("JTA transaction already rolled back (probably due to a timeout)");
			}
			txObject.getUserTransaction().commit();
		}
		catch (RollbackException ex) {
			throw new UnexpectedRollbackException(
					"JTA transaction unexpectedly rolled back (maybe due to a timeout)", ex);
		}
		catch (HeuristicMixedException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_MIXED, ex);
		}
		catch (HeuristicRollbackException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_ROLLED_BACK, ex);
		}
		catch (IllegalStateException ex) {
			throw new TransactionSystemException("Unexpected internal transaction state", ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on commit", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
		try {
			int jtaStatus = txObject.getUserTransaction().getStatus();
			if (jtaStatus != Status.STATUS_NO_TRANSACTION) {
				try {
					txObject.getUserTransaction().rollback();
				}
				catch (IllegalStateException ex) {
					if (jtaStatus == Status.STATUS_ROLLEDBACK) {
						// 只有在早期超时的情况下才会真正发生在JBoss 4.2上...
						if (logger.isDebugEnabled()) {
							logger.debug("Rollback failure with transaction already marked as rolled back: " + ex);
						}
					}
					else {
						throw new TransactionSystemException("Unexpected internal transaction state", ex);
					}
				}
			}
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on rollback", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JtaTransactionObject txObject = (JtaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JTA transaction rollback-only");
		}
		try {
			int jtaStatus = txObject.getUserTransaction().getStatus();
			if (jtaStatus != Status.STATUS_NO_TRANSACTION && jtaStatus != Status.STATUS_ROLLEDBACK) {
				txObject.getUserTransaction().setRollbackOnly();
			}
		}
		catch (IllegalStateException ex) {
			throw new TransactionSystemException("Unexpected internal transaction state", ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on setRollbackOnly", ex);
		}
	}


	@Override
	protected void registerAfterCompletionWithExistingTransaction(
			Object transaction, List<TransactionSynchronization> synchronizations) {

		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		logger.debug("Registering after-completion synchronization with existing JTA transaction");
		try {
			doRegisterAfterCompletionWithJtaTransaction(txObject, synchronizations);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on registerSynchronization", ex);
		}
		catch (Exception ex) {
			// Note: JBoss抛出普通的RuntimeException, 并将RollbackException作为原因.
			if (ex instanceof RollbackException || ex.getCause() instanceof RollbackException) {
				logger.debug("Participating in existing JTA transaction that has been marked for rollback: " +
						"cannot register Spring after-completion callbacks with outer JTA transaction - " +
						"immediately performing Spring after-completion callbacks with outcome status 'rollback'. " +
						"Original exception: " + ex);
				invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
			}
			else {
				logger.debug("Participating in existing JTA transaction, but unexpected internal transaction " +
						"state encountered: cannot register Spring after-completion callbacks with outer JTA " +
						"transaction - processing Spring after-completion callbacks with outcome status 'unknown'" +
						"Original exception: " + ex);
				invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
			}
		}
	}

	/**
	 * 在JTA TransactionManager上注册JTA同步, 以便在给定的Spring TransactionSynchronizations上调用{@code afterCompletion}.
	 * <p>默认实现在JTA 1.1 TransactionSynchronizationRegistry上注册同步,
	 * 或者在JTA TransactionManager的当前事务上注册 - 如果可用的话.
	 * 如果两者都不可用, 则会记录警告.
	 * <p>可以在子类中重写, 以用于特定的JTA实现.
	 * 
	 * @param txObject 当前的事务对象
	 * @param synchronizations TransactionSynchronization对象的列表
	 * 
	 * @throws RollbackException 如果被JTA方法抛出
	 * @throws SystemException 如果被JTA方法抛出
	 */
	protected void doRegisterAfterCompletionWithJtaTransaction(
			JtaTransactionObject txObject, List<TransactionSynchronization> synchronizations)
			throws RollbackException, SystemException {

		int jtaStatus = txObject.getUserTransaction().getStatus();
		if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
			throw new RollbackException("JTA transaction already completed - probably rolled back");
		}
		if (jtaStatus == Status.STATUS_ROLLEDBACK) {
			throw new RollbackException("JTA transaction already rolled back (probably due to a timeout)");
		}

		if (this.transactionSynchronizationRegistry != null) {
			// JTA 1.1 TransactionSynchronizationRegistry available - use it.
			this.transactionSynchronizationRegistry.registerInterposedSynchronization(
					new JtaAfterCompletionSynchronization(synchronizations));
		}

		else if (getTransactionManager() != null) {
			// 至少JTA TransactionManager可用 - 使用那个.
			Transaction transaction = getTransactionManager().getTransaction();
			if (transaction == null) {
				throw new IllegalStateException("No JTA Transaction available");
			}
			transaction.registerSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
		}

		else {
			// 没有JTA TransactionManager可用 - 记录警告.
			logger.warn("Participating in existing JTA transaction, but no JTA TransactionManager available: " +
					"cannot register Spring after-completion callbacks with outer JTA transaction - " +
					"processing Spring after-completion callbacks with outcome status 'unknown'");
			invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
		}
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JtaTransactionObject txObject = (JtaTransactionObject) transaction;
		if (txObject.resetTransactionTimeout) {
			try {
				txObject.getUserTransaction().setTransactionTimeout(0);
			}
			catch (SystemException ex) {
				logger.debug("Failed to reset transaction timeout after JTA completion", ex);
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of TransactionFactory interface
	//---------------------------------------------------------------------

	@Override
	public Transaction createTransaction(String name, int timeout) throws NotSupportedException, SystemException {
		TransactionManager tm = getTransactionManager();
		Assert.state(tm != null, "No JTA TransactionManager available");
		if (timeout >= 0) {
			tm.setTransactionTimeout(timeout);
		}
		tm.begin();
		return new ManagedTransactionAdapter(tm);
	}

	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return false;
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依靠默认序列化; 只是在反序列化后初始化状态.
		ois.defaultReadObject();

		// 为客户端JNDI查找创建模板.
		this.jndiTemplate = new JndiTemplate();

		// 重新查找JTA句柄.
		initUserTransactionAndTransactionManager();
		initTransactionSynchronizationRegistry();
	}

}
