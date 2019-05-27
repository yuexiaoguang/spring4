package org.springframework.jdbc.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * <b>这是JDBC核心包中的中心类.</b>
 * 它简化了JDBC的使用, 有助于避免常见错误.
 * 它执行核心JDBC工作流, 使应用程序代码提供SQL并提取结果.
 * 此类执行SQL查询或更新, 启动对ResultSet的迭代, 并捕获JDBC异常,
 * 并将它们转换为{@code org.springframework.dao}包中定义的通用, 更具信息性的异常层次结构.
 *
 * <p>使用此类的代码只需要实现回调接口, 为它们提供明确定义的约定.
 * {@link PreparedStatementCreator}回调接口在给定Connection的情况下创建预准备语句, 提供SQL和任何必要的参数.
 * {@link ResultSetExtractor}接口从ResultSet中提取值.
 * 另请参阅{@link PreparedStatementSetter}和{@link RowMapper}以获取两个流行的替代回调接口.
 *
 * <p>可以通过使用DataSource引用直接实例化在服务实现中使用, 或者在应用程序上下文中准备并作为bean引用提供给服务.
 * Note: DataSource应始终在应用程序上下文中配置为bean, 在第一种情况下直接提供给服务, 在第二种情况下配置为准备好的模板.
 *
 * <p>因为这个类可以通过回调接口和{@link org.springframework.jdbc.support.SQLExceptionTranslator}接口进行参数化,
 * 所以不需要对它进行子类化.
 *
 * <p>此类执行的所有SQL操作都在debug级别记录, 使用"org.springframework.jdbc.core.JdbcTemplate"作为日志类别.
 *
 * <p><b>NOTE: 一旦配置, 该类的实例就是线程安全的.</b>
 */
public class JdbcTemplate extends JdbcAccessor implements JdbcOperations {

	private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";

	private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";


	/** 自定义 NativeJdbcExtractor */
	private NativeJdbcExtractor nativeJdbcExtractor;

	/** 如果此变量为false, 将抛出SQL警告的异常 */
	private boolean ignoreWarnings = true;

	/**
	 * 如果此变量设置为非负值, 则它将在用于查询处理的语句上设置fetchSize属性.
	 */
	private int fetchSize = -1;

	/**
	 * 如果此变量设置为非负值, 则它将在用于查询处理的语句上设置maxRows属性.
	 */
	private int maxRows = -1;

	/**
	 * 如果此变量设置为非负值, 则它将在用于查询处理的语句上设置queryTimeout属性.
	 */
	private int queryTimeout = -1;

	/**
	 * 如果此变量设置为true, 则将绕过所有可调用语句处理的所有结果检查.
	 * 这可用于避免某些较旧的Oracle JDBC驱动程序(如 10.1.0.2)中的错误.
	 */
	private boolean skipResultsProcessing = false;

	/**
	 * 如果此变量设置为true, 那么将绕过没有相应SqlOutParameter声明的存储过程调用的所有结果.
	 * 除非变量{@code skipResultsProcessing}设置为{@code true}, 否则将执行所有其他结果处理.
	 */
	private boolean skipUndeclaredResults = false;

	/**
	 * 如果此变量设置为true, 则CallableStatement的执行将在Map中返回结果, 该Map使用不区分大小写的参数名称.
	 */
	private boolean resultsMapCaseInsensitive = false;


	/**
	 * 为bean使用构造一个新的JdbcTemplate.
	 * <p>Note: 必须在使用实例之前设置DataSource.
	 */
	public JdbcTemplate() {
	}

	/**
	 * 构造一个新的JdbcTemplate, 给定一个DataSource来获取连接.
	 * <p>Note: 这不会触发异常翻译器的初始化.
	 * 
	 * @param dataSource 从中获取连接的JDBC DataSource
	 */
	public JdbcTemplate(DataSource dataSource) {
		setDataSource(dataSource);
		afterPropertiesSet();
	}

	/**
	 * 构造一个新的JdbcTemplate, 给定一个DataSource来获取连接.
	 * <p>Note: 根据"lazyInit"标志, 将触发异常转换器的初始化.
	 * 
	 * @param dataSource 从中获取连接的JDBC DataSource
	 * @param lazyInit 是否延迟初始化SQLExceptionTranslator
	 */
	public JdbcTemplate(DataSource dataSource, boolean lazyInit) {
		setDataSource(dataSource);
		setLazyInit(lazyInit);
		afterPropertiesSet();
	}


	/**
	 * 设置NativeJdbcExtractor以从包装的句柄中提取本机JDBC对象.
	 * 如果需要将本机Statement和/或ResultSet句柄转换为特定于数据库的实现类,
	 * 但是使用包装JDBC对象的连接池 (note: <i>任何</i>池将返回包装的Connection), 这很有用.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor extractor) {
		this.nativeJdbcExtractor = extractor;
	}

	/**
	 * 返回当前的NativeJdbcExtractor实现.
	 */
	public NativeJdbcExtractor getNativeJdbcExtractor() {
		return this.nativeJdbcExtractor;
	}

