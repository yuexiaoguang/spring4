package org.springframework.jdbc.datasource.lookup;

import org.springframework.core.Constants;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 根据当前事务隔离级别路由到各种目标DataSource之一的DataSource.
 * 需要使用隔离级别名称作为键进行配置的目标DataSource, 如
 * {@link org.springframework.transaction.TransactionDefinition TransactionDefinition接口}上所定义.
 *
 * <p>这在与JTA事务管理结合使用时特别有用
 * (通常通过Spring的{@link org.springframework.transaction.jta.JtaTransactionManager}).
 * 标准JTA不支持特定于事务的隔离级别.
 * 一些JTA提供程序支持隔离级别作为特定于供应商的扩展 (e.g. WebLogic), 这是解决此问题的首选方法.
 * 作为替代方案 (e.g. 在WebSphere上), 目标数据库可以通过多个JNDI DataSource表示,
 * 每个都配置有不同的隔离级别 (对于整个DataSource).
 * 目前的DataSource路由器允许根据当前事务的隔离级别透明地切换到适当的DataSource.
 *
 * <p>例如, 假设目标DataSources被定义为名为"myRepeatableReadDataSource",
 * "mySerializableDataSource"和"myDefaultDataSource"的单个Spring bean, 配置可以是这样的:
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value-ref="myRepeatableReadDataSource"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value-ref="mySerializableDataSource"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" ref="myDefaultDataSource"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 或者, 键也可以是数据源名称, 通过{@link #setDataSourceLookup DataSourceLookup}解析:
 * 默认情况下, 标准JNDI查找的JNDI名称.
 * 这允许单个简洁的定义, 而无需单独的DataSource bean定义.
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value="java:comp/env/jdbc/myrrds"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value="java:comp/env/jdbc/myserds"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" value="java:comp/env/jdbc/mydefds"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Note: 如果将此路由器与Spring的{@link org.springframework.transaction.jta.JtaTransactionManager}结合使用,
 * 不要忘记将"allowCustomIsolationLevels"标志切换为"true".
 * (默认情况下, JtaTransactionManager 仅接受默认隔离级别, 因为标准JTA本身缺乏隔离级别支持.)
 *
 * <pre class="code">
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"&gt;
 *   &lt;property name="allowCustomIsolationLevels" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 */
public class IsolationLevelDataSourceRouter extends AbstractRoutingDataSource {

	/** TransactionDefinition的常量实例 */
	private static final Constants constants = new Constants(TransactionDefinition.class);


	/**
	 * 支持隔离级别常量的整数值, 以及
	 * {@link org.springframework.transaction.TransactionDefinition TransactionDefinition接口}上定义的隔离级别名称.
	 */
	@Override
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		if (lookupKey instanceof Integer) {
			return lookupKey;
		}
		else if (lookupKey instanceof String) {
			String constantName = (String) lookupKey;
			if (!constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
				throw new IllegalArgumentException("Only isolation constants allowed");
			}
			return constants.asNumber(constantName);
		}
		else {
			throw new IllegalArgumentException(
					"Invalid lookup key - needs to be isolation level Integer or isolation level name String: " + lookupKey);
		}
	}

	@Override
	protected Object determineCurrentLookupKey() {
		return TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
	}

}
