package org.springframework.jdbc.object;

import java.util.Map;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 表示SQL更新的可重用操作对象.
 *
 * <p>此类提供了许多{@code update}方法, 类似于查询对象的{@code execute}方法.
 *
 * <p>此类是具体的. 虽然它可以子类化 (例如添加自定义更新方法), 但可以通过设置SQL和声明参数来轻松地对其进行参数化.
 *
 * <p>与Spring Framework附带的所有{@code RdbmsOperation}类一样, {@code SqlQuery}实例在初始化完成后是线程安全的.
 * 也就是说, 在通过setter方法构造和配置它们之后, 可以从多个线程安全地使用它们.
 */
public class SqlUpdate extends SqlOperation {

	/**
	 * 更新可能影响的最大行数. 如果更多受影响, 将抛出异常. 如果为0则忽略.
	 */
	private int maxRowsAffected = 0;

	/**
	 * 必须受影响的确切行数.
	 * 如果为0则忽略.
	 */
	private int requiredRowsAffected = 0;


	/**
	 * 允许用作JavaBean的构造方法. 在编译和使用之前必须提供DataSource和SQL.
	 */
	public SqlUpdate() {
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要执行的SQL语句
	 */
	public SqlUpdate(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要执行的SQL语句
	 * @param types 参数的SQL类型, 如{@code java.sql.Types}类中所定义
	 */
	public SqlUpdate(DataSource ds, String sql, int[] types) {
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要执行的SQL语句
	 * @param types 参数的SQL类型, 如{@code java.sql.Types}类中所定义
	 * @param maxRowsAffected 更新可能影响的最大行数
	 */
	public SqlUpdate(DataSource ds, String sql, int[] types, int maxRowsAffected) {
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
		this.maxRowsAffected = maxRowsAffected;
	}


	/**
	 * 设置此更新可能影响的最大行数.
	 * 默认值为0, 不限制受影响的行数.
	 * 
	 * @param maxRowsAffected 此更新可能影响的最大行数, 如果没有此类的更新方法, 则将其视为错误
	 */
	public void setMaxRowsAffected(int maxRowsAffected) {
		this.maxRowsAffected = maxRowsAffected;
	}

	/**
	 * 设置必须受此更新影响的<i>确切</i>行数.
	 * 默认值为0, 允许影响任意数量的行.
	 * <p>这是设置可能受影响的<i>最大</i>行数的替代方法.
	 * 
	 * @param requiredRowsAffected 此更新必须受影响的确切行数, 如果没有此类的更新方法, 则将其视为错误
	 */
	public void setRequiredRowsAffected(int requiredRowsAffected) {
		this.requiredRowsAffected = requiredRowsAffected;
	}

	/**
	 * 根据指定的最大数量或所需数量检查给定的受影响行数.
	 * 
	 * @param rowsAffected 受影响的行数
	 * 
	 * @throws JdbcUpdateAffectedIncorrectNumberOfRowsException 如果实际受影响的行超出范围
	 */
	protected void checkRowsAffected(int rowsAffected) throws JdbcUpdateAffectedIncorrectNumberOfRowsException {
		if (this.maxRowsAffected > 0 && rowsAffected > this.maxRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(getSql(), this.maxRowsAffected, rowsAffected);
		}
		if (this.requiredRowsAffected > 0 && rowsAffected != this.requiredRowsAffected) {
			throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(getSql(), this.requiredRowsAffected, rowsAffected);
		}
	}


	/**
	 * 执行更新给定参数的通用方法.
	 * 所有其他更新方法都会调用此方法.
	 * 
	 * @param params 参数对象数组
	 * 
	 * @return 受更新影响的行数
	 */
	public int update(Object... params) throws DataAccessException {
		validateParameters(params);
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(params));
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}

	/**
	 * 使用KeyHolder执行给定参数的更新并检索生成的键的方法.
	 * 
	 * @param params 参数对象数组
	 * @param generatedKeyHolder 保存生成的键的KeyHolder
	 * 
	 * @return 受更新影响的行数
	 */
	public int update(Object[] params, KeyHolder generatedKeyHolder) throws DataAccessException {
		if (!isReturnGeneratedKeys() && getGeneratedKeysColumnNames() == null) {
			throw new InvalidDataAccessApiUsageException(
					"The update method taking a KeyHolder should only be used when generated keys have " +
					"been configured by calling either 'setReturnGeneratedKeys' or " +
					"'setGeneratedKeysColumnNames'.");
		}
		validateParameters(params);
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(params), generatedKeyHolder);
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}

	/**
	 * 无参数执行更新.
	 */
	public int update() throws DataAccessException {
		return update((Object[]) null);
	}

	/**
	 * 给定一个int 参数执行更新.
	 */
	public int update(int p1) throws DataAccessException {
		return update(new Object[] {p1});
	}

	/**
	 * 给定两个int参数执行更新.
	 */
	public int update(int p1, int p2) throws DataAccessException {
		return update(new Object[] {p1, p2});
	}

	/**
	 * 给定一个long参数执行更新.
	 */
	public int update(long p1) throws DataAccessException {
		return update(new Object[] {p1});
	}

	/**
	 * 给定两个long参数执行更新.
	 */
	public int update(long p1, long p2) throws DataAccessException {
		return update(new Object[] {p1, p2});
	}

	/**
	 * 给定一个String 参数执行更新.
	 */
	public int update(String p) throws DataAccessException {
		return update(new Object[] {p});
	}

	/**
	 * 给定两个String参数执行更新.
	 */
	public int update(String p1, String p2) throws DataAccessException {
		return update(new Object[] {p1, p2});
	}

	/**
	 * 给定命名参数执行更新.
	 * 所有其他更新方法都会调用此方法.
	 * 
	 * @param paramMap 参数名称到参数对象的Map, 匹配SQL语句中指定的命名参数
	 * 
	 * @return 受更新影响的行数
	 */
	public int updateByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		validateNamedParameters(paramMap);
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(sqlToUse, params));
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}

	/**
	 * 使用KeyHolder执行给定参数的更新并检索生成的键的方法.
	 * 
	 * @param paramMap 参数名称到参数对象的Map, 匹配SQL语句中指定的命名参数
	 * @param generatedKeyHolder 保存生成的键的KeyHolder
	 * 
	 * @return 受更新影响的行数
	 */
	public int updateByNamedParam(Map<String, ?> paramMap, KeyHolder generatedKeyHolder) throws DataAccessException {
		validateNamedParameters(paramMap);
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
		int rowsAffected = getJdbcTemplate().update(newPreparedStatementCreator(sqlToUse, params), generatedKeyHolder);
		checkRowsAffected(rowsAffected);
		return rowsAffected;
	}
}
