package org.springframework.jdbc.core.namedparam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlRowSetResultSetExtractor;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

/**
 * 具有一组基本的JDBC操作的模板类, 允许使用命名参数而不是传统的 '?' 占位符.
 *
 * <p>一旦执行时从命名参数替换为JDBC样式 '?'占位符, 该类将委托给一个包装的{@link #getJdbcOperations() JdbcTemplate}.
 * 它还允许将{@link java.util.List}值扩展为适当数量的占位符.
 *
 * <p>公开底层{@link org.springframework.jdbc.core.JdbcTemplate}以便于访问传统的{@link org.springframework.jdbc.core.JdbcTemplate}方法.
 *
 * <p><b>NOTE: 一旦配置, 该类的实例就是线程安全的.</b>
 */
public class NamedParameterJdbcTemplate implements NamedParameterJdbcOperations {

	/** 此模板的SQL缓存的默认最大条目数: 256 */
	public static final int DEFAULT_CACHE_LIMIT = 256;


	/** 正在包装的JdbcTemplate */
	private final JdbcOperations classicJdbcTemplate;

	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	/** 原始SQL String的缓存到ParsedSql表示 */
	@SuppressWarnings("serial")
	private final Map<String, ParsedSql> parsedSqlCache =
			new LinkedHashMap<String, ParsedSql>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, ParsedSql> eldest) {
					return size() > getCacheLimit();
				}
			};


	/**
	 * 为给定的{@link DataSource}创建一个新的NamedParameterJdbcTemplate.
	 * <p>创建一个经典的Spring {@link org.springframework.jdbc.core.JdbcTemplate}并包装它.
	 * 
	 * @param dataSource 要访问的JDBC DataSource
	 */
	public NamedParameterJdbcTemplate(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.classicJdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 为给定的经典Spring {@link org.springframework.jdbc.core.JdbcTemplate}创建一个新的NamedParameterJdbcTemplate.
	 * 
	 * @param classicJdbcTemplate 要包装的经典Spring JdbcTemplate
	 */
	public NamedParameterJdbcTemplate(JdbcOperations classicJdbcTemplate) {
		Assert.notNull(classicJdbcTemplate, "JdbcTemplate must not be null");
		this.classicJdbcTemplate = classicJdbcTemplate;
	}


	/**
	 * 公开经典的Spring JdbcTemplate操作以允许调用不太常用的方法.
	 */
	@Override
	public JdbcOperations getJdbcOperations() {
		return this.classicJdbcTemplate;
	}

	/**
	 * 指定此模板的SQL缓存的最大条目数.
	 * 默认 256.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * 返回此模板的SQL缓存的最大条目数.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}


	@Override
	public <T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return getJdbcOperations().execute(getPreparedStatementCreator(sql, paramSource), action);
	}

	@Override
	public <T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action)
			throws DataAccessException {

		return execute(sql, new MapSqlParameterSource(paramMap), action);
	}

	@Override
	public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
		return execute(sql, EmptySqlParameterSource.INSTANCE, action);
	}

	@Override
	public <T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rse);
	}

	@Override
	public <T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse)
			throws DataAccessException {

		return query(sql, new MapSqlParameterSource(paramMap), rse);
	}

	@Override
	public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
		return query(sql, EmptySqlParameterSource.INSTANCE, rse);
	}

	@Override
	public void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch)
			throws DataAccessException {

		getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rch);
	}

	@Override
	public void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch)
			throws DataAccessException {

		query(sql, new MapSqlParameterSource(paramMap), rch);
	}

	@Override
	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
		query(sql, EmptySqlParameterSource.INSTANCE, rch);
	}

	@Override
	public <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		return getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
	}

	@Override
	public <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
			throws DataAccessException {

		return query(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		return query(sql, EmptySqlParameterSource.INSTANCE, rowMapper);
	}

	@Override
	public <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper)
			throws DataAccessException {

		List<T> results = getJdbcOperations().query(getPreparedStatementCreator(sql, paramSource), rowMapper);
		return DataAccessUtils.requiredSingleResult(results);
	}

	@Override
	public <T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T>rowMapper)
			throws DataAccessException {

		return queryForObject(sql, new MapSqlParameterSource(paramMap), rowMapper);
	}

	@Override
	public <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, paramSource, new SingleColumnRowMapper<T>(requiredType));
	}

	@Override
	public <T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType)
			throws DataAccessException {

		return queryForObject(sql, paramMap, new SingleColumnRowMapper<T>(requiredType));
	}

	@Override
	public Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return queryForObject(sql, paramSource, new ColumnMapRowMapper());
	}

	@Override
	public Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException {
		return queryForObject(sql, paramMap, new ColumnMapRowMapper());
	}

	@Override
	public <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType)
			throws DataAccessException {

		return query(sql, paramSource, new SingleColumnRowMapper<T>(elementType));
	}

	@Override
	public <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType)
			throws DataAccessException {

		return queryForList(sql, new MapSqlParameterSource(paramMap), elementType);
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource)
			throws DataAccessException {

		return query(sql, paramSource, new ColumnMapRowMapper());
	}

	@Override
	public List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap)
			throws DataAccessException {

		return queryForList(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return getJdbcOperations().query(
				getPreparedStatementCreator(sql, paramSource), new SqlRowSetResultSetExtractor());
	}

	@Override
	public SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException {
		return queryForRowSet(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public int update(String sql, SqlParameterSource paramSource) throws DataAccessException {
		return getJdbcOperations().update(getPreparedStatementCreator(sql, paramSource));
	}

	@Override
	public int update(String sql, Map<String, ?> paramMap) throws DataAccessException {
		return update(sql, new MapSqlParameterSource(paramMap));
	}

	@Override
	public int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder)
			throws DataAccessException {

		return update(sql, paramSource, generatedKeyHolder, null);
	}

	@Override
	public int update(
			String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames)
			throws DataAccessException {

		ParsedSql parsedSql = getParsedSql(sql);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		List<SqlParameter> declaredParameters = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);
		PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(sqlToUse, declaredParameters);
		if (keyColumnNames != null) {
			pscf.setGeneratedKeysColumnNames(keyColumnNames);
		}
		else {
			pscf.setReturnGeneratedKeys(true);
		}
		return getJdbcOperations().update(pscf.newPreparedStatementCreator(params), generatedKeyHolder);
	}

	@Override
	public int[] batchUpdate(String sql, Map<String, ?>[] batchValues) {
		return batchUpdate(sql, SqlParameterSourceUtils.createBatch(batchValues));
	}

	@Override
	public int[] batchUpdate(String sql, SqlParameterSource[] batchArgs) {
		return NamedParameterBatchUpdateUtils.executeBatchUpdateWithNamedParameters(
				getParsedSql(sql), batchArgs, getJdbcOperations());
	}


	/**
	 * 根据给定的SQL和命名参数构建{@link PreparedStatementCreator}.
	 * <p>Note: 直接从所有 {@code query}变体调用.
	 * 未用于生成键处理的{@code update}变体.
	 * 
	 * @param sql 要执行的SQL语句
	 * @param paramSource 要绑定参数的容器
	 * 
	 * @return 相应的{@link PreparedStatementCreator}
	 */
	protected PreparedStatementCreator getPreparedStatementCreator(String sql, SqlParameterSource paramSource) {
		ParsedSql parsedSql = getParsedSql(sql);
		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		List<SqlParameter> declaredParameters = NamedParameterUtils.buildSqlParameterList(parsedSql, paramSource);
		PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(sqlToUse, declaredParameters);
		return pscf.newPreparedStatementCreator(params);
	}

	/**
	 * 获取给定SQL语句的已解析表示.
	 * <p>默认实现使用LRU缓存, 上限为256个条目.
	 * 
	 * @param sql 原始的SQL语句
	 * 
	 * @return 已解析的SQL语句的表示形式
	 */
	protected ParsedSql getParsedSql(String sql) {
		if (getCacheLimit() <= 0) {
			return NamedParameterUtils.parseSqlStatement(sql);
		}
		synchronized (this.parsedSqlCache) {
			ParsedSql parsedSql = this.parsedSqlCache.get(sql);
			if (parsedSql == null) {
				parsedSql = NamedParameterUtils.parseSqlStatement(sql);
				this.parsedSqlCache.put(sql, parsedSql);
			}
			return parsedSql;
		}
	}
}
