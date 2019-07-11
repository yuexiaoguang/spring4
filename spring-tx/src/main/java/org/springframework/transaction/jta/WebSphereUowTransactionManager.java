package org.springframework.transaction.jta;

import java.util.List;
import javax.naming.NamingException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 特定于WebSphere的PlatformTransactionManager实现, 它委托给从WebSphere的JNDI环境获得的{@link com.ibm.wsspi.uow.UOWManager}实例.
 * 这允许Spring以完全符合官方支持的WebSphere API的方式利用WebSphere事务协调器的全部功能, 包括事务暂停.
 *
 * <p>此类实现的{@link CallbackPreferringPlatformTransactionManager}接口
 * 表明调用者最好通过{@link #execute}方法传递{@link TransactionCallback},
 * 它将通过基于回调的WebSphere UOWManager API, 而不是通过标准JTA API (UserTransaction / TransactionManager).
 * 这避免了在WebSphere上使用非public {@code javax.transaction.TransactionManager} API, 保持在受支持的WebSphere API边界内.
 *
 * <p>此事务管理器实现源自Spring的标准{@link JtaTransactionManager},
 * 继承了使用{@code getTransaction}/{@code commit}/{@code rollback}调用通过JTA UserTransaction句柄支持程序化事务划分的功能,
 * 用于不使用基于TransactionCallback的{@link #execute}方法的调用者.
 * 但是, 此{@code getTransaction}<i>不支持</i>事务暂停 (除非明确指定{@link #setTransactionManager}引用, 尽管有官方的WebSphere建议).
 * 对于可能需要事务暂停的代码, 请使用{@link #execute}样式.
 *
 * <p>此事务管理器与WebSphere 6.1.0.9及更高版本兼容.
 * UOWManager的默认JNDI位置是 "java:comp/websphere/UOWManager".
 * 如果位置根据WebSphere文档而不同, 只需通过此事务管理器的"uowManagerName" bean属性指定实际位置.
 *
 * <p><b>NOTE: 这个JtaTransactionManager旨在改进Spring的特定事务划分行为.
 * 它将与持久化提供者中独立配置的WebSphere事务策略共存, 而无需以任何方式专门连接这些设置.</b>
 */
@SuppressWarnings("serial")
public class WebSphereUowTransactionManager extends JtaTransactionManager
		implements CallbackPreferringPlatformTransactionManager {

	/**
	 * WebSphere UOWManager的默认JNDI位置.
	 */
	public static final String DEFAULT_UOW_MANAGER_NAME = "java:comp/websphere/UOWManager";


	private UOWManager uowManager;

	private String uowManagerName;


	public WebSphereUowTransactionManager() {
		setAutodetectTransactionManager(false);
	}

	/**
	 * @param uowManager 用作直接引用的WebSphere UOWManager
	 */
	public WebSphereUowTransactionManager(UOWManager uowManager) {
		this();
		this.uowManager = uowManager;
	}


	/**
	 * 设置WebSphere UOWManager为直接引用.
	 * <p>通常只用于测试设置; 在Java EE环境中, 将始终从JNDI获取UOWManager.
	 */
	public void setUowManager(UOWManager uowManager) {
		this.uowManager = uowManager;
	}

	/**
	 * 设置WebSphere UOWManager的JNDI名称.
	 * 如果未设置, 则使用默认的"java:comp/websphere/UOWManager".
	 */
	public void setUowManagerName(String uowManagerName) {
		this.uowManagerName = uowManagerName;
	}


	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		initUserTransactionAndTransactionManager();

		// Fetch UOWManager handle from JNDI, if necessary.
		if (this.uowManager == null) {
			if (this.uowManagerName != null) {
				this.uowManager = lookupUowManager(this.uowManagerName);
			}
			else {
				this.uowManager = lookupDefaultUowManager();
			}
		}
	}

	/**
	 * 通过配置的名称在JNDI中查找WebSphere UOWManager.
	 * 
	 * @param uowManagerName UOWManager的JNDI名称
	 * 
	 * @return UOWManager对象
	 * @throws TransactionSystemException 如果JNDI查找失败
	 */
	protected UOWManager lookupUowManager(String uowManagerName) throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving WebSphere UOWManager from JNDI location [" + uowManagerName + "]");
			}
			return getJndiTemplate().lookup(uowManagerName, UOWManager.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"WebSphere UOWManager is not available at JNDI location [" + uowManagerName + "]", ex);
		}
	}

	/**
	 * 从默认的JNDI位置"java:comp/websphere/UOWManager"获取WebSphere UOWManager.
	 * 
	 * @return UOWManager对象
	 * @throws TransactionSystemException 如果JNDI查找失败
	 */
	protected UOWManager lookupDefaultUowManager() throws TransactionSystemException {
		try {
			logger.debug("Retrieving WebSphere UOWManager from default JNDI location [" + DEFAULT_UOW_MANAGER_NAME + "]");
			return getJndiTemplate().lookup(DEFAULT_UOW_MANAGER_NAME, UOWManager.class);
		}
		catch (NamingException ex) {
			logger.debug("WebSphere UOWManager is not available at default JNDI location [" +
					DEFAULT_UOW_MANAGER_NAME + "] - falling back to UOWManagerFactory lookup");
			return UOWManagerFactory.getUOWManager();
		}
	}

	/**
	 * 在UOWManager上将同步注册为插入的JTA同步.
	 */
	@Override
	protected void doRegisterAfterCompletionWithJtaTransaction(
			JtaTransactionObject txObject, List<TransactionSynchronization> synchronizations) {

		this.uowManager.registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
	}

	/**
	 * 返回{@code true}, 因为如果MessageEndpointFactory的{@code isDeliveryTransacted}方法返回{@code true},
	 * 则WebSphere ResourceAdapters (在JNDI中公开) 会隐式执行事务登记.
	 * 在这种情况下, 只需跳过{@link #createTransaction}调用.
	 */
	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return true;
	}


	@Override
	public <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) throws TransactionException {
		if (definition == null) {
			// 如果没有给出事务定义, 则使用默认值.
			definition = new DefaultTransactionDefinition();
		}

		if (definition.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", definition.getTimeout());
		}
		int pb = definition.getPropagationBehavior();
		boolean existingTx = (this.uowManager.getUOWStatus() != UOWSynchronizationRegistry.UOW_STATUS_NONE &&
				this.uowManager.getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);

		int uowType = UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION;
		boolean joinTx = false;
		boolean newSynch = false;

		if (existingTx) {
			if (pb == TransactionDefinition.PROPAGATION_NEVER) {
				throw new IllegalTransactionStateException(
						"Transaction propagation 'never' but existing transaction found");
			}
			if (pb == TransactionDefinition.PROPAGATION_NESTED) {
				throw new NestedTransactionNotSupportedException(
						"Transaction propagation 'nested' not supported for WebSphere UOW transactions");
			}
			if (pb == TransactionDefinition.PROPAGATION_SUPPORTS ||
					pb == TransactionDefinition.PROPAGATION_REQUIRED ||
					pb == TransactionDefinition.PROPAGATION_MANDATORY) {
				joinTx = true;
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
			else if (pb == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
				uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
				newSynch = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			}
			else {
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
		}
		else {
			if (pb == TransactionDefinition.PROPAGATION_MANDATORY) {
				throw new IllegalTransactionStateException(
						"Transaction propagation 'mandatory' but no existing transaction found");
			}
			if (pb == TransactionDefinition.PROPAGATION_SUPPORTS ||
					pb == TransactionDefinition.PROPAGATION_NOT_SUPPORTED ||
					pb == TransactionDefinition.PROPAGATION_NEVER) {
				uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
				newSynch = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			}
			else {
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
		}

		boolean debug = logger.isDebugEnabled();
		if (debug) {
			logger.debug("Creating new transaction with name [" + definition.getName() + "]: " + definition);
		}
		SuspendedResourcesHolder suspendedResources = (!joinTx ? suspend(null) : null);
		UOWActionAdapter<T> action = null;
		try {
			if (definition.getTimeout() > TransactionDefinition.TIMEOUT_DEFAULT) {
				this.uowManager.setUOWTimeout(uowType, definition.getTimeout());
			}
			if (debug) {
				logger.debug("Invoking WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			action = new UOWActionAdapter<T>(
					definition, callback, (uowType == UOWManager.UOW_TYPE_GLOBAL_TRANSACTION), !joinTx, newSynch, debug);
			this.uowManager.runUnderUOW(uowType, joinTx, action);
			if (debug) {
				logger.debug("Returned from WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			return action.getResult();
		}
		catch (UOWException ex) {
			TransactionSystemException tse =
					new TransactionSystemException("UOWManager transaction processing failed", ex);
			Throwable appEx = action.getException();
			if (appEx != null) {
				logger.error("Application exception overridden by rollback exception", appEx);
				tse.initApplicationException(appEx);
			}
			throw tse;
		}
		catch (UOWActionException ex) {
			TransactionSystemException tse =
					new TransactionSystemException("UOWManager threw unexpected UOWActionException", ex);
			Throwable appEx = action.getException();
			if (appEx != null) {
				logger.error("Application exception overridden by rollback exception", appEx);
				tse.initApplicationException(appEx);
			}
			throw tse;
		}
		finally {
			if (suspendedResources != null) {
				resume(null, suspendedResources);
			}
		}
	}


	/**
	 * 在WebSphere UOWAction形状内执行给定Spring事务的适配器.
	 */
	private class UOWActionAdapter<T> implements UOWAction, SmartTransactionObject {

		private final TransactionDefinition definition;

		private final TransactionCallback<T> callback;

		private final boolean actualTransaction;

		private final boolean newTransaction;

		private final boolean newSynchronization;

		private boolean debug;

		private T result;

		private Throwable exception;

		public UOWActionAdapter(TransactionDefinition definition, TransactionCallback<T> callback,
				boolean actualTransaction, boolean newTransaction, boolean newSynchronization, boolean debug) {

			this.definition = definition;
			this.callback = callback;
			this.actualTransaction = actualTransaction;
			this.newTransaction = newTransaction;
			this.newSynchronization = newSynchronization;
			this.debug = debug;
		}

		@Override
		public void run() {
			DefaultTransactionStatus status = prepareTransactionStatus(
					this.definition, (this.actualTransaction ? this : null),
					this.newTransaction, this.newSynchronization, this.debug, null);
			try {
				this.result = this.callback.doInTransaction(status);
				triggerBeforeCommit(status);
			}
			catch (Throwable ex) {
				this.exception = ex;
				if (status.isDebug()) {
					logger.debug("Rolling back on application exception from transaction callback", ex);
				}
				uowManager.setRollbackOnly();
			}
			finally {
				if (status.isLocalRollbackOnly()) {
					if (status.isDebug()) {
						logger.debug("Transaction callback has explicitly requested rollback");
					}
					uowManager.setRollbackOnly();
				}
				triggerBeforeCompletion(status);
				if (status.isNewSynchronization()) {
					List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
					TransactionSynchronizationManager.clear();
					if (!synchronizations.isEmpty()) {
						uowManager.registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
					}
				}
			}
		}

		public T getResult() {
			if (this.exception != null) {
				ReflectionUtils.rethrowRuntimeException(this.exception);
			}
			return this.result;
		}

		public Throwable getException() {
			return this.exception;
		}

		@Override
		public boolean isRollbackOnly() {
			return uowManager.getRollbackOnly();
		}

		@Override
		public void flush() {
			TransactionSynchronizationUtils.triggerFlush();
		}
	}

}
