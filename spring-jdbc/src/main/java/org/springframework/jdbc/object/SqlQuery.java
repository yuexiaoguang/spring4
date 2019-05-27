package org.springframework.jdbc.object;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

/**
 * 表示SQL查询的可重用操作对象.
 *
 * <p>子类必须实现{@link #newRowMapper}方法, 以提供一个对象,
 * 该对象可以提取在迭代执行查询期间创建的{@code ResultSet}的结果.
 *
 * <p>此类提供了许多public {@code execute}方法, 类似于不同的JDO查询execute方法.
 * 子类可以依赖于这些继承的方法之一, 也可以添加自己的自定义执行方法, 使用有意义的名称和类型参数 (绝对是最佳实践).
 * 每个自定义查询方法都将调用此类的无类型查询方法之一.
 *
 * <p>与Spring Framework附带的所有{@code RdbmsOperation}类一样, {@code SqlQuery}实例在初始化完成后是线程安全的.
 * 也就是说, 在通过setter方法构造和配置它们之后, 可以从多个线程安全地使用它们.
 */
public abstract class SqlQuery<T> extends SqlOperation {

	/** 预期的行数; 如果为0, 未知. */
	private int rowsExpected = 0;


	/**
	 * 允许用作JavaBean的构造方法.
	 * <p>必须在编译和使用之前提供{@code DataSource}和SQL.
	 */
	public SqlQuery() {
	}

	/**
	 * @param ds 用于获取连接的{@code DataSource}
	 * @param sql 要执行的SQL; 也可以通过覆盖{@link #getSql()}方法在运行时提供SQL.
	 */
	public SqlQuery(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}


	/**
	 * 设置预期的行数.
	 * <p>这可用于确保结果的有效存储. 默认行为是不期望任何特定数量的行.
	 */
	public void setRowsExpected(int rowsExpected) {
		this.rowsExpected = rowsExpected;
	}

	/**
	 * 获取预期的行数.
	 */
	public int getRowsExpected() {
		return this.rowsExpected;
	}


	/**
	 * 中央执行方法. 所有未命名的参数执行都通过此方法执行.
	 * 
	 * @param params 参数, 类似于JDO查询参数.
	 * 基础类型参数必须由其对象包装类型表示. 参数的顺序很重要.
	 * @param context 传递给{@code mapRow}回调方法的上下文信息.
	 * JDBC操作本身不依赖于此参数, 但它对于创建结果列表的对象非常有用.
	 * 
	 * @return 一个对象列表, 每行一个ResultSet. 通常所有这些都属于同一类, 尽管可以使用不同的类型.
	 */
	public List<T> execute(Object[] params, Map<?, ?> context) throws DataAccessException {
		validateParameters(params);
		RowMapper<T> rowMapper = newRowMapper(params, context);
		return getJdbcTemplate().query(newPreparedStatementCreator(params), rowMapper);
	}

	/**
	 * 无上下文执行.
	 * 
	 * @param params 查询的参数.
	 * 基础类型参数必须由其对象包装类型表示. 参数的顺序很重要.
	 */
	public List<T> execute(Object... params) throws DataAccessException {
		return execute(params, null);
	}

	/**
	 * 无参数执行.
	 * 
	 * @param context 对象创建的上下文信息
	 */
	public List<T> execute(Map<?, ?> context) throws DataAccessException {
		return execute((Object[]) null, context);
	}

	/**
	 * 无参数或上下文执行.
	 */
	public List<T> execute() throws DataAccessException {
		return execute((Object[]) null);
	}

	/**
	 * 使用单个int参数和上下文执行.
	 * 
	 * @param p1 单个int参数
	 * @param context 对象创建的上下文信息
	 */
	public List<T> execute(int p1, Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个int参数执行.
	 * 
	 * @param p1 单个int参数
	 */
	public List<T> execute(int p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 使用两个int参数和上下文执行.
	 * 
	 * @param p1 第一个int参数
	 * @param p2 第二个int参数
	 * @param context 对象创建的上下文信息
	 */
	public List<T> execute(int p1, int p2, Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1, p2}, context);
	}

	/**
	 * 使用两个int参数执行.
	 * 
	 * @param p1 第一个int参数
	 * @param p2 第二个int参数
	 */
	public List<T> execute(int p1, int p2) throws DataAccessException {
		return execute(p1, p2, null);
	}

