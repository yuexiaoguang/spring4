package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.core.Constants;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 目标{@link javax.sql.DataSource}的适配器,
 * 将当前Spring事务的隔离级别 (以及可能指定的用户凭据)应用于每个{@code getConnection}调用.
 * 如果指定, 还应用只读标志.
 *
 * <p>可用于代理未配置所需隔离级别(和用户凭据)的目标JNDI DataSource.
 * 客户端代码可以照常使用此DataSource, 而不必担心此类设置.
 *
 * <p>继承从其超类{@link UserCredentialsDataSourceAdapter}应用特定用户凭据的功能;
 * 有关该功能的详细信息, 请参阅后者的javadoc (e.g. {@link #setCredentialsForCurrentThread}).
 *
 * <p><b>WARNING:</b> 此适配器调用{@link java.sql.Connection#setTransactionIsolation}
 * 或{@link java.sql.Connection#setReadOnly}, 从中获取每个Connection.
 * 但是, <i>不</i>重置这些设置; 它更希望目标DataSource在连接池处理过程中执行此类重置.
 * <b>确保目标DataSource正确清理此类事务状态.</b>
 */
public class IsolationLevelDataSourceAdapter extends UserCredentialsDataSourceAdapter {

	/** TransactionDefinition的常量实例 */
	private static final Constants constants = new Constants(TransactionDefinition.class);

	private Integer isolationLevel;


	/**
	 * 在{@link org.springframework.transaction.TransactionDefinition}中通过相应常量的名称, 设置默认隔离级别,
	 * e.g. "ISOLATION_SERIALIZABLE".
	 * <p>如果未指定, 将使用目标DataSource的默认值.
	 * 请注意, 特定于事务的隔离值将始终覆盖在DataSource级别指定的任何隔离设置.
	 * 
	 * @param constantName 常量的名称
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
			throw new IllegalArgumentException("Only isolation constants allowed");
		}
		setIsolationLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * 根据JDBC {@link java.sql.Connection}常量, 指定用于连接检索的默认隔离级别
	 * (等效于相应的Spring {@link org.springframework.transaction.TransactionDefinition}常量).
	 * <p>如果未指定, 将使用目标DataSource的默认值.
	 * 请注意, 特定于事务的隔离值将始终覆盖在DataSource级别指定的任何隔离设置.
	 */
	public void setIsolationLevel(int isolationLevel) {
		if (!constants.getValues(DefaultTransactionDefinition.PREFIX_ISOLATION).contains(isolationLevel)) {
			throw new IllegalArgumentException("Only values of isolation constants allowed");
		}
		this.isolationLevel = (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT ? isolationLevel : null);
	}

	/**
	 * 返回静态指定的隔离级别, 或{@code null}.
	 */
	protected Integer getIsolationLevel() {
		return this.isolationLevel;
	}


	/**
	 * 将当前隔离级别值和只读标志应用于返回的Connection.
	 */
	@Override
	protected Connection doGetConnection(String username, String password) throws SQLException {
		Connection con = super.doGetConnection(username, password);
		Boolean readOnlyToUse = getCurrentReadOnlyFlag();
		if (readOnlyToUse != null) {
			con.setReadOnly(readOnlyToUse);
		}
		Integer isolationLevelToUse = getCurrentIsolationLevel();
		if (isolationLevelToUse != null) {
			con.setTransactionIsolation(isolationLevelToUse);
		}
		return con;
	}

	/**
	 * 确定当前隔离级别: 事务的隔离级别, 或静态定义的隔离级别.
	 * 
	 * @return 当前隔离级别, 或{@code null}
	 */
	protected Integer getCurrentIsolationLevel() {
		Integer isolationLevelToUse = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
		if (isolationLevelToUse == null) {
			isolationLevelToUse = getIsolationLevel();
		}
		return isolationLevelToUse;
	}

	/**
	 * 确定当前的只读标志: 默认情况下, 事务的只读提示.
	 * 
	 * @return 是否存在当前范围的只读提示
	 */
	protected Boolean getCurrentReadOnlyFlag() {
		boolean txReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
		return (txReadOnly ? Boolean.TRUE : null);
	}
}
