package org.springframework.orm.hibernate5;

import java.lang.reflect.Method;
import java.util.Map;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PessimisticLockException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.service.UnknownServiceException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 用于Hibernate会话处理.
 * 还提供异常转换支持.
 *
 * <p>由{@link HibernateTransactionManager}内部使用.
 * 也可以直接在应用程序代码中使用.
 */
public abstract class SessionFactoryUtils {

	/**
	 * 清理Hibernate会话的TransactionSynchronization对象的顺序值.
	 * 返回{@code DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100}, 以在JDBC连接清理之前执行会话清理.
	 */
	public static final int SESSION_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);


	private static Method getFlushMode;

	static {
		try {
			// Hibernate 5.2+ getHibernateFlushMode()
			getFlushMode = Session.class.getMethod("getHibernateFlushMode");
		}
		catch (NoSuchMethodException ex) {
			try {
				// Hibernate 5.0/5.1 getFlushMode() with FlushMode return type
				getFlushMode = Session.class.getMethod("getFlushMode");
			}
			catch (NoSuchMethodException ex2) {
				throw new IllegalStateException("No compatible Hibernate getFlushMode signature found", ex2);
			}
		}
		// 检查它是否是Hibernate FlushMode类型, 而不是JPA...
		Assert.state(FlushMode.class == getFlushMode.getReturnType(), "Could not find Hibernate getFlushMode method");
	}


	/**
	 * 获取原生的Hibernate FlushMode, 适配为Hibernate 5.0/5.1 和 5.2+.
	 * 
	 * @param session 从中获取刷新模式的Hibernate Session
	 * 
	 * @return the FlushMode (never {@code null})
	 */
	static FlushMode getFlushMode(Session session) {
		return (FlushMode) ReflectionUtils.invokeMethod(getFlushMode, session);
	}

	/**
	 * 在给定的Hibernate Session上触发刷新, 转换常规{@link HibernateException}实例
	 * 以及Hibernate 5.2的{@link PersistenceException}包装器.
	 * 
	 * @param session 要刷新的Hibernate会话
	 * @param synch 是否由事务同步触发此刷新
	 * 
	 * @throws DataAccessException 刷新失败
	 */
	static void flush(Session session, boolean synch) throws DataAccessException {
		if (synch) {
			logger.debug("Flushing Hibernate Session on transaction synchronization");
		}
		else {
			logger.debug("Flushing Hibernate Session on explicit request");
		}
		try {
			session.flush();
		}
		catch (HibernateException ex) {
			throw convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}

	}

	/**
	 * 执行Hibernate Session的实际关闭, 捕获并记录抛出的任何清理异常.
	 * 
	 * @param session 要关闭的Hibernate Session (may be {@code null})
	 */
	public static void closeSession(Session session) {
		if (session != null) {
			try {
				session.close();
			}
			catch (HibernateException ex) {
				logger.debug("Could not close Hibernate Session", ex);
			}
			catch (Throwable ex) {
				logger.debug("Unexpected exception on closing Hibernate Session", ex);
			}
		}
	}

	/**
	 * 确定给定SessionFactory的DataSource.
	 * 
	 * @param sessionFactory 要检查的SessionFactory
	 * 
	 * @return DataSource, 或{@code null}
	 */
	public static DataSource getDataSource(SessionFactory sessionFactory) {
		Method getProperties = ClassUtils.getMethodIfAvailable(sessionFactory.getClass(), "getProperties");
		if (getProperties != null) {
			Map<?, ?> props = (Map<?, ?>) ReflectionUtils.invokeMethod(getProperties, sessionFactory);
			Object dataSourceValue = props.get(Environment.DATASOURCE);
			if (dataSourceValue instanceof DataSource) {
				return (DataSource) dataSourceValue;
			}
		}
		if (sessionFactory instanceof SessionFactoryImplementor) {
			SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;
			try {
				ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
				if (cp != null) {
					return cp.unwrap(DataSource.class);
				}
			}
			catch (UnknownServiceException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No ConnectionProvider found - cannot determine DataSource for SessionFactory: " + ex);
				}
			}
		}
		return null;
	}

	/**
	 * 将给定的HibernateException转换为{@code org.springframework.dao}层次结构中的适当异常.
	 * 
	 * @param ex 发生的HibernateException
	 * 
	 * @return 相应的DataAccessException 实例
	 */
	public static DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (ex instanceof JDBCConnectionException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof SQLGrammarException) {
			SQLGrammarException jdbcEx = (SQLGrammarException) ex;
			return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof QueryTimeoutException) {
			QueryTimeoutException jdbcEx = (QueryTimeoutException) ex;
			return new org.springframework.dao.QueryTimeoutException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof LockAcquisitionException) {
			LockAcquisitionException jdbcEx = (LockAcquisitionException) ex;
			return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof PessimisticLockException) {
			PessimisticLockException jdbcEx = (PessimisticLockException) ex;
			return new PessimisticLockingFailureException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof ConstraintViolationException) {
			ConstraintViolationException jdbcEx = (ConstraintViolationException) ex;
			return new DataIntegrityViolationException(ex.getMessage()  + "; SQL [" + jdbcEx.getSQL() +
					"]; constraint [" + jdbcEx.getConstraintName() + "]", ex);
		}
		if (ex instanceof DataException) {
			DataException jdbcEx = (DataException) ex;
			return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof JDBCException) {
			return new HibernateJdbcException((JDBCException) ex);
		}
		// end of JDBCException (subclass) handling

		if (ex instanceof QueryException) {
			return new HibernateQueryException((QueryException) ex);
		}
		if (ex instanceof NonUniqueResultException) {
			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof NonUniqueObjectException) {
			return new DuplicateKeyException(ex.getMessage(), ex);
		}
		if (ex instanceof PropertyValueException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		if (ex instanceof PersistentObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof TransientObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof ObjectDeletedException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof UnresolvableObjectException) {
			return new HibernateObjectRetrievalFailureException((UnresolvableObjectException) ex);
		}
		if (ex instanceof WrongClassException) {
			return new HibernateObjectRetrievalFailureException((WrongClassException) ex);
		}
		if (ex instanceof StaleObjectStateException) {
			return new HibernateOptimisticLockingFailureException((StaleObjectStateException) ex);
		}
		if (ex instanceof StaleStateException) {
			return new HibernateOptimisticLockingFailureException((StaleStateException) ex);
		}
		if (ex instanceof OptimisticEntityLockException) {
			return new HibernateOptimisticLockingFailureException((OptimisticEntityLockException) ex);
		}
		if (ex instanceof PessimisticEntityLockException) {
			if (ex.getCause() instanceof LockAcquisitionException) {
				return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
			}
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}

		// fallback
		return new HibernateSystemException(ex);
	}

}
