package org.springframework.orm.jpa.vendor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.OptimisticLockException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PessimisticLockException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Hibernate EntityManager的{@link org.springframework.orm.jpa.JpaDialect}实现.
 * 针对3.6, 4.2/4.3 以及 5.0/5.1/5.2开发和测试.
 */
@SuppressWarnings("serial")
public class HibernateJpaDialect extends DefaultJpaDialect {

	private static Class<?> optimisticLockExceptionClass;

	private static Class<?> pessimisticLockExceptionClass;

	private static Method getFlushMode;

	static {
		// 检查Hibernate 4.x的Optimistic/PessimisticEntityLockException
		ClassLoader cl = HibernateJpaDialect.class.getClassLoader();
		try {
			optimisticLockExceptionClass = cl.loadClass("org.hibernate.dialect.lock.OptimisticEntityLockException");
		}
		catch (ClassNotFoundException ex) {
			// 在Hibernate 4.x上不推荐使用OptimisticLockException; 只是在3.x上使用它
			optimisticLockExceptionClass = OptimisticLockException.class;
		}
		try {
			pessimisticLockExceptionClass = cl.loadClass("org.hibernate.dialect.lock.PessimisticEntityLockException");
		}
		catch (ClassNotFoundException ex) {
			pessimisticLockExceptionClass = null;
		}

		try {
			// Hibernate 5.2+ getHibernateFlushMode()
			getFlushMode = Session.class.getMethod("getHibernateFlushMode");
		}
		catch (NoSuchMethodException ex) {
			try {
				// 经典的Hibernate getFlushMode() 与FlushMode返回类型
				getFlushMode = Session.class.getMethod("getFlushMode");
			}
			catch (NoSuchMethodException ex2) {
				throw new IllegalStateException("No compatible Hibernate getFlushMode signature found", ex2);
			}
		}
		// 检查它是否是Hibernate FlushMode类型, 而不是JPA...
		Assert.state(FlushMode.class == getFlushMode.getReturnType(), "Could not find Hibernate getFlushMode method");
	}


	boolean prepareConnection = (HibernateConnectionHandle.sessionConnectionMethod == null);