	/**
	 * 使用单个long参数和上下文执行.
	 * 
	 * @param p1 单个long参数
	 * @param context 对象创建的上下文信息
	 */
	public List<T> execute(long p1, Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个long参数执行.
	 * 
	 * @param p1 单个long参数
	 */
	public List<T> execute(long p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 使用单个String参数和上下文执行.
	 * 
	 * @param p1 单个String参数
	 * @param context 对象创建的上下文信息
	 */
	public List<T> execute(String p1, Map<?, ?> context) throws DataAccessException {
		return execute(new Object[] {p1}, context);
	}

	/**
	 * 使用单个String参数执行.
	 * 
	 * @param p1 单个String参数
	 */
	public List<T> execute(String p1) throws DataAccessException {
		return execute(p1, null);
	}

	/**
	 * 中央执行方法. 所有命名参数执行都通过此方法.
	 * 
	 * @param paramMap 与声明SqlParameters时指定的名称关联的参数.
	 * 基础类型参数必须由其对象包装类型表示.
	 * 参数的顺序并不重要, 因为它们是在SqlParameterMap中提供的, SqlParameterMap是Map接口的实现.
	 * @param context 传递给{@code mapRow}回调方法的上下文信息.
	 * JDBC操作本身不依赖于此参数, 但它对于创建结果列表的对象非常有用.
	 * 
	 * @return 一个对象列表, 每行一个ResultSet.
	 * 通常所有这些都属于同一类, 尽管可以使用不同的类型.
	 */
	public List<T> executeByNamedParam(Map<String, ?> paramMap, Map<?, ?> context) throws DataAccessException {
		validateNamedParameters(paramMap);
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
		RowMapper<T> rowMapper = newRowMapper(params, context);
 		return getJdbcTemplate().query(newPreparedStatementCreator(sqlToUse, params), rowMapper);
	}

	/**
	 * 无上下文执行的便捷方法.
	 * 
	 * @param paramMap 与声明SqlParameters时指定的名称关联的参数.
	 * 基础类型参数必须由其对象包装类型表示. 参数的顺序并不重要.
	 */
	public List<T> executeByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		return executeByNamedParam(paramMap, null);
	}


	/**
	 * 所有其他{@code findObject}方法使用的通用对象查找器方法.
	 * 对象查找器方法类似于EJB实体bean查找器, 因为如果它们返回多个结果, 则认为它是错误的.
	 * 
	 * @return 结果对象, 如果找不到, 则为{@code null}. 子类可以选择将此视为错误并抛出异常.
	 */
	public T findObject(Object[] params, Map<?, ?> context) throws DataAccessException {
		List<T> results = execute(params, context);
		return DataAccessUtils.singleResult(results);
	}

	/**
	 * 在没有上下文的情况下查找单个对象.
	 */
	public T findObject(Object... params) throws DataAccessException {
		return findObject(params, null);
	}

	/**
	 * 给定单个int参数和上下文查找单个对象.
	 */
	public T findObject(int p1, Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * 给定单个int参数查找单个对象.
	 */
	public T findObject(int p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * 给定两个int参数和一个上下文查找单个对象.
	 */
	public T findObject(int p1, int p2, Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1, p2}, context);
	}

	/**
	 * 给出两个int参数查找单个对象.
	 */
	public T findObject(int p1, int p2) throws DataAccessException {
		return findObject(p1, p2, null);
	}

	/**
	 * 给定单个long参数和上下文查找单个对象.
	 */
	public T findObject(long p1, Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * 给定单个long参数查找单个对象.
	 */
	public T findObject(long p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * 给定单个String参数和上下文查找单个对象.
	 */
	public T findObject(String p1, Map<?, ?> context) throws DataAccessException {
		return findObject(new Object[] {p1}, context);
	}

	/**
	 * 给定单个String参数查找单个对象.
	 */
	public T findObject(String p1) throws DataAccessException {
		return findObject(p1, null);
	}

	/**
	 * 命名参数的通用对象查找器方法.
	 * 
	 * @param paramMap 参数名称到参数对象的Map, 匹配SQL语句中指定的命名参数.
	 * 顺序并不重要.
	 * @param context 传递给{@code mapRow}回调方法的上下文信息.
	 * JDBC操作本身不依赖于此参数, 但它对于创建结果列表的对象非常有用.
	 * 
	 * @return 一个对象列表, 每行一个ResultSet.
	 * 通常所有这些都属于同一类, 尽管可以使用不同的类型.
	 */
	public T findObjectByNamedParam(Map<String, ?> paramMap, Map<?, ?> context) throws DataAccessException {
		List<T> results = executeByNamedParam(paramMap, context);
		return DataAccessUtils.singleResult(results);
	}

	/**
	 * 无上下文执行.
	 * 
	 * @param paramMap 参数名称到参数对象的Map, 匹配SQL语句中指定的命名参数.
	 * 顺序并不重要.
	 */
	public T findObjectByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		return findObjectByNamedParam(paramMap, null);
	}


	/**
	 * 子类必须实现此方法以提取每行的对象, 由<cod>execute</code>方法作为聚合{@link List}返回.
	 * 
	 * @param parameters {@code execute()}方法的参数; 如果没有参数, 可能是{@code null}.
	 * @param context 传递给{@code mapRow}回调方法的上下文信息.
	 * JDBC操作本身不依赖于此参数, 但它对于创建结果列表的对象非常有用.
	 */
	protected abstract RowMapper<T> newRowMapper(Object[] parameters, Map<?, ?> context);

}
