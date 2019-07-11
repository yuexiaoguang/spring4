package org.springframework.transaction.jta;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;

/**
 * BEA WebLogic (9.0及更高版本)的特殊{@link JtaTransactionManager}变体.
 * 在WebLogic的事务协调器上支持Spring的事务定义的全部功能, <i>超出标准JTA</i>:
 * 事务名称, 每事务隔离级别, 以及在所有情况下正确恢复事务.
 *
 * <p>使用WebLogic的特殊{@code begin(name)}方法启动JTA事务, 为了使<b>Spring驱动的事务在WebLogic的事务监视器中可见</b>.
 * 对于Spring的声明性事务, 暴露的名称 (默认情况下)将是完全限定的类名 + "." + 方法名.
 *
 * <p>通过WebLogic的相应JTA事务属性"ISOLATION LEVEL"支持<b>每事务隔离级别</b>.
 * 这将将指定的隔离级别 (e.g. ISOLATION_SERIALIZABLE) 应用于参与给定事务的所有JDBC连接.
 *
 * <p>如果标准JTA恢复失败, 则调用WebLogic的特殊{@code forceResume}方法, 如果目标事务标记为仅回滚, <b>也恢复</b>.
 * 如果不是首先依赖于事务暂停的这个特性, Spring的标准JtaTransactionManager也会表现得很好.
 *
 * <p>默认情况下, JTA UserTransaction 和TransactionManager句柄直接从WebLogic的{@code TransactionHelper}获取.
 * 这可以通过指定"userTransaction"/"userTransactionName"和"transactionManager"/"transactionManagerName"来覆盖,
 * 传入现有句柄或指定相应的JNDI位置以查找.
 *
 * <p><b>NOTE: 这个JtaTransactionManager旨在改进Spring的特定事务划分行为.
 * 它将与持久化提供者中独立配置的WebLogic事务策略共存, 而无需以任何方式专门连接这些设置.</b>
 */
@SuppressWarnings("serial")
public class WebLogicJtaTransactionManager extends JtaTransactionManager {

	private static final String USER_TRANSACTION_CLASS_NAME = "weblogic.transaction.UserTransaction";

	private static final String CLIENT_TRANSACTION_MANAGER_CLASS_NAME = "weblogic.transaction.ClientTransactionManager";

	private static final String TRANSACTION_CLASS_NAME = "weblogic.transaction.Transaction";

	private static final String TRANSACTION_HELPER_CLASS_NAME = "weblogic.transaction.TransactionHelper";

	private static final String ISOLATION_LEVEL_KEY = "ISOLATION LEVEL";


	private boolean weblogicUserTransactionAvailable;

	private Method beginWithNameMethod;

	private Method beginWithNameAndTimeoutMethod;

	private boolean weblogicTransactionManagerAvailable;

	private Method forceResumeMethod;

	private Method setPropertyMethod;

