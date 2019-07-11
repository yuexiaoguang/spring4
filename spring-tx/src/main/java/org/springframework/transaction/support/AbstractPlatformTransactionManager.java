package org.springframework.transaction.support;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Constants;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * 实现Spring标准事务工作流的抽象基类, 作为
 * {@link org.springframework.transaction.jta.JtaTransactionManager}等具体平台事务管理器的基础.
 *
 * <p>此基类提供以下工作流处理:
 * <ul>
 * <li>确定是否存在现有事务;
 * <li>应用适当的传播行为;
 * <li>必要时, 暂停和恢复事务;
 * <li>提交时检查rollback-only标志;
 * <li>在回滚时应用适当的修改 (实际回滚或设置仅回滚);
 * <li>触发已注册的同步回调 (如果事务同步处于活动状态).
 * </ul>
 *
 * <p>子类必须为事务的特定状态实现特定的模板方法, e.g.: begin, suspend, resume, commit, rollback.
 * 其中最重要的是抽象的, 必须由具体实现提供; 对于其余部分, 提供了默认值, 因此覆盖是可选的.
 *
 * <p>事务同步是一种通用机制, 用于注册在事务完成时调用的回调.
 * 当在JTA事务中运行时, 这主要由JDBC, Hibernate, JPA等的数据访问支持类在内部使用:
 * 它们注册在事务中打开的资源, 以便在事务完成时关闭, 允许在事务中重用相同的Hibernate会话.
 * 同样的机制也可以用于应用程序中的自定义同步需求.
 *
 * <p>此类的状态是可序列化的, 以允许序列化事务策略以及携带事务拦截器的代理.
 * 如果希望使其状态也可序列化, 则由子类决定.
 * 在这种情况下, 应该实现{@code java.io.Serializable}记接口, 如果需要恢复任何瞬态状态,
 * 可能会使用私有的{@code readObject()}方法 (根据Java序列化规则).
 */
@SuppressWarnings("serial")
public abstract class AbstractPlatformTransactionManager implements PlatformTransactionManager, Serializable {

	/**
	 * 始终激活事务同步, 即使对于没有现有后端事务的PROPAGATION_SUPPORTS导致的"空"事务也是如此.
	 */
	public static final int SYNCHRONIZATION_ALWAYS = 0;

	/**
	 * 仅针对实际事务激活事务同步, 即不是没有现有后端事务的PROPAGATION_SUPPORTS导致的"空"事务.
	 */
	public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;

	/**
	 * 从不激活事务同步, 即使对于实际事务也是如此.
	 */
	public static final int SYNCHRONIZATION_NEVER = 2;


	/** AbstractPlatformTransactionManager的常量实例 */
	private static final Constants constants = new Constants(AbstractPlatformTransactionManager.class);


	protected transient Log logger = LogFactory.getLog(getClass());

	private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;

	private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;

	private boolean nestedTransactionAllowed = false;

	private boolean validateExistingTransaction = false;

	private boolean globalRollbackOnParticipationFailure = true;

	private boolean failEarlyOnGlobalRollbackOnly = false;

	private boolean rollbackOnCommitFailure = false;


	/**
	 * 通过此类中相应常量的名称设置事务同步, e.g. "SYNCHRONIZATION_ALWAYS".
	 * 
	 * @param constantName 常量名称
	 */
	public final void setTransactionSynchronizationName(String constantName) {
		setTransactionSynchronization(constants.asNumber(constantName).intValue());
	}

	/**
	 * 设置什么时候此事务管理器应该激活线程绑定的事务同步支持. 默认"always".
	 * <p>请注意, 不同事务管理器的多个并发事务不支持事务同步.
	 * 任何时候只允许一个事务管理器激活它.
	 */
	public final void setTransactionSynchronization(int transactionSynchronization) {
		this.transactionSynchronization = transactionSynchronization;
	}

	/**
	 * 返回此事务管理器是否应激活线程绑定的事务同步支持.
	 */
	public final int getTransactionSynchronization() {
		return this.transactionSynchronization;
	}

	/**
	 * 如果在事务级别没有指定超时(以秒为单位), 指定此事务管理器应应用的默认超时.
	 * <p>默认是底层事务基础结构的默认超时, e.g. 对于JTA提供程序, 通常为30秒,
	 * 由{@code TransactionDefinition.TIMEOUT_DEFAULT}值指示.
	 */
	public final void setDefaultTimeout(int defaultTimeout) {
		if (defaultTimeout < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid default timeout", defaultTimeout);
		}
		this.defaultTimeout = defaultTimeout;
	}

	/**
	 * 如果在事务级别没有指定超时(以秒为单位), 则返回此事务管理器应应用的默认超时.
	 * <p>返回{@code TransactionDefinition.TIMEOUT_DEFAULT}, 以指示底层事务基础结构的默认超时.
	 */
	public final int getDefaultTimeout() {
		return this.defaultTimeout;
	}