	/**
	 * 设置是否要忽略SQLWarnings.
	 * <p>默认"true", 吞咽和记录所有警告. 将此标志切换为"false", 以使JdbcTemplate抛出SQLWarningException.
	 */
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		this.ignoreWarnings = ignoreWarnings;
	}

	/**
	 * 返回是否忽略SQLWarnings.
	 */
	public boolean isIgnoreWarnings() {
		return this.ignoreWarnings;
	}

	/**
	 * 设置此JdbcTemplate的获取大小.
	 * 这对于处理大型结果集很重要:
	 * 将其设置为高于默认值将以内存消耗为代价提高处理速度;
	 * 将此值设置得较低可以避免传输应用程序可能永远不会读取的行数据.
	 * <p>默认 -1, 指示使用JDBC驱动程序的默认配置 (i.e. 不将特定的提取大小设置传递给驱动程序).
	 * <p>Note: 从4.3开始, -1以外的负值将被传递给驱动程序, 因为 e.g. MySQL支持{@code Integer.MIN_VALUE}的特殊行为.
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * 返回此JdbcTemplate的提取大小.
	 */
	public int getFetchSize() {
		return this.fetchSize;
	}

	/**
	 * 设置此JdbcTemplate的最大行数.
	 * 这对于处理大型结果集的子集非常重要, 如果从不对整个结果感兴趣 (例如, 执行可能返回大量匹配的搜索时),
	 * 避免读取和保存数据库或JDBC驱动程序中的整个结果集.
	 * <p>默认 -1, 表示使用JDBC驱动程序的默认配置 (i.e. 不将特定的最大行设置传递给驱动程序).
	 * <p>Note: 从4.3开始, -1以外的负值将传递给驱动程序, 与{@link #setFetchSize}对特殊MySQL值的支持同步.
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	/**
	 * 返回为此JdbcTemplate指定的最大行数.
	 */
	public int getMaxRows() {
		return this.maxRows;
	}

	/**
	 * 为此JdbcTemplate执行的语句设置查询超时.
	 * <p>默认 -1, 表示使用JDBC驱动程序的默认值 (i.e. 不在驱动程序上传递特定的查询超时设置).
	 * <p>Note: 在事务级别指定超时的事务中执行时, 此处指定的任何超时都将被剩余的事务超时覆盖.
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	/**
	 * 返回此JdbcTemplate执行的语句的查询超时.
	 */
	public int getQueryTimeout() {
		return this.queryTimeout;
	}

	/**
	 * 设置是否应跳过结果处理.
	 * 当我们知道没有传回结果时, 可以用来优化可调用语句处理 - 仍然会进行out参数的处理.
	 * 这可用于避免某些较旧的Oracle JDBC驱动程序(如 10.1.0.2)中的错误.
	 */
	public void setSkipResultsProcessing(boolean skipResultsProcessing) {
		this.skipResultsProcessing = skipResultsProcessing;
	}

	/**
	 * 返回是否应跳过结果处理.
	 */
	public boolean isSkipResultsProcessing() {
		return this.skipResultsProcessing;
	}

	/**
	 * 设置是否应跳过未声明的结果.
	 */
	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		this.skipUndeclaredResults = skipUndeclaredResults;
	}

	/**
	 * 返回是否应跳过未声明的结果.
	 */
	public boolean isSkipUndeclaredResults() {
		return this.skipUndeclaredResults;
	}

	/**
	 * 设置Call​​ableStatement的执行是否将在Map中返回结果, 该Map使用参数的不区分大小写的名称.
	 */
	public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
		this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
	}

	/**
	 * 返回Call​​ableStatement的执行是否将在Map中返回结果, 该Map使用参数的不区分大小写的名称.
	 */
	public boolean isResultsMapCaseInsensitive() {
		return this.resultsMapCaseInsensitive;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with a plain java.sql.Connection
	//-------------------------------------------------------------------------

	@Override
	public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Connection con = DataSourceUtils.getConnection(getDataSource());
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null) {
				// 提取本机JDBC连接, 可转换为OracleConnection等.
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			else {
				// 创建关闭抑制Connection代理, 同时准备返回的Statement.
				conToUse = createConnectionProxy(con);
			}
			return action.doInConnection(conToUse);
		}
		catch (SQLException ex) {
			// 尽早释放Connection, 以避免在尚未初始化异常转换器的情况下, 潜在的连接池死锁.
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw getExceptionTranslator().translate("ConnectionCallback", getSql(action), ex);
		}
		finally {
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	/**
	 * 为给定的JDBC连接创建一个关闭抑制代理.
	 * 由{@code execute}方法调用.
	 * <p>代理还准备返回的JDBC Statements, 应用语句设置, 如获取大小, 最大行数, 和查询超时.
	 * 
	 * @param con 用于创建代理的JDBC Connection
	 * 
	 * @return Connection代理
	 */
	protected Connection createConnectionProxy(Connection con) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new CloseSuppressingInvocationHandler(con));
	}


	//-------------------------------------------------------------------------
	// Methods dealing with static SQL (java.sql.Statement)
	//-------------------------------------------------------------------------

	@Override
	public <T> T execute(StatementCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null &&
					this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativeStatements()) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			stmt = conToUse.createStatement();
			applyStatementSettings(stmt);
			Statement stmtToUse = stmt;
			if (this.nativeJdbcExtractor != null) {
				stmtToUse = this.nativeJdbcExtractor.getNativeStatement(stmt);
			}
			T result = action.doInStatement(stmtToUse);
			handleWarnings(stmt);
			return result;
		}
		catch (SQLException ex) {
			// 尽早释放连接, 以避免在尚未初始化异常转换器的情况下潜在的连接池死锁.
			JdbcUtils.closeStatement(stmt);
			stmt = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw getExceptionTranslator().translate("StatementCallback", getSql(action), ex);
		}
		finally {
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	@Override
	public void execute(final String sql) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL statement [" + sql + "]");
		}

		class ExecuteStatementCallback implements StatementCallback<Object>, SqlProvider {
			@Override
			public Object doInStatement(Statement stmt) throws SQLException {
				stmt.execute(sql);
				return null;
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		execute(new ExecuteStatementCallback());
	}

	@Override
	public <T> T query(final String sql, final ResultSetExtractor<T> rse) throws DataAccessException {
		Assert.notNull(sql, "SQL must not be null");
		Assert.notNull(rse, "ResultSetExtractor must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL query [" + sql + "]");
		}

		class QueryStatementCallback implements StatementCallback<T>, SqlProvider {
			@Override
			public T doInStatement(Statement stmt) throws SQLException {
				ResultSet rs = null;
				try {
					rs = stmt.executeQuery(sql);
					ResultSet rsToUse = rs;
					if (nativeJdbcExtractor != null) {
						rsToUse = nativeJdbcExtractor.getNativeResultSet(rs);
					}
					return rse.extractData(rsToUse);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
				}
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return execute(new QueryStatementCallback());
	}

	@Override
	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
		query(sql, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		return query(sql, new RowMapperResultSetExtractor<T>(rowMapper));
	}

	@Override
	public Map<String, Object> queryForMap(String sql) throws DataAccessException {
		return queryForObject(sql, getColumnMapRowMapper());
	}

	@Override
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		List<T> results = query(sql, rowMapper);
		return DataAccessUtils.requiredSingleResult(results);
	}

	@Override
	public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
		return query(sql, getSingleColumnRowMapper(elementType));
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
		return query(sql, getColumnMapRowMapper());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
		return query(sql, new SqlRowSetResultSetExtractor());
	}

	@Override
	public int update(final String sql) throws DataAccessException {
		Assert.notNull(sql, "SQL must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL update [" + sql + "]");
		}

		class UpdateStatementCallback implements StatementCallback<Integer>, SqlProvider {
			@Override
			public Integer doInStatement(Statement stmt) throws SQLException {
				int rows = stmt.executeUpdate(sql);
				if (logger.isDebugEnabled()) {
					logger.debug("SQL update affected " + rows + " rows");
				}
				return rows;
			}
			@Override
			public String getSql() {
				return sql;
			}
		}

		return execute(new UpdateStatementCallback());
	}

	@Override
	public int[] batchUpdate(final String... sql) throws DataAccessException {
		Assert.notEmpty(sql, "SQL array must not be empty");
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update of " + sql.length + " statements");
		}

		class BatchUpdateStatementCallback implements StatementCallback<int[]>, SqlProvider {

			private String currSql;

			@Override
			public int[] doInStatement(Statement stmt) throws SQLException, DataAccessException {
				int[] rowsAffected = new int[sql.length];
				if (JdbcUtils.supportsBatchUpdates(stmt.getConnection())) {
					for (String sqlStmt : sql) {
						this.currSql = appendSql(this.currSql, sqlStmt);
						stmt.addBatch(sqlStmt);
					}
					try {
						rowsAffected = stmt.executeBatch();
					}
					catch (BatchUpdateException ex) {
						String batchExceptionSql = null;
						for (int i = 0; i < ex.getUpdateCounts().length; i++) {
							if (ex.getUpdateCounts()[i] == Statement.EXECUTE_FAILED) {
								batchExceptionSql = appendSql(batchExceptionSql, sql[i]);
							}
						}
						if (StringUtils.hasLength(batchExceptionSql)) {
							this.currSql = batchExceptionSql;
						}
						throw ex;
					}
				}
				else {
					for (int i = 0; i < sql.length; i++) {
						this.currSql = sql[i];
						if (!stmt.execute(sql[i])) {
							rowsAffected[i] = stmt.getUpdateCount();
						}
						else {
							throw new InvalidDataAccessApiUsageException("Invalid batch SQL statement: " + sql[i]);
						}
					}
				}
				return rowsAffected;
			}

			private String appendSql(String sql, String statement) {
				return (StringUtils.isEmpty(sql) ? statement : sql + "; " + statement);
			}

			@Override
			public String getSql() {
				return this.currSql;
			}
		}

		return execute(new BatchUpdateStatementCallback());
	}


	//-------------------------------------------------------------------------
	// Methods dealing with prepared statements
	//-------------------------------------------------------------------------

	@Override
	public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
			throws DataAccessException {

		Assert.notNull(psc, "PreparedStatementCreator must not be null");
		Assert.notNull(action, "Callback object must not be null");
		if (logger.isDebugEnabled()) {
			String sql = getSql(psc);
			logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
		}

		Connection con = DataSourceUtils.getConnection(getDataSource());
		PreparedStatement ps = null;
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null &&
					this.nativeJdbcExtractor.isNativeConnectionNecessaryForNativePreparedStatements()) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			ps = psc.createPreparedStatement(conToUse);
			applyStatementSettings(ps);
			PreparedStatement psToUse = ps;
			if (this.nativeJdbcExtractor != null) {
				psToUse = this.nativeJdbcExtractor.getNativePreparedStatement(ps);
			}
			T result = action.doInPreparedStatement(psToUse);
			handleWarnings(ps);
			return result;
		}
		catch (SQLException ex) {
			// 尽早释放连接, 以避免在尚未初始化异常转换器的情况下潜在的连接池死锁.
			if (psc instanceof ParameterDisposer) {
				((ParameterDisposer) psc).cleanupParameters();
			}
			String sql = getSql(psc);
			psc = null;
			JdbcUtils.closeStatement(ps);
			ps = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw getExceptionTranslator().translate("PreparedStatementCallback", sql, ex);
		}
		finally {
			if (psc instanceof ParameterDisposer) {
				((ParameterDisposer) psc).cleanupParameters();
			}
			JdbcUtils.closeStatement(ps);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	@Override
	public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
		return execute(new SimplePreparedStatementCreator(sql), action);
	}

	/**
	 * 使用预准备语句进行查询, 允许PreparedStatementCreator和PreparedStatementSetter.
	 * 大多数其他查询方法使用此方法, 但应用程序代码将始终与创建者或setter一起使用.
	 * 
	 * @param psc 在给定Connection的情况下可以创建PreparedStatement的回调处理器
	 * @param pss 知道如何在预准备语句上设置值的对象.
	 * 如果为 null, 则假定SQL不包含绑定参数.
	 * @param rse 将提取结果的对象.
	 * 
	 * @return ResultSetExtractor返回的任意结果对象
	 * @throws DataAccessException 如果有任何问题
	 */
	public <T> T query(
			PreparedStatementCreator psc, final PreparedStatementSetter pss, final ResultSetExtractor<T> rse)
			throws DataAccessException {

		Assert.notNull(rse, "ResultSetExtractor must not be null");
		logger.debug("Executing prepared SQL query");

		return execute(psc, new PreparedStatementCallback<T>() {
			@Override
			public T doInPreparedStatement(PreparedStatement ps) throws SQLException {
				ResultSet rs = null;
				try {
					if (pss != null) {
						pss.setValues(ps);
					}
					rs = ps.executeQuery();
					ResultSet rsToUse = rs;
					if (nativeJdbcExtractor != null) {
						rsToUse = nativeJdbcExtractor.getNativeResultSet(rs);
					}
					return rse.extractData(rsToUse);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
					if (pss instanceof ParameterDisposer) {
						((ParameterDisposer) pss).cleanupParameters();
					}
				}
			}
		});
	}

	@Override
	public <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(psc, null, rse);
	}

	@Override
	public <T> T query(String sql, PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(new SimplePreparedStatementCreator(sql), pss, rse);
	}

	@Override
	public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgTypePreparedStatementSetter(args, argTypes), rse);
	}

	@Override
	public <T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, newArgPreparedStatementSetter(args), rse);
	}

	@Override
	public <T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws DataAccessException {
		return query(sql, newArgPreparedStatementSetter(args), rse);
	}

	@Override
	public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
		query(psc, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
		query(sql, pss, new RowCallbackHandlerResultSetExtractor(rch));
	}

	@Override
	public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
		query(sql, newArgTypePreparedStatementSetter(args, argTypes), rch);
	}

	@Override
	public void query(String sql, Object[] args, RowCallbackHandler rch) throws DataAccessException {
		query(sql, newArgPreparedStatementSetter(args), rch);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch, Object... args) throws DataAccessException {
		query(sql, newArgPreparedStatementSetter(args), rch);
	}

	@Override
	public <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
		return query(psc, new RowMapperResultSetExtractor<T>(rowMapper));
	}

	@Override
	public <T> List<T> query(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
		return query(sql, pss, new RowMapperResultSetExtractor<T>(rowMapper));
	}

	@Override
	public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
		return query(sql, args, argTypes, new RowMapperResultSetExtractor<T>(rowMapper));
	}

	@Override
	public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		return query(sql, args, new RowMapperResultSetExtractor<T>(rowMapper));
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
		return query(sql, args, new RowMapperResultSetExtractor<T>(rowMapper));
	}

	@Override
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException {

		List<T> results = query(sql, args, argTypes, new RowMapperResultSetExtractor<T>(rowMapper, 1));
		return DataAccessUtils.requiredSingleResult(results);
	}

	@Override
	public <T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		List<T> results = query(sql, args, new RowMapperResultSetExtractor<T>(rowMapper, 1));
		return DataAccessUtils.requiredSingleResult(results);
	}

	@Override
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
		List<T> results = query(sql, args, new RowMapperResultSetExtractor<T>(rowMapper, 1));
		return DataAccessUtils.requiredSingleResult(results);
	}

	@Override
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, args, argTypes, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public <T> T queryForObject(String sql, Object[] args, Class<T> requiredType) throws DataAccessException {
		return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
		return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return queryForObject(sql, args, argTypes, getColumnMapRowMapper());
	}

	@Override
	public Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException {
		return queryForObject(sql, args, getColumnMapRowMapper());
	}

	@Override
	public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType) throws DataAccessException {
		return query(sql, args, argTypes, getSingleColumnRowMapper(elementType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Object[] args, Class<T> elementType) throws DataAccessException {
		return query(sql, args, getSingleColumnRowMapper(elementType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException {
		return query(sql, args, getSingleColumnRowMapper(elementType));
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return query(sql, args, argTypes, getColumnMapRowMapper());
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException {
		return query(sql, args, getColumnMapRowMapper());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return query(sql, args, argTypes, new SqlRowSetResultSetExtractor());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Object... args) throws DataAccessException {
		return query(sql, args, new SqlRowSetResultSetExtractor());
	}

	protected int update(final PreparedStatementCreator psc, final PreparedStatementSetter pss)
			throws DataAccessException {

		logger.debug("Executing prepared SQL update");
		return execute(psc, new PreparedStatementCallback<Integer>() {
			@Override
			public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException {
				try {
					if (pss != null) {
						pss.setValues(ps);
					}
					int rows = ps.executeUpdate();
					if (logger.isDebugEnabled()) {
						logger.debug("SQL update affected " + rows + " rows");
					}
					return rows;
				}
				finally {
					if (pss instanceof ParameterDisposer) {
						((ParameterDisposer) pss).cleanupParameters();
					}
				}
			}
		});
	}

	@Override
	public int update(PreparedStatementCreator psc) throws DataAccessException {
		return update(psc, (PreparedStatementSetter) null);
	}

	@Override
	public int update(final PreparedStatementCreator psc, final KeyHolder generatedKeyHolder)
			throws DataAccessException {

		Assert.notNull(generatedKeyHolder, "KeyHolder must not be null");
		logger.debug("Executing SQL update and returning generated keys");

		return execute(psc, new PreparedStatementCallback<Integer>() {
			@Override
			public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException {
				int rows = ps.executeUpdate();
				List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
				generatedKeys.clear();
				ResultSet keys = ps.getGeneratedKeys();
				if (keys != null) {
					try {
						RowMapperResultSetExtractor<Map<String, Object>> rse =
								new RowMapperResultSetExtractor<Map<String, Object>>(getColumnMapRowMapper(), 1);
						generatedKeys.addAll(rse.extractData(keys));
					}
					finally {
						JdbcUtils.closeResultSet(keys);
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("SQL update affected " + rows + " rows and returned " + generatedKeys.size() + " keys");
				}
				return rows;
			}
		});
	}

	@Override
	public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
		return update(new SimplePreparedStatementCreator(sql), pss);
	}

	@Override
	public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return update(sql, newArgTypePreparedStatementSetter(args, argTypes));
	}

	@Override
	public int update(String sql, Object... args) throws DataAccessException {
		return update(sql, newArgPreparedStatementSetter(args));
	}

	@Override
	public int[] batchUpdate(String sql, final BatchPreparedStatementSetter pss) throws DataAccessException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update [" + sql + "]");
		}

		return execute(sql, new PreparedStatementCallback<int[]>() {
			@Override
			public int[] doInPreparedStatement(PreparedStatement ps) throws SQLException {
				try {
					int batchSize = pss.getBatchSize();
					InterruptibleBatchPreparedStatementSetter ipss =
							(pss instanceof InterruptibleBatchPreparedStatementSetter ?
							(InterruptibleBatchPreparedStatementSetter) pss : null);
					if (JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
						for (int i = 0; i < batchSize; i++) {
							pss.setValues(ps, i);
							if (ipss != null && ipss.isBatchExhausted(i)) {
								break;
							}
							ps.addBatch();
						}
						return ps.executeBatch();
					}
					else {
						List<Integer> rowsAffected = new ArrayList<Integer>();
						for (int i = 0; i < batchSize; i++) {
							pss.setValues(ps, i);
							if (ipss != null && ipss.isBatchExhausted(i)) {
								break;
							}
							rowsAffected.add(ps.executeUpdate());
						}
						int[] rowsAffectedArray = new int[rowsAffected.size()];
						for (int i = 0; i < rowsAffectedArray.length; i++) {
							rowsAffectedArray[i] = rowsAffected.get(i);
						}
						return rowsAffectedArray;
					}
				}
				finally {
					if (pss instanceof ParameterDisposer) {
						((ParameterDisposer) pss).cleanupParameters();
					}
				}
			}
		});
	}

	@Override
	public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
		return batchUpdate(sql, batchArgs, new int[0]);
	}

	@Override
	public int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException {
		return BatchUpdateUtils.executeBatchUpdate(sql, batchArgs, argTypes, this);
	}

	@Override
	public <T> int[][] batchUpdate(String sql, final Collection<T> batchArgs, final int batchSize,
			final ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {

		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL batch update [" + sql + "] with a batch size of " + batchSize);
		}
		return execute(sql, new PreparedStatementCallback<int[][]>() {
			@Override
			public int[][] doInPreparedStatement(PreparedStatement ps) throws SQLException {
				List<int[]> rowsAffected = new ArrayList<int[]>();
				try {
					boolean batchSupported = true;
					if (!JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
						batchSupported = false;
						logger.warn("JDBC Driver does not support Batch updates; resorting to single statement execution");
					}
					int n = 0;
					for (T obj : batchArgs) {
						pss.setValues(ps, obj);
						n++;
						if (batchSupported) {
							ps.addBatch();
							if (n % batchSize == 0 || n == batchArgs.size()) {
								if (logger.isDebugEnabled()) {
									int batchIdx = (n % batchSize == 0) ? n / batchSize : (n / batchSize) + 1;
									int items = n - ((n % batchSize == 0) ? n / batchSize - 1 : (n / batchSize)) * batchSize;
									logger.debug("Sending SQL batch update #" + batchIdx + " with " + items + " items");
								}
								rowsAffected.add(ps.executeBatch());
							}
						}
						else {
							int i = ps.executeUpdate();
							rowsAffected.add(new int[] {i});
						}
					}
					int[][] result = new int[rowsAffected.size()][];
					for (int i = 0; i < result.length; i++) {
						result[i] = rowsAffected.get(i);
					}
					return result;
				}
				finally {
					if (pss instanceof ParameterDisposer) {
						((ParameterDisposer) pss).cleanupParameters();
					}
				}
			}
		});
	}

	//-------------------------------------------------------------------------
	// Methods dealing with callable statements
	//-------------------------------------------------------------------------

	@Override
	public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action)
			throws DataAccessException {

		Assert.notNull(csc, "CallableStatementCreator must not be null");
		Assert.notNull(action, "Callback object must not be null");
		if (logger.isDebugEnabled()) {
			String sql = getSql(csc);
			logger.debug("Calling stored procedure" + (sql != null ? " [" + sql  + "]" : ""));
		}

		Connection con = DataSourceUtils.getConnection(getDataSource());
		CallableStatement cs = null;
		try {
			Connection conToUse = con;
			if (this.nativeJdbcExtractor != null) {
				conToUse = this.nativeJdbcExtractor.getNativeConnection(con);
			}
			cs = csc.createCallableStatement(conToUse);
			applyStatementSettings(cs);
			CallableStatement csToUse = cs;
			if (this.nativeJdbcExtractor != null) {
				csToUse = this.nativeJdbcExtractor.getNativeCallableStatement(cs);
			}
			T result = action.doInCallableStatement(csToUse);
			handleWarnings(cs);
			return result;
		}
		catch (SQLException ex) {
			// 尽早释放连接, 以避免在尚未初始化异常转换器的情况下潜在的连接池死锁.
			if (csc instanceof ParameterDisposer) {
				((ParameterDisposer) csc).cleanupParameters();
			}
			String sql = getSql(csc);
			csc = null;
			JdbcUtils.closeStatement(cs);
			cs = null;
			DataSourceUtils.releaseConnection(con, getDataSource());
			con = null;
			throw getExceptionTranslator().translate("CallableStatementCallback", sql, ex);
		}
		finally {
			if (csc instanceof ParameterDisposer) {
				((ParameterDisposer) csc).cleanupParameters();
			}
			JdbcUtils.closeStatement(cs);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

	@Override
	public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
		return execute(new SimpleCallableStatementCreator(callString), action);
	}

	@Override
	public Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
			throws DataAccessException {

		final List<SqlParameter> updateCountParameters = new ArrayList<SqlParameter>();
		final List<SqlParameter> resultSetParameters = new ArrayList<SqlParameter>();
		final List<SqlParameter> callParameters = new ArrayList<SqlParameter>();
		for (SqlParameter parameter : declaredParameters) {
			if (parameter.isResultsParameter()) {
				if (parameter instanceof SqlReturnResultSet) {
					resultSetParameters.add(parameter);
				}
				else {
					updateCountParameters.add(parameter);
				}
			}
			else {
				callParameters.add(parameter);
			}
		}
		return execute(csc, new CallableStatementCallback<Map<String, Object>>() {
			@Override
			public Map<String, Object> doInCallableStatement(CallableStatement cs) throws SQLException {
				boolean retVal = cs.execute();
				int updateCount = cs.getUpdateCount();
				if (logger.isDebugEnabled()) {
					logger.debug("CallableStatement.execute() returned '" + retVal + "'");
					logger.debug("CallableStatement.getUpdateCount() returned " + updateCount);
				}
				Map<String, Object> returnedResults = createResultsMap();
				if (retVal || updateCount != -1) {
					returnedResults.putAll(extractReturnedResults(cs, updateCountParameters, resultSetParameters, updateCount));
				}
				returnedResults.putAll(extractOutputParameters(cs, callParameters));
				return returnedResults;
			}
		});
	}

	/**
	 * 从完成的存储过程中提取返回的ResultSet.
	 * 
	 * @param cs 存储过程的JDBC包装器
	 * @param updateCountParameters 存储过程的已声明的更新计数参数的参数列表
	 * @param resultSetParameters 存储过程的已声明resultSet参数的参数列表
	 * 
	 * @return 包含返回结果的Map
	 */
	protected Map<String, Object> extractReturnedResults(CallableStatement cs,
			List<SqlParameter> updateCountParameters, List<SqlParameter> resultSetParameters, int updateCount)
			throws SQLException {

		Map<String, Object> returnedResults = new HashMap<String, Object>();
		int rsIndex = 0;
		int updateIndex = 0;
		boolean moreResults;
		if (!this.skipResultsProcessing) {
			do {
				if (updateCount == -1) {
					if (resultSetParameters != null && resultSetParameters.size() > rsIndex) {
						SqlReturnResultSet declaredRsParam = (SqlReturnResultSet) resultSetParameters.get(rsIndex);
						returnedResults.putAll(processResultSet(cs.getResultSet(), declaredRsParam));
						rsIndex++;
					}
					else {
						if (!this.skipUndeclaredResults) {
							String rsName = RETURN_RESULT_SET_PREFIX + (rsIndex + 1);
							SqlReturnResultSet undeclaredRsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
							if (logger.isDebugEnabled()) {
								logger.debug("Added default SqlReturnResultSet parameter named '" + rsName + "'");
							}
							returnedResults.putAll(processResultSet(cs.getResultSet(), undeclaredRsParam));
							rsIndex++;
						}
					}
				}
				else {
					if (updateCountParameters != null && updateCountParameters.size() > updateIndex) {
						SqlReturnUpdateCount ucParam = (SqlReturnUpdateCount) updateCountParameters.get(updateIndex);
						String declaredUcName = ucParam.getName();
						returnedResults.put(declaredUcName, updateCount);
						updateIndex++;
					}
					else {
						if (!this.skipUndeclaredResults) {
							String undeclaredName = RETURN_UPDATE_COUNT_PREFIX + (updateIndex + 1);
							if (logger.isDebugEnabled()) {
								logger.debug("Added default SqlReturnUpdateCount parameter named '" + undeclaredName + "'");
							}
							returnedResults.put(undeclaredName, updateCount);
							updateIndex++;
						}
					}
				}
				moreResults = cs.getMoreResults();
				updateCount = cs.getUpdateCount();
				if (logger.isDebugEnabled()) {
					logger.debug("CallableStatement.getUpdateCount() returned " + updateCount);
				}
			}
			while (moreResults || updateCount != -1);
		}
		return returnedResults;
	}

	/**
	 * 从完成的存储过程中提取输出参数.
	 * 
	 * @param cs 存储过程的JDBC包装器
	 * @param parameters 存储过程的参数列表
	 * 
	 * @return 包含返回结果的Map
	 */
	protected Map<String, Object> extractOutputParameters(CallableStatement cs, List<SqlParameter> parameters)
			throws SQLException {

		Map<String, Object> returnedResults = new HashMap<String, Object>();
		int sqlColIndex = 1;
		for (SqlParameter param : parameters) {
			if (param instanceof SqlOutParameter) {
				SqlOutParameter outParam = (SqlOutParameter) param;
				if (outParam.isReturnTypeSupported()) {
					Object out = outParam.getSqlReturnType().getTypeValue(
							cs, sqlColIndex, outParam.getSqlType(), outParam.getTypeName());
					returnedResults.put(outParam.getName(), out);
				}
				else {
					Object out = cs.getObject(sqlColIndex);
					if (out instanceof ResultSet) {
						if (outParam.isResultSetSupported()) {
							returnedResults.putAll(processResultSet((ResultSet) out, outParam));
						}
						else {
							String rsName = outParam.getName();
							SqlReturnResultSet rsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
							returnedResults.putAll(processResultSet((ResultSet) out, rsParam));
							if (logger.isDebugEnabled()) {
								logger.debug("Added default SqlReturnResultSet parameter named '" + rsName + "'");
							}
						}
					}
					else {
						returnedResults.put(outParam.getName(), out);
					}
				}
			}
			if (!(param.isResultsParameter())) {
				sqlColIndex++;
			}
		}
		return returnedResults;
	}

	/**
	 * 从存储过程处理给定的ResultSet.
	 * 
	 * @param rs 要处理的ResultSet
	 * @param param 相应的存储过程参数
	 * 
	 * @return 包含返回结果的Map
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Map<String, Object> processResultSet(ResultSet rs, ResultSetSupportingSqlParameter param)
			throws SQLException {

		if (rs == null) {
			return Collections.emptyMap();
		}

		Map<String, Object> returnedResults = new HashMap<String, Object>();
		try {
			ResultSet rsToUse = rs;
			if (this.nativeJdbcExtractor != null) {
				rsToUse = this.nativeJdbcExtractor.getNativeResultSet(rs);
			}
			if (param.getRowMapper() != null) {
				RowMapper rowMapper = param.getRowMapper();
				Object result = (new RowMapperResultSetExtractor(rowMapper)).extractData(rsToUse);
				returnedResults.put(param.getName(), result);
			}
			else if (param.getRowCallbackHandler() != null) {
				RowCallbackHandler rch = param.getRowCallbackHandler();
				(new RowCallbackHandlerResultSetExtractor(rch)).extractData(rsToUse);
				returnedResults.put(param.getName(), "ResultSet returned from stored procedure was processed");
			}
			else if (param.getResultSetExtractor() != null) {
				Object result = param.getResultSetExtractor().extractData(rsToUse);
				returnedResults.put(param.getName(), result);
			}
		}
		finally {
			JdbcUtils.closeResultSet(rs);
		}
		return returnedResults;
	}


	//-------------------------------------------------------------------------
	// Implementation hooks and helper methods
	//-------------------------------------------------------------------------

	/**
	 * 创建一个新的RowMapper, 用于将列作为键值对读取.
	 * 
	 * @return 要使用的RowMapper
	 */
	protected RowMapper<Map<String, Object>> getColumnMapRowMapper() {
		return new ColumnMapRowMapper();
	}

	/**
	 * 创建一个新的RowMapper, 用于从单个列读取结果对象.
	 * 
	 * @param requiredType 每个结果对象应匹配的类型
	 * 
	 * @return 要使用的RowMapper
	 */
	protected <T> RowMapper<T> getSingleColumnRowMapper(Class<T> requiredType) {
		return new SingleColumnRowMapper<T>(requiredType);
	}

	/**
	 * 创建要用作结果映射的Map实例.
	 * <p>如果{@link #resultsMapCaseInsensitive}已设置为 true, 则将创建{@link LinkedCaseInsensitiveMap};
	 * 否则, 将创建{@link LinkedHashMap}.
	 * 
	 * @return 结果Map实例
	 */
	protected Map<String, Object> createResultsMap() {
		if (isResultsMapCaseInsensitive()) {
			return new LinkedCaseInsensitiveMap<Object>();
		}
		else {
			return new LinkedHashMap<String, Object>();
		}
	}

	/**
	 * 准备给定的JDBC语句 (或PreparedStatement, 或CallableStatement), 应用语句设置, 如获取大小, 最大行数, 和查询超时.
	 * 
	 * @param stmt 要准备的JDBC语句
	 * 
	 * @throws SQLException 如果被JDBC API抛出
	 */
	protected void applyStatementSettings(Statement stmt) throws SQLException {
		int fetchSize = getFetchSize();
		if (fetchSize != -1) {
			stmt.setFetchSize(fetchSize);
		}
		int maxRows = getMaxRows();
		if (maxRows != -1) {
			stmt.setMaxRows(maxRows);
		}
		DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
	}

	/**
	 * 使用传入的args创建一个新的基于arg的PreparedStatementSetter.
	 * <p>默认情况下, 将创建一个{@link ArgumentPreparedStatementSetter}.
	 * 此方法允许子类重写创建.
	 * 
	 * @param args 带参数的对象数组
	 * 
	 * @return 要使用的新PreparedStatementSetter
	 */
	protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
		return new ArgumentPreparedStatementSetter(args);
	}

	/**
	 * 使用传入的args和类型创建一个新的基于arg-type的PreparedStatementSetter.
	 * <p>默认情况下, 将创建一个{@link ArgumentTypePreparedStatementSetter}.
	 * 此方法允许子类重写创建.
	 * 
	 * @param args 带参数的对象数组
	 * @param argTypes 关联参数的SQLType数组
	 * 
	 * @return 要使用的新PreparedStatementSetter
	 */
	protected PreparedStatementSetter newArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
		return new ArgumentTypePreparedStatementSetter(args, argTypes);
	}

	/**
	 * 如果不忽略警告, 则抛出SQLWarningException, 否则记录警告 (在debug级别).
	 * 
	 * @param stmt 当前的JDBC语句
	 * 
	 * @throws SQLWarningException 如果不是忽略警告
	 */
	protected void handleWarnings(Statement stmt) throws SQLException {
		if (isIgnoreWarnings()) {
			if (logger.isDebugEnabled()) {
				SQLWarning warningToLog = stmt.getWarnings();
				while (warningToLog != null) {
					logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" +
							warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
					warningToLog = warningToLog.getNextWarning();
				}
			}
		}
		else {
			handleWarnings(stmt.getWarnings());
		}
	}

	/**
	 * 如果遇到实际警告, 则抛出SQLWarningException.
	 * 
	 * @param warning 来自当前语句的警告对象.
	 * 可能是{@code null}, 在这种情况下, 此方法不执行任何操作.
	 * 
	 * @throws SQLWarningException 如果发出实际警告
	 */
	protected void handleWarnings(SQLWarning warning) throws SQLWarningException {
		if (warning != null) {
			throw new SQLWarningException("Warning not ignored", warning);
		}
	}

	/**
	 * 从潜在的提供者对象确定SQL.
	 * 
	 * @param sqlProvider 可能是SqlProvider的对象
	 * 
	 * @return SQL字符串, 或{@code null}
	 */
	private static String getSql(Object sqlProvider) {
		if (sqlProvider instanceof SqlProvider) {
			return ((SqlProvider) sqlProvider).getSql();
		}
		else {
			return null;
		}
	}


	/**
	 * 调用JDBC连接的调用处理程序.
	 * 还准备返回的Statement (Prepared/CallbackStatement)对象.
	 */
	private class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		public CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自ConnectionProxy接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用PersistenceManager代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("unwrap")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return proxy;
				}
			}
			else if (method.getName().equals("isWrapperFor")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return true;
				}
			}
			else if (method.getName().equals("close")) {
				// 处理close方法: 抑制, 无效.
				return null;
			}
			else if (method.getName().equals("isClosed")) {
				return false;
			}
			else if (method.getName().equals("getTargetConnection")) {
				// 处理getTargetConnection方法: 返回底层Connection.
				return this.target;
			}

			// 在目标Connection上调用方法.
			try {
				Object retVal = method.invoke(this.target, args);

				// 如果返回值是JDBC语句, 则应用语句设置 (提取大小, 最大行数, 事务超时).
				if (retVal instanceof Statement) {
					applyStatementSettings(((Statement) retVal));
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * PreparedStatementCreator的简单适配器, 允许使用普通的SQL语句.
	 */
	private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		private final String sql;

		public SimplePreparedStatementCreator(String sql) {
			Assert.notNull(sql, "SQL must not be null");
			this.sql = sql;
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			return con.prepareStatement(this.sql);
		}

		@Override
		public String getSql() {
			return this.sql;
		}
	}


	/**
	 * CallableStatementCreator的简单适配器, 允许使用普通的SQL语句.
	 */
	private static class SimpleCallableStatementCreator implements CallableStatementCreator, SqlProvider {

		private final String callString;

		public SimpleCallableStatementCreator(String callString) {
			Assert.notNull(callString, "Call string must not be null");
			this.callString = callString;
		}

		@Override
		public CallableStatement createCallableStatement(Connection con) throws SQLException {
			return con.prepareCall(this.callString);
		}

		@Override
		public String getSql() {
			return this.callString;
		}
	}


	/**
	 * 适配器, 用于在ResultSetExtractor中使用RowCallbackHandler.
	 * <p>使用常规 ResultSet, 因此在使用它时必须小心:
	 * 不会将其用于导航, 因为这可能会导致不可预测的后果.
	 */
	private static class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor<Object> {

		private final RowCallbackHandler rch;

		public RowCallbackHandlerResultSetExtractor(RowCallbackHandler rch) {
			this.rch = rch;
		}

		@Override
		public Object extractData(ResultSet rs) throws SQLException {
			while (rs.next()) {
				this.rch.processRow(rs);
			}
			return null;
		}
	}
}