	private Object transactionHelper;


	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		super.afterPropertiesSet();
		loadWebLogicTransactionClasses();
	}

	@Override
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		loadWebLogicTransactionHelper();
		try {
			logger.debug("Retrieving JTA UserTransaction from WebLogic TransactionHelper");
			Method getUserTransactionMethod = this.transactionHelper.getClass().getMethod("getUserTransaction");
			return (UserTransaction) getUserTransactionMethod.invoke(this.transactionHelper);
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"WebLogic's TransactionHelper.getUserTransaction() method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not invoke WebLogic's TransactionHelper.getUserTransaction() method", ex);
		}
	}

	@Override
	protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
		loadWebLogicTransactionHelper();
		try {
			logger.debug("Retrieving JTA TransactionManager from WebLogic TransactionHelper");
			Method getTransactionManagerMethod = this.transactionHelper.getClass().getMethod("getTransactionManager");
			return (TransactionManager) getTransactionManagerMethod.invoke(this.transactionHelper);
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"WebLogic's TransactionHelper.getTransactionManager() method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not invoke WebLogic's TransactionHelper.getTransactionManager() method", ex);
		}
	}

	private void loadWebLogicTransactionHelper() throws TransactionSystemException {
		if (this.transactionHelper == null) {
			try {
				Class<?> transactionHelperClass = getClass().getClassLoader().loadClass(TRANSACTION_HELPER_CLASS_NAME);
				Method getTransactionHelperMethod = transactionHelperClass.getMethod("getTransactionHelper");
				this.transactionHelper = getTransactionHelperMethod.invoke(null);
				logger.debug("WebLogic TransactionHelper found");
			}
			catch (InvocationTargetException ex) {
				throw new TransactionSystemException(
						"WebLogic's TransactionHelper.getTransactionHelper() method failed", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new TransactionSystemException(
						"Could not initialize WebLogicJtaTransactionManager because WebLogic API classes are not available",
						ex);
			}
		}
	}

	private void loadWebLogicTransactionClasses() throws TransactionSystemException {
		try {
			Class<?> userTransactionClass = getClass().getClassLoader().loadClass(USER_TRANSACTION_CLASS_NAME);
			this.weblogicUserTransactionAvailable = userTransactionClass.isInstance(getUserTransaction());
			if (this.weblogicUserTransactionAvailable) {
				this.beginWithNameMethod = userTransactionClass.getMethod("begin", String.class);
				this.beginWithNameAndTimeoutMethod = userTransactionClass.getMethod("begin", String.class, int.class);
				logger.info("Support for WebLogic transaction names available");
			}
			else {
				logger.info("Support for WebLogic transaction names not available");
			}

			// 获取WebLogic ClientTransactionManager接口.
			Class<?> transactionManagerClass =
					getClass().getClassLoader().loadClass(CLIENT_TRANSACTION_MANAGER_CLASS_NAME);
			logger.debug("WebLogic ClientTransactionManager found");

			this.weblogicTransactionManagerAvailable = transactionManagerClass.isInstance(getTransactionManager());
			if (this.weblogicTransactionManagerAvailable) {
				Class<?> transactionClass = getClass().getClassLoader().loadClass(TRANSACTION_CLASS_NAME);
				this.forceResumeMethod = transactionManagerClass.getMethod("forceResume", Transaction.class);
				this.setPropertyMethod = transactionClass.getMethod("setProperty", String.class, Serializable.class);
				logger.debug("Support for WebLogic forceResume available");
			}
			else {
				logger.warn("Support for WebLogic forceResume not available");
			}
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not initialize WebLogicJtaTransactionManager because WebLogic API classes are not available",
					ex);
		}
	}


	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
			throws NotSupportedException, SystemException {

		int timeout = determineTimeout(definition);

		// 将事务名称应用于WebLogic事务.
		if (this.weblogicUserTransactionAvailable && definition.getName() != null) {
			try {
				if (timeout > TransactionDefinition.TIMEOUT_DEFAULT) {
					/*
					weblogic.transaction.UserTransaction wut = (weblogic.transaction.UserTransaction) ut;
					wut.begin(definition.getName(), timeout);
					*/
					this.beginWithNameAndTimeoutMethod.invoke(txObject.getUserTransaction(), definition.getName(), timeout);
				}
				else {
					/*
					weblogic.transaction.UserTransaction wut = (weblogic.transaction.UserTransaction) ut;
					wut.begin(definition.getName());
					*/
					this.beginWithNameMethod.invoke(txObject.getUserTransaction(), definition.getName());
				}
			}
			catch (InvocationTargetException ex) {
				throw new TransactionSystemException(
						"WebLogic's UserTransaction.begin() method failed", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new TransactionSystemException(
						"Could not invoke WebLogic's UserTransaction.begin() method", ex);
			}
		}
		else {
			// 没有可用的WebLogic UserTransaction或没有指定事务名称 -> 标准JTA开始调用.
			applyTimeout(txObject, timeout);
			txObject.getUserTransaction().begin();
		}

		// 通过相应的WebLogic事务属性指定隔离级别.
		if (this.weblogicTransactionManagerAvailable) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				try {
					Transaction tx = getTransactionManager().getTransaction();
					Integer isolationLevel = definition.getIsolationLevel();
					/*
					weblogic.transaction.Transaction wtx = (weblogic.transaction.Transaction) tx;
					wtx.setProperty(ISOLATION_LEVEL_KEY, isolationLevel);
					*/
					this.setPropertyMethod.invoke(tx, ISOLATION_LEVEL_KEY, isolationLevel);
				}
				catch (InvocationTargetException ex) {
					throw new TransactionSystemException(
							"WebLogic's Transaction.setProperty(String, Serializable) method failed", ex.getTargetException());
				}
				catch (Exception ex) {
					throw new TransactionSystemException(
							"Could not invoke WebLogic's Transaction.setProperty(String, Serializable) method", ex);
				}
			}
		}
		else {
			applyIsolationLevel(txObject, definition.getIsolationLevel());
		}
	}

	@Override
	protected void doJtaResume(JtaTransactionObject txObject, Object suspendedTransaction)
			throws InvalidTransactionException, SystemException {

		try {
			getTransactionManager().resume((Transaction) suspendedTransaction);
		}
		catch (InvalidTransactionException ex) {
			if (!this.weblogicTransactionManagerAvailable) {
				throw ex;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Standard JTA resume threw InvalidTransactionException: " + ex.getMessage() +
					" - trying WebLogic JTA forceResume");
			}
			/*
			weblogic.transaction.TransactionManager wtm =
					(weblogic.transaction.TransactionManager) getTransactionManager();
			wtm.forceResume(suspendedTransaction);
			*/
			try {
				this.forceResumeMethod.invoke(getTransactionManager(), suspendedTransaction);
			}
			catch (InvocationTargetException ex2) {
				throw new TransactionSystemException(
						"WebLogic's TransactionManager.forceResume(Transaction) method failed", ex2.getTargetException());
			}
			catch (Exception ex2) {
				throw new TransactionSystemException(
						"Could not access WebLogic's TransactionManager.forceResume(Transaction) method", ex2);
			}
		}
	}

	@Override
	public Transaction createTransaction(String name, int timeout) throws NotSupportedException, SystemException {
		if (this.weblogicUserTransactionAvailable && name != null) {
			try {
				if (timeout >= 0) {
					this.beginWithNameAndTimeoutMethod.invoke(getUserTransaction(), name, timeout);
				}
				else {
					this.beginWithNameMethod.invoke(getUserTransaction(), name);
				}
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof NotSupportedException) {
					throw (NotSupportedException) ex.getTargetException();
				}
				else if (ex.getTargetException() instanceof SystemException) {
					throw (SystemException) ex.getTargetException();
				}
				else if (ex.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException) ex.getTargetException();
				}
				else {
					throw new SystemException(
							"WebLogic's begin() method failed with an unexpected error: " + ex.getTargetException());
				}
			}
			catch (Exception ex) {
				throw new SystemException("Could not invoke WebLogic's UserTransaction.begin() method: " + ex);
			}
			return new ManagedTransactionAdapter(getTransactionManager());
		}

		else {
			// 没有指定名称 - 标准JTA就足够了.
			return super.createTransaction(name, timeout);
		}
	}

}