	/**
	 * 设置是否允许嵌套事务. 默认 "false".
	 * <p>通常由具体事务管理器的子类以适当的默认值初始化.
	 */
	public final void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
		this.nestedTransactionAllowed = nestedTransactionAllowed;
	}

	/**
	 * 返回是否允许嵌套事务.
	 */
	public final boolean isNestedTransactionAllowed() {
		return this.nestedTransactionAllowed;
	}

	/**
	 * 设置是否应在参与之前验证现有事务.
	 * <p>当参与现有事务时, (e.g. PROPAGATION_REQUIRED 或 PROPAGATION_SUPPORTS遇到现有事务时),
	 * 此外部事务的特征甚至将适用于内部事务范围.
	 * 验证将检测内部事务定义上的不兼容隔离级别和只读设置, 并通过抛出相应的异常来拒绝参与.
	 * <p>默认"false", 忽略内部事务设置, 简单地用外部事务的特征覆盖它们.
	 * 将此标志切换为"true", 以强制执行严格的验证.
	 */
	public final void setValidateExistingTransaction(boolean validateExistingTransaction) {
		this.validateExistingTransaction = validateExistingTransaction;
	}

	/**
	 * 返回现有事务是否应在参与之前进行验证.
	 */
	public final boolean isValidateExistingTransaction() {
		return this.validateExistingTransaction;
	}

	/**
	 * 设置是否在参与的事务失败后, 将现有事务全局的标记为仅回滚.
	 * <p>默认"true": 如果参与的事务(e.g. PROPAGATION_REQUIRED 或 PROPAGATION_SUPPORTS 遇到现有事务)失败,
	 * 则该事务将全局的标记为仅回滚.
	 * 这种事务唯一可能的结果是回滚: 事务发起者<i>不能</i>再进行事务提交.
	 * <p>将其切换为"false"以让事务发起者做出回滚决定.
	 * 如果参与的事务因异常而失败, 则调用者仍然可以决定在事务中继续使用不同的路径.
	 * 但请注意, 只有所有参与资源即使在数据访问失败后仍能继续执行事务提交时, 此操作才会起作用:
	 * 例如, 对于Hibernate会话通常不是这种情况; 它不是一系列JDBC插入/更新/删除操作.
	 * <p><b>Note:</b>此标志仅适用于子事务的显式回滚尝试, 通常由数据访问操作引发的异常引起
	 * (其中TransactionInterceptor将根据回滚规则触发{@code PlatformTransactionManager.rollback()}调用).
	 * 如果标志关闭, 则调用者可以处理异常并决定回滚, 而不依赖于子事务的回滚规则.
	 * 但是, 此标志确实<i>不</i>适用于{@code TransactionStatus}上的显式{@code setRollbackOnly}调用,
	 * 这将始终导致最终的全局回滚 (因为它可能在回滚后不会引发异常).
	 * <p>处理子事务失败的推荐解决方案是"嵌套事务", 其中全局事务可以回滚到子事务开始时采用的保存点.
	 * PROPAGATION_NESTED 提供了那些语义; 但是, 它仅在嵌套事务支持可用时才有效.
	 * 这是DataSourceTransactionManager的情况, 但不是JtaTransactionManager.
	 */
	public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
		this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
	}

	/**
	 * 在参与的事务失败后, 返回是否将现有事务全局标记为仅回滚.
	 */
	public final boolean isGlobalRollbackOnParticipationFailure() {
		return this.globalRollbackOnParticipationFailure;
	}

	/**
	 * 设置是否在事务全局标记为仅回滚的情况下提前失败.
	 * <p>默认"false", 仅在最外层的事务边界处导致UnexpectedRollbackException.
	 * 早在第一次检测到全局仅回滚标记时, 即使在内部事务边界内, 也会打开此标志以导致UnexpectedRollbackException.
	 * <p>请注意, 从Spring 2.0开始, 全局回滚标记的失败早期行为已经统一:
	 * 默认情况下, 所有事务管理器仅在最外层事务边界处导致UnexpectedRollbackException.
	 * 例如, 即使在操作失败并且事务永远不会完成之后, 这也允许继续单元测试.
	 * 如果此标志已明确设置为"true", 则所有事务管理器将仅提前失败.
	 */
	public final void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
		this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * 如果事务全局标记为仅回滚, 则返回是否提前失败.
	 */
	public final boolean isFailEarlyOnGlobalRollbackOnly() {
		return this.failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * 设置是否应在{@code doCommit}调用失败时执行{@code doRollback}.
	 * 通常不是必需的, 因此要避免, 因为它可能会使用后续的回滚异常覆盖提交异常.
	 * <p>默认"false".
	 */
	public final void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
		this.rollbackOnCommitFailure = rollbackOnCommitFailure;
	}

	/**
	 * 返回是否应在{@code doCommit}调用失败时执行{@code doRollback}.
	 */
	public final boolean isRollbackOnCommitFailure() {
		return this.rollbackOnCommitFailure;
	}


	//---------------------------------------------------------------------
	// Implementation of PlatformTransactionManager
	//---------------------------------------------------------------------

	/**
	 * 此实现处理传播行为. 委托给{@code doGetTransaction}, {@code isExistingTransaction} 和 {@code doBegin}.
	 */
	@Override
	public final TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		Object transaction = doGetTransaction();

		// 缓存debug标志以避免重复检查.
		boolean debugEnabled = logger.isDebugEnabled();

		if (definition == null) {
			// 如果没有给出事务定义, 则使用默认值.
			definition = new DefaultTransactionDefinition();
		}

		if (isExistingTransaction(transaction)) {
			// 找到现有事务 -> 检查传播行为以了解如何处理.
			return handleExistingTransaction(definition, transaction, debugEnabled);
		}

		// 检查新事务的定义设置.
		if (definition.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", definition.getTimeout());
		}

		// 找不到现有的事务 -> 检查传播行为以了解如何继续.
		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
			throw new IllegalTransactionStateException(
					"No existing transaction found for transaction marked with propagation 'mandatory'");
		}
		else if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
				definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			SuspendedResourcesHolder suspendedResources = suspend(null);
			if (debugEnabled) {
				logger.debug("Creating new transaction with name [" + definition.getName() + "]: " + definition);
			}
			try {
				boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
				DefaultTransactionStatus status = newTransactionStatus(
						definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
				doBegin(transaction, definition);
				prepareSynchronization(status, definition);
				return status;
			}
			catch (RuntimeException ex) {
				resume(null, suspendedResources);
				throw ex;
			}
			catch (Error err) {
				resume(null, suspendedResources);
				throw err;
			}
		}
		else {
			// 创建"空"事务: 没有实际事务, 但可能是同步.
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
				logger.warn("Custom isolation level specified but no actual transaction initiated; " +
						"isolation level will effectively be ignored: " + definition);
			}
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			return prepareTransactionStatus(definition, null, true, newSynchronization, debugEnabled, null);
		}
	}

	/**
	 * 为现有事务创建TransactionStatus.
	 */
	private TransactionStatus handleExistingTransaction(
			TransactionDefinition definition, Object transaction, boolean debugEnabled)
			throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			throw new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'");
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction");
			}
			Object suspendedResources = suspend(transaction);
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			return prepareTransactionStatus(
					definition, null, false, newSynchronization, debugEnabled, suspendedResources);
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction, creating new transaction with name [" +
						definition.getName() + "]");
			}
			SuspendedResourcesHolder suspendedResources = suspend(transaction);
			try {
				boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
				DefaultTransactionStatus status = newTransactionStatus(
						definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
				doBegin(transaction, definition);
				prepareSynchronization(status, definition);
				return status;
			}
			catch (RuntimeException beginEx) {
				resumeAfterBeginException(transaction, suspendedResources, beginEx);
				throw beginEx;
			}
			catch (Error beginErr) {
				resumeAfterBeginException(transaction, suspendedResources, beginErr);
				throw beginErr;
			}
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			if (!isNestedTransactionAllowed()) {
				throw new NestedTransactionNotSupportedException(
						"Transaction manager does not allow nested transactions by default - " +
						"specify 'nestedTransactionAllowed' property with value 'true'");
			}
			if (debugEnabled) {
				logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
			}
			if (useSavepointForNestedTransaction()) {
				// 通过TransactionStatus实现的SavepointManager API在现有的Spring管理的事务中创建保存点.
				// 通常使用JDBC 3.0保存点. 永远不会激活Spring同步.
				DefaultTransactionStatus status =
						prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);
				status.createAndHoldSavepoint();
				return status;
			}
			else {
				// 嵌套事务通过嵌套的 begin 和 commit/rollback调用.
				// 通常仅针对JTA: 如果存在预先存在的JTA事务, 则可能会在此处激活Spring同步.
				boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
				DefaultTransactionStatus status = newTransactionStatus(
						definition, transaction, true, newSynchronization, debugEnabled, null);
				doBegin(transaction, definition);
				prepareSynchronization(status, definition);
				return status;
			}
		}

		// 可能是 PROPAGATION_SUPPORTS 或 PROPAGATION_REQUIRED.
		if (debugEnabled) {
			logger.debug("Participating in existing transaction");
		}
		if (isValidateExistingTransaction()) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
					Constants isoConstants = DefaultTransactionDefinition.constants;
					throw new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] specifies isolation level which is incompatible with existing transaction: " +
							(currentIsolationLevel != null ?
									isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) :
									"(unknown)"));
				}
			}
			if (!definition.isReadOnly()) {
				if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
					throw new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] is not marked as read-only but existing transaction is");
				}
			}
		}
		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
		return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
	}

	/**
	 * 为给定的参数创建一个新的TransactionStatus, 同时根据需要初始化事务同步.
	 */
	protected final DefaultTransactionStatus prepareTransactionStatus(
			TransactionDefinition definition, Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, Object suspendedResources) {

		DefaultTransactionStatus status = newTransactionStatus(
				definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
		prepareSynchronization(status, definition);
		return status;
	}

	/**
	 * 为给定的参数创建TransactionStatus实例.
	 */
	protected DefaultTransactionStatus newTransactionStatus(
			TransactionDefinition definition, Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, Object suspendedResources) {

		boolean actualNewSynchronization = newSynchronization &&
				!TransactionSynchronizationManager.isSynchronizationActive();
		return new DefaultTransactionStatus(
				transaction, newTransaction, actualNewSynchronization,
				definition.isReadOnly(), debug, suspendedResources);
	}

	/**
	 * 根据需要初始化事务同步.
	 */
	protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
			TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
					definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
							definition.getIsolationLevel() : null);
			TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
			TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
			TransactionSynchronizationManager.initSynchronization();
		}
	}

	/**
	 * 确定用于给定定义的实际超时.
	 * 如果事务定义未指定非默认值, 则将回退到此管理器的默认超时.
	 * 
	 * @param definition 事务定义
	 * 
	 * @return 要使用的实际超时
	 */
	protected int determineTimeout(TransactionDefinition definition) {
		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			return definition.getTimeout();
		}
		return this.defaultTimeout;
	}


	/**
	 * 暂停给定的事务. 首先暂停事务同步, 然后委托给{@code doSuspend}模板方法.
	 * 
	 * @param transaction 当前事务对象 (或{@code null}只是暂停活动的同步)
	 * 
	 * @return 保存暂停资源的对象 (如果事务和同步都未激活, 则为{@code null})
	 */
	protected final SuspendedResourcesHolder suspend(Object transaction) throws TransactionException {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
			try {
				Object suspendedResources = null;
				if (transaction != null) {
					suspendedResources = doSuspend(transaction);
				}
				String name = TransactionSynchronizationManager.getCurrentTransactionName();
				TransactionSynchronizationManager.setCurrentTransactionName(null);
				boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
				Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
				boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
				TransactionSynchronizationManager.setActualTransactionActive(false);
				return new SuspendedResourcesHolder(
						suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
			}
			catch (RuntimeException ex) {
				// doSuspend失败 - 原始事务仍处于活动状态...
				doResumeSynchronization(suspendedSynchronizations);
				throw ex;
			}
			catch (Error err) {
				// doSuspend 失败 - 原始事务仍处于活动状态...
				doResumeSynchronization(suspendedSynchronizations);
				throw err;
			}
		}
		else if (transaction != null) {
			// 事务处于活动状态但未激活同步.
			Object suspendedResources = doSuspend(transaction);
			return new SuspendedResourcesHolder(suspendedResources);
		}
		else {
			// 事务和同步都不活动.
			return null;
		}
	}

	/**
	 * 恢复给定的事务. 首先委托给 {@code doResume}模板方法, 然后恢复事务同步.
	 * 
	 * @param transaction 当前的事务对象
	 * @param resourcesHolder 保存暂停资源的对象, 由{@code suspend}返回 (或{@code null}仅恢复同步)
	 */
	protected final void resume(Object transaction, SuspendedResourcesHolder resourcesHolder)
			throws TransactionException {

		if (resourcesHolder != null) {
			Object suspendedResources = resourcesHolder.suspendedResources;
			if (suspendedResources != null) {
				doResume(transaction, suspendedResources);
			}
			List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
			if (suspendedSynchronizations != null) {
				TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
				TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
				TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
				doResumeSynchronization(suspendedSynchronizations);
			}
		}
	}

	/**
	 * 内部事务启动失败后恢复外部事务.
	 */
	private void resumeAfterBeginException(
			Object transaction, SuspendedResourcesHolder suspendedResources, Throwable beginEx) {

		String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
		try {
			resume(transaction, suspendedResources);
		}
		catch (RuntimeException resumeEx) {
			logger.error(exMessage, beginEx);
			throw resumeEx;
		}
		catch (Error resumeErr) {
			logger.error(exMessage, beginEx);
			throw resumeErr;
		}
	}

	/**
	 * 暂停所有当前同步并停用当前线程的事务同步.
	 * 
	 * @return 已暂停的TransactionSynchronization对象的列表
	 */
	private List<TransactionSynchronization> doSuspendSynchronization() {
		List<TransactionSynchronization> suspendedSynchronizations =
				TransactionSynchronizationManager.getSynchronizations();
		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
			synchronization.suspend();
		}
		TransactionSynchronizationManager.clearSynchronization();
		return suspendedSynchronizations;
	}

	/**
	 * 重新激活当前线程的事务同步, 并恢复所有给定的同步.
	 * 
	 * @param suspendedSynchronizations TransactionSynchronization对象的列表
	 */
	private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
		TransactionSynchronizationManager.initSynchronization();
		for (TransactionSynchronization synchronization : suspendedSynchronizations) {
			synchronization.resume();
			TransactionSynchronizationManager.registerSynchronization(synchronization);
		}
	}


	/**
	 * 此提交实现处理参与现有事务和编程回滚的请求.
	 * 委托给{@code isRollbackOnly}, {@code doCommit} 和 {@code rollback}.
	 */
	@Override
	public final void commit(TransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			throw new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction");
		}

		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
		if (defStatus.isLocalRollbackOnly()) {
			if (defStatus.isDebug()) {
				logger.debug("Transactional code has requested rollback");
			}
			processRollback(defStatus);
			return;
		}
		if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
			if (defStatus.isDebug()) {
				logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
			}
			processRollback(defStatus);
			// 仅在最外层事务边界或明确要求时抛出UnexpectedRollbackException.
			if (status.isNewTransaction() || isFailEarlyOnGlobalRollbackOnly()) {
				throw new UnexpectedRollbackException(
						"Transaction rolled back because it has been marked as rollback-only");
			}
			return;
		}

		processCommit(defStatus);
	}

	/**
	 * 处理实际的提交.
	 * 仅回滚标志已被检查并应用.
	 * 
	 * @param status 表示事务的对象
	 * 
	 * @throws TransactionException 提交失败
	 */
	private void processCommit(DefaultTransactionStatus status) throws TransactionException {
		try {
			boolean beforeCompletionInvoked = false;
			try {
				prepareForCommit(status);
				triggerBeforeCommit(status);
				triggerBeforeCompletion(status);
				beforeCompletionInvoked = true;
				boolean globalRollbackOnly = false;
				if (status.isNewTransaction() || isFailEarlyOnGlobalRollbackOnly()) {
					globalRollbackOnly = status.isGlobalRollbackOnly();
				}
				if (status.hasSavepoint()) {
					if (status.isDebug()) {
						logger.debug("Releasing transaction savepoint");
					}
					status.releaseHeldSavepoint();
				}
				else if (status.isNewTransaction()) {
					if (status.isDebug()) {
						logger.debug("Initiating transaction commit");
					}
					doCommit(status);
				}
				// 如果有一个全局仅回滚标记, 但仍未从提交中获得相应的异常, 则抛出UnexpectedRollbackException.
				if (globalRollbackOnly) {
					throw new UnexpectedRollbackException(
							"Transaction silently rolled back because it has been marked as rollback-only");
				}
			}
			catch (UnexpectedRollbackException ex) {
				// 只能由doCommit引起
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
				throw ex;
			}
			catch (TransactionException ex) {
				// 只能由doCommit引起
				if (isRollbackOnCommitFailure()) {
					doRollbackOnCommitException(status, ex);
				}
				else {
					triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
				}
				throw ex;
			}
			catch (RuntimeException ex) {
				if (!beforeCompletionInvoked) {
					triggerBeforeCompletion(status);
				}
				doRollbackOnCommitException(status, ex);
				throw ex;
			}
			catch (Error err) {
				if (!beforeCompletionInvoked) {
					triggerBeforeCompletion(status);
				}
				doRollbackOnCommitException(status, err);
				throw err;
			}

			// 触发afterCommit回调, 抛出的异常传播给调用者, 但事务仍被视为已提交.
			try {
				triggerAfterCommit(status);
			}
			finally {
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
			}

		}
		finally {
			cleanupAfterCompletion(status);
		}
	}

	/**
	 * 此回滚的实现处理参与的现有事务.
	 * 委托给{@code doRollback} 和 {@code doSetRollbackOnly}.
	 */
	@Override
	public final void rollback(TransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			throw new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction");
		}

		DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
		processRollback(defStatus);
	}

	/**
	 * 处理实际的回滚.
	 * 已完成标志已经过检查.
	 * 
	 * @param status 表示事务的对象
	 * 
	 * @throws TransactionException 回滚失败
	 */
	private void processRollback(DefaultTransactionStatus status) {
		try {
			try {
				triggerBeforeCompletion(status);
				if (status.hasSavepoint()) {
					if (status.isDebug()) {
						logger.debug("Rolling back transaction to savepoint");
					}
					status.rollbackToHeldSavepoint();
				}
				else if (status.isNewTransaction()) {
					if (status.isDebug()) {
						logger.debug("Initiating transaction rollback");
					}
					doRollback(status);
				}
				else if (status.hasTransaction()) {
					if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
						if (status.isDebug()) {
							logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
						}
						doSetRollbackOnly(status);
					}
					else {
						if (status.isDebug()) {
							logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
						}
					}
				}
				else {
					logger.debug("Should roll back transaction but cannot - no transaction available");
				}
			}
			catch (RuntimeException ex) {
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
				throw ex;
			}
			catch (Error err) {
				triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
				throw err;
			}
			triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
		}
		finally {
			cleanupAfterCompletion(status);
		}
	}

	/**
	 * 调用{@code doRollback}, 正确处理回滚异常.
	 * 
	 * @param status 表示事务的对象
	 * @param ex 抛出的应用程序异常或错误
	 * 
	 * @throws TransactionException 回滚失败
	 */
	private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
		try {
			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback after commit exception", ex);
				}
				doRollback(status);
			}
			else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
				if (status.isDebug()) {
					logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
				}
				doSetRollbackOnly(status);
			}
		}
		catch (RuntimeException rbex) {
			logger.error("Commit exception overridden by rollback exception", ex);
			triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
			throw rbex;
		}
		catch (Error rberr) {
			logger.error("Commit exception overridden by rollback exception", ex);
			triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
			throw rberr;
		}
		triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
	}


	/**
	 * 触发{@code beforeCommit}回调.
	 * 
	 * @param status 表示事务的对象
	 */
	protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				logger.trace("Triggering beforeCommit synchronization");
			}
			TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
		}
	}

	/**
	 * 触发{@code beforeCompletion}回调.
	 * 
	 * @param status 表示事务的对象
	 */
	protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				logger.trace("Triggering beforeCompletion synchronization");
			}
			TransactionSynchronizationUtils.triggerBeforeCompletion();
		}
	}

	/**
	 * 触发{@code afterCommit}回调.
	 * 
	 * @param status 表示事务的对象
	 */
	private void triggerAfterCommit(DefaultTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				logger.trace("Triggering afterCommit synchronization");
			}
			TransactionSynchronizationUtils.triggerAfterCommit();
		}
	}

	/**
	 * 触发{@code afterCompletion}回调.
	 * 
	 * @param status 表示事务的对象
	 * @param completionStatus 来自TransactionSynchronization常量的完成状态
	 */
	private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus) {
		if (status.isNewSynchronization()) {
			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
			TransactionSynchronizationManager.clearSynchronization();
			if (!status.hasTransaction() || status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.trace("Triggering afterCompletion synchronization");
				}
				// 当前作用域没有事务或新事务 -> 立即调用afterCompletion回调
				invokeAfterCompletion(synchronizations, completionStatus);
			}
			else if (!synchronizations.isEmpty()) {
				// 参与的现有事务, 在Spring事务管理器范围之外控制 -> 尝试使用现有 (JTA) 事务注册afterCompletion回调.
				registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
			}
		}
	}

	/**
	 * 实际调用给定Spring TransactionSynchronization对象的{@code afterCompletion}方法.
	 * <p>由此抽象管理器本身调用, 或由{@code registerAfterCompletionWithExistingTransaction}回调的特殊实现调用.
	 * 
	 * @param synchronizations TransactionSynchronization对象集合
	 * @param completionStatus 来自TransactionSynchronization接口中的常量的完成状态
	 */
	protected final void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
		TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * 完成后清理, 必要时清除同步, 并调用doCleanupAfterCompletion.
	 * 
	 * @param status 表示事务的对象
	 */
	private void cleanupAfterCompletion(DefaultTransactionStatus status) {
		status.setCompleted();
		if (status.isNewSynchronization()) {
			TransactionSynchronizationManager.clear();
		}
		if (status.isNewTransaction()) {
			doCleanupAfterCompletion(status.getTransaction());
		}
		if (status.getSuspendedResources() != null) {
			if (status.isDebug()) {
				logger.debug("Resuming suspended transaction after completion of inner transaction");
			}
			resume(status.getTransaction(), (SuspendedResourcesHolder) status.getSuspendedResources());
		}
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented in subclasses
	//---------------------------------------------------------------------

	/**
	 * 返回当前事务状态的事务对象.
	 * <p>返回的对象通常特定于具体的事务管理器实现, 以可修改的方式携带相应的事务状态.
	 * 此对象将直接或作为DefaultTransactionStatus实例的一部分传递到其他模板方法 (e.g. doBegin 和 doCommit).
	 * <p>返回的对象应包含有关任何现有事务的信息, 即在事务管理器上当前{@code getTransaction}调用之前已启动的事务.
	 * 因此, {@code doGetTransaction}实现通常会查找现有事务, 并在返回的事务对象中存储相应的状态.
	 * 
	 * @return 当前事务对象
	 * @throws org.springframework.transaction.CannotCreateTransactionException 如果没有事务支持
	 * @throws TransactionException 如果查找或系统错误
	 */
	protected abstract Object doGetTransaction() throws TransactionException;

	/**
	 * 检查给定的事务对象是否指示现有事务 (即已启动的事务).
	 * <p>将根据新事务的指定传播行为来评估结果.
	 * 现有的事务可能会被暂停 (如果是PROPAGATION_REQUIRES_NEW), 或者新事务可能参与现有事务 (如果是PROPAGATION_REQUIRED).
	 * <p>假设通常不支持参与现有事务, 默认实现返回{@code false}. 当然鼓励子类提供这样的支持.
	 * 
	 * @param transaction doGetTransaction返回的事务对象
	 * 
	 * @return 如果存在现有事务
	 * @throws TransactionException 如果系统错误
	 */
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return false;
	}

	/**
	 * 返回是否对嵌套事务使用保存点.
	 * <p>默认{@code true}, 这会导致委托给DefaultTransactionStatus来创建和保存保存点.
	 * 如果事务对象未实现SavepointManager接口, 则将抛出NestedTransactionNotSupportedException.
	 * 否则, 将要求SavepointManager创建一个新的保存点来划分嵌套事务的开始.
	 * <p>子类可以覆盖它以返回{@code false}, 从而导致对{@code doBegin}的进一步调用 - 在现有事务的上下文中.
	 * {@code doBegin}实现需要在这种情况下相应地处理这个问题. 例如, 这适用于JTA.
	 */
	protected boolean useSavepointForNestedTransaction() {
		return true;
	}

	/**
	 * 根据给定的事务定义启动具有语义的新事务.
	 * 不必关心应用传播行为, 因为这已经由此抽象管理器处理.
	 * <p>当事务管理器决定实际启动新事务时, 将调用此方法.
	 * 之前没有任何事务, 或者之前的事务已被暂停.
	 * <p>特殊情况是没有保存点的嵌套事务: 如果{@code useSavepointForNestedTransaction()}返回"false",
	 * 则将调用此方法以在必要时启动嵌套事务.
	 * 在这样的上下文中, 将存在活动的事务: 此方法的实现必须检测, 并启动适当的嵌套事务.
	 * 
	 * @param transaction {@code doGetTransaction}返回的事务对象
	 * @param definition TransactionDefinition实例, 描述传播行为, 隔离级别, 只读标志, 超时, 和事务名称
	 * 
	 * @throws TransactionException 如果创建或系统错误
	 */
	protected abstract void doBegin(Object transaction, TransactionDefinition definition)
			throws TransactionException;

	/**
	 * 暂停当前​​事务的资源. 事务同步已经暂停.
	 * <p>假设通常不支持事务暂停, 默认实现会抛出TransactionSuspensionNotSupportedException.
	 * 
	 * @param transaction {@code doGetTransaction}返回的事务对象
	 * 
	 * @return 保存暂停资源的对象 (将保留以将其传递给doResume)
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * 如果事务管理器实现不支持暂停
	 * @throws TransactionException 如果系统错误
	 */
	protected Object doSuspend(Object transaction) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * 恢复当前事务的资源. 事务同步将在之后恢复.
	 * <p>假设通常不支持事务暂停, 默认实现会抛出TransactionSuspensionNotSupportedException.
	 * 
	 * @param transaction {@code doGetTransaction}返回的事务对象
	 * @param suspendedResources 保存挂起资源的对象, 由doSuspend返回
	 * 
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * 如果事务管理器实现不支持恢复
	 * @throws TransactionException 如果系统错误
	 */
	protected void doResume(Object transaction, Object suspendedResources) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * 返回是否在以全局方式标记为仅回滚的事务上调用{@code doCommit}.
	 * <p>如果应用程序本地通过TransactionStatus将事务设置为仅回滚, 则不适用, 但仅适用于通过事务协调器标记为仅回滚的事务本身.
	 * <p>默认"false": 本地事务策略通常不会在事务本身中保留仅回滚标记, 因此它们无法处理作为事务提交的一部分的仅回滚事务.
	 * 因此, 在这种情况下, AbstractPlatformTransactionManager将触发回滚, 之后抛出UnexpectedRollbackException.
	 * <p>如果具体事务管理器期望{@code doCommit}调用, 甚至是仅回滚的事务, 覆盖这个方法并返回"true",
	 * 允许在那里进行特殊处理.
	 * 例如, 这将是JTA的情况, 其中{@code UserTransaction.commit}将检查只读标志本身, 并抛出相应的RollbackException,
	 * 其中可能包括特定原因 (例如事务超时).
	 * <p>如果此方法返回"true", 但{@code doCommit}实现不会抛出异常, 则此事务管理器将抛出UnexpectedRollbackException本身.
	 * 这不应该是典型的情况; 主要检查是否覆盖行为不端的JTA提供者, 即使调用代码未请求回滚, 也会静默回滚.
	 */
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return false;
	}

	/**
	 * 准备提交, 在{@code beforeCommit}同步回调发生之前执行.
	 * <p>请注意, 异常将传播到提交调用者并导致事务回滚.
	 * 
	 * @param status 事务的状态表示
	 * 
	 * @throws RuntimeException 如果有错误; 将<b>传播给调用者</b> (note: 不要在这里抛出TransactionException子类!)
	 */
	protected void prepareForCommit(DefaultTransactionStatus status) {
	}

	/**
	 * 执行给定事务的实际提交.
	 * <p>实现不需要检查"new transaction"标志或仅回滚标志; 这已经在以前处理过了.
	 * 通常, 将对传入状态中包含的事务对象执行直接提交.
	 * 
	 * @param status 事务的状态表示
	 * 
	 * @throws TransactionException 如果提交或系统错误
	 */
	protected abstract void doCommit(DefaultTransactionStatus status) throws TransactionException;

	/**
	 * 执行给定事务的实际回滚.
	 * <p>实现不需要检查"new transaction"标志; 这已经在以前处理过了.
	 * 通常, 将对传入状态中包含的事务对象执行直接回滚.
	 * 
	 * @param status 事务的状态表示
	 * 
	 * @throws TransactionException 如果系统错误
	 */
	protected abstract void doRollback(DefaultTransactionStatus status) throws TransactionException;

	/**
	 * 设置给定的事务仅回滚. 仅在当前事务参与现有事务时才调用回滚.
	 * <p>默认实现抛出IllegalTransactionStateException, 假设通常不支持参与现有事务.
	 * 当然鼓励子类提供这样的支持.
	 * 
	 * @param status 事务的状态表示
	 * 
	 * @throws TransactionException 如果系统错误
	 */
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
		throw new IllegalTransactionStateException(
				"Participating in existing transactions is not supported - when 'isExistingTransaction' " +
				"returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
	}

	/**
	 * 使用现有事务注册给定的事务同步列表.
	 * <p>当Spring事务管理器的控制以及所有Spring事务同步结束时调用, 而事务尚未完成.
	 * 例如, 这是参与现有JTA或EJB CMT事务的情况.
	 * <p>默认实现只是立即调用{@code afterCompletion}方法, 传入"STATUS_UNKNOWN".
	 * 如果无法确定外部事务的实际结果, 这是能做的最好的事情.
	 * 
	 * @param transaction {@code doGetTransaction}返回的事务对象
	 * @param synchronizations TransactionSynchronization对象集合
	 * 
	 * @throws TransactionException 如果系统错误
	 */
	protected void registerAfterCompletionWithExistingTransaction(
			Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {

		logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
				"processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
		invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
	}

	/**
	 * 事务完成后清理资源.
	 * <p>在{@code doCommit}和{@code doRollback}执行后调用, 不管结果如何. 默认实现什么都不做.
	 * <p>不应抛出任何异常, 只是发出错误警告.
	 * 
	 * @param transaction {@code doGetTransaction}返回的事务对象
	 */
	protected void doCleanupAfterCompletion(Object transaction) {
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依靠默认序列化; 只是在反序列化后初始化状态.
		ois.defaultReadObject();

		// 初始化transient字段.
		this.logger = LogFactory.getLog(getClass());
	}


	/**
	 * 暂停资源的保存器.
	 * 由{@code suspend}和{@code resume}在内部使用.
	 */
	protected static class SuspendedResourcesHolder {

		private final Object suspendedResources;

		private List<TransactionSynchronization> suspendedSynchronizations;

		private String name;

		private boolean readOnly;

		private Integer isolationLevel;

		private boolean wasActive;

		private SuspendedResourcesHolder(Object suspendedResources) {
			this.suspendedResources = suspendedResources;
		}

		private SuspendedResourcesHolder(
				Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations,
				String name, boolean readOnly, Integer isolationLevel, boolean wasActive) {
			this.suspendedResources = suspendedResources;
			this.suspendedSynchronizations = suspendedSynchronizations;
			this.name = name;
			this.readOnly = readOnly;
			this.isolationLevel = isolationLevel;
			this.wasActive = wasActive;
		}
	}
}
