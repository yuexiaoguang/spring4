package org.springframework.transaction.support;

import java.lang.reflect.UndeclaredThrowableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

/**
 * 模板类, 简化了程序化事务划分和事务异常处理.
 *
 * <p>中心方法是{@link #execute}, 支持实现{@link TransactionCallback}接口的事务代码.
 * 此模板处理事务生命周期和可能的异常, 因此TransactionCallback实现和调用代码都不需要显式处理事务.
 *
 * <p>典型用法: 允许编写使用JDBC DataSource等资源但本身不具有事务感知功能的低级数据访问对象.
 * 相反, 它们可以隐式参与由使用此类的更高级应用程序服务处理的事务, 通过内部类回调对象调用低级服务.
 *
 * <p>可以通过使用事务管理器引用直接实例化在服务实现中使用, 或者在应用程序上下文中准备, 并作为bean引用传递给服务.
 * Note: 事务管理器应始终在应用程序上下文中配置为bean: 在第一种情况下直接提供给服务, 在第二种情况下提供给准备好的模板.
 *
 * <p>支持按名称设置传播行为和隔离级别, 以便在上下文定义中进行配置.
 */
@SuppressWarnings("serial")
public class TransactionTemplate extends DefaultTransactionDefinition
		implements TransactionOperations, InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private PlatformTransactionManager transactionManager;


	/**
	 * <p>Note: 需要在任何{@code execute}调用之前设置PlatformTransactionManager.
	 */
	public TransactionTemplate() {
	}

	/**
	 * @param transactionManager 要使用的事务管理策略
	 */
	public TransactionTemplate(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 使用给定的事务管理器构造一个新的TransactionTemplate, 从给定的事务定义中获取其默认设置.
	 * 
	 * @param transactionManager 要使用的事务管理策略
	 * @param transactionDefinition 要从中复制默认设置的事务定义. 仍可以将本地属性设置为更改值.
	 */
	public TransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
		super(transactionDefinition);
		this.transactionManager = transactionManager;
	}


	/**
	 * 设置要使用的事务管理策略.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回要使用的事务管理策略.
	 */
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.transactionManager == null) {
			throw new IllegalArgumentException("Property 'transactionManager' is required");
		}
	}


	@Override
	public <T> T execute(TransactionCallback<T> action) throws TransactionException {
		if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager) {
			return ((CallbackPreferringPlatformTransactionManager) this.transactionManager).execute(this, action);
		}
		else {
			TransactionStatus status = this.transactionManager.getTransaction(this);
			T result;
			try {
				result = action.doInTransaction(status);
			}
			catch (RuntimeException ex) {
				// 事务代码抛出应用程序异常 -> 回滚
				rollbackOnException(status, ex);
				throw ex;
			}
			catch (Error err) {
				// 事务代码抛出错误 -> 回滚
				rollbackOnException(status, err);
				throw err;
			}
			catch (Throwable ex) {
				// 事务代码抛出异常 -> 回滚
				rollbackOnException(status, ex);
				throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
			}
			this.transactionManager.commit(status);
			return result;
		}
	}

	/**
	 * 执行回滚, 正确处理回滚异常.
	 * 
	 * @param status 表示事务的对象
	 * @param ex 抛出的应用程序异常或错误
	 * 
	 * @throws TransactionException 回滚错误
	 */
	private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
		logger.debug("Initiating transaction rollback on application exception", ex);
		try {
			this.transactionManager.rollback(status);
		}
		catch (TransactionSystemException ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			ex2.initApplicationException(ex);
			throw ex2;
		}
		catch (RuntimeException ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			throw ex2;
		}
		catch (Error err) {
			logger.error("Application exception overridden by rollback error", ex);
			throw err;
		}
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (super.equals(other) && (!(other instanceof TransactionTemplate) ||
				getTransactionManager() == ((TransactionTemplate) other).getTransactionManager())));
	}

}