	/**
	 * 设置是否准备事务性Hibernate会话的底层JDBC连接, 即是否将特定于事务的隔离级别和/或事务的只读标志应用于底层JDBC连接.
	 * <p>Hibernate EntityManager 4.x上的默认值为"true" (具有'on-close'连接释放模式),
	 * 而Hibernate EntityManager 3.6上为"false" (由于那里的'事务之后'释放模式).
	 * <b>请注意, 强烈建议使用Hibernate 4.2+, 以使隔离级别高效工作.</b>
	 * <p>如果关闭此标志, JPA事务管理将不再支持每个事务的隔离级别.
	 * 对于只读事务, 它也不会调用{@code Connection.setReadOnly(true)}.
	 * 如果关闭此标志, 则事务后不需要清除JDBC连接, 因为不会修改任何连接设置.
	 * <p><b>NOTE:</b> 在只读处理方面的默认行为在Spring 4.1中发生了变化,
	 * 现在将只读状态传播到JDBC Connection, 类似于其他Spring事务管理器.
	 * 这可能会导致强制只读, 而以前的写访问被意外容忍:
	 * 请相应地修改事务声明, 必要时删除无效的只读标记.
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.prepareConnection = prepareConnection;
	}


	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		Session session = getSession(entityManager);

		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			session.getTransaction().setTimeout(definition.getTimeout());
		}

		boolean isolationLevelNeeded = (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT);
		Integer previousIsolationLevel = null;
		Connection preparedCon = null;

		if (isolationLevelNeeded || definition.isReadOnly()) {
			if (this.prepareConnection) {
				preparedCon = HibernateConnectionHandle.doGetConnection(session);
				previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(preparedCon, definition);
			}
			else if (isolationLevelNeeded) {
				throw new InvalidIsolationLevelException(getClass().getSimpleName() +
						" does not support custom isolation levels since the 'prepareConnection' flag is off. " +
						"This is the case on Hibernate 3.6 by default; either switch that flag at your own risk " +
						"or upgrade to Hibernate 4.x, with 4.2+ recommended.");
			}
		}

		// 标准JPA事务开始调用完整的JPA上下文设置...
		entityManager.getTransaction().begin();

		// 调整刷新模式, 并存储先前的隔离级别.
		FlushMode previousFlushMode = prepareFlushMode(session, definition.isReadOnly());
		return new SessionTransactionData(session, previousFlushMode, preparedCon, previousIsolationLevel);
	}

	@Override
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException {

		Session session = getSession(entityManager);
		FlushMode previousFlushMode = prepareFlushMode(session, readOnly);
		return new SessionTransactionData(session, previousFlushMode, null, null);
	}

	protected FlushMode prepareFlushMode(Session session, boolean readOnly) throws PersistenceException {
		FlushMode flushMode = (FlushMode) ReflectionUtils.invokeMethod(getFlushMode, session);
		if (readOnly) {
			// 应该禁止刷新只读事务.
			if (!flushMode.equals(FlushMode.MANUAL)) {
				session.setFlushMode(FlushMode.MANUAL);
				return flushMode;
			}
		}
		else {
			// 对于非只读事务, 需要AUTO或COMMIT.
			if (flushMode.lessThan(FlushMode.COMMIT)) {
				session.setFlushMode(FlushMode.AUTO);
				return flushMode;
			}
		}
		// 不需要FlushMode更改...
		return null;
	}

	@Override
	public void cleanupTransaction(Object transactionData) {
		((SessionTransactionData) transactionData).resetSessionState();
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		Session session = getSession(entityManager);
		return new HibernateConnectionHandle(session);
	}

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex);
		}
		if (ex instanceof PersistenceException && ex.getCause() instanceof HibernateException) {
			return convertHibernateAccessException((HibernateException) ex.getCause());
		}
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}

	/**
	 * 将给定的HibernateException转换为{@code org.springframework.dao}层次结构中的适当异常.
	 * 
	 * @param ex 发生的HibernateException
	 * 
	 * @return 相应的DataAccessException实例
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
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
		// end of JDBCException subclass handling

		if (ex instanceof QueryException) {
			return new InvalidDataAccessResourceUsageException(ex.getMessage(), ex);
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
			UnresolvableObjectException hibEx = (UnresolvableObjectException) ex;
			return new ObjectRetrievalFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex.getMessage(), ex);
		}
		if (ex instanceof WrongClassException) {
			WrongClassException hibEx = (WrongClassException) ex;
			return new ObjectRetrievalFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex.getMessage(), ex);
		}
		if (ex instanceof StaleObjectStateException) {
			StaleObjectStateException hibEx = (StaleObjectStateException) ex;
			return new ObjectOptimisticLockingFailureException(hibEx.getEntityName(), hibEx.getIdentifier(), ex);
		}
		if (ex instanceof StaleStateException) {
			return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (optimisticLockExceptionClass.isInstance(ex)) {
			return new ObjectOptimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (pessimisticLockExceptionClass != null && pessimisticLockExceptionClass.isInstance(ex)) {
			if (ex.getCause() instanceof LockAcquisitionException) {
				return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
			}
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}

		// fallback
		return new JpaSystemException(ex);
	}

	protected Session getSession(EntityManager entityManager) {
		return entityManager.unwrap(Session.class);
	}


	private static class SessionTransactionData {

		private final Session session;

		private final FlushMode previousFlushMode;

		private final Connection preparedCon;

		private final Integer previousIsolationLevel;

		public SessionTransactionData(
				Session session, FlushMode previousFlushMode, Connection preparedCon, Integer previousIsolationLevel) {
			this.session = session;
			this.previousFlushMode = previousFlushMode;
			this.preparedCon = preparedCon;
			this.previousIsolationLevel = previousIsolationLevel;
		}

		public void resetSessionState() {
			if (this.previousFlushMode != null) {
				this.session.setFlushMode(this.previousFlushMode);
			}
			if (this.preparedCon != null && this.session.isConnected()) {
				Connection conToReset = HibernateConnectionHandle.doGetConnection(this.session);
				if (conToReset != this.preparedCon) {
					LogFactory.getLog(HibernateJpaDialect.class).warn(
							"JDBC Connection to reset not identical to originally prepared Connection - please " +
							"make sure to use connection release mode ON_CLOSE (the default) and to run against " +
							"Hibernate 4.2+ (or switch HibernateJpaDialect's prepareConnection flag to false");
				}
				DataSourceUtils.resetConnectionAfterTransaction(conToReset, this.previousIsolationLevel);
			}
		}
	}


	private static class HibernateConnectionHandle implements ConnectionHandle {

		// 这将在Hibernate 3.x上找到相应的方法, 但在4.x上找不到
		private static final Method sessionConnectionMethod =
				ClassUtils.getMethodIfAvailable(Session.class, "connection");

		private static volatile Method connectionMethodToUse = sessionConnectionMethod;

		private final Session session;

		public HibernateConnectionHandle(Session session) {
			this.session = session;
		}

		@Override
		public Connection getConnection() {
			return doGetConnection(this.session);
		}

		@Override
		public void releaseConnection(Connection con) {
			if (sessionConnectionMethod != null) {
				// 需要使用Hibernate 3.x显式调用close(), 以便在必要时允许实时释放底层物理连接.
				// 但是, 不要在Hibernate 4.2+上执行此操作, 因为它会立即将物理连接返回到池中, 使其无法用于当前事务中的进一步操作!
				JdbcUtils.closeConnection(con);
			}
		}

		public static Connection doGetConnection(Session session) {
			try {
				if (connectionMethodToUse == null) {
					// 在Hibernate 4.x上反射查找SessionImpl的connection()方法
					connectionMethodToUse = session.getClass().getMethod("connection");
				}
				return (Connection) ReflectionUtils.invokeMethod(connectionMethodToUse, session);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Cannot find connection() method on Hibernate Session", ex);
			}
		}
	}
}
