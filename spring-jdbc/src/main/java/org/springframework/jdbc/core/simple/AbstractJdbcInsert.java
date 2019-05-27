package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.metadata.TableMetaDataContext;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.Assert;

/**
 * 抽象类, 提供基于配置选项和数据库元数据的简单插入的基本功能.
 *
 * <p>该类为{@link SimpleJdbcInsert}提供基本SPI.
 */
public abstract class AbstractJdbcInsert {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 用于执行SQL的低级类 */
	private final JdbcTemplate jdbcTemplate;

	/** 用于检索和管理数据库元数据的上下文 */
	private final TableMetaDataContext tableMetaDataContext = new TableMetaDataContext();

	/** 要在insert语句中使用的列对象的列表 */
	private final List<String> declaredColumns = new ArrayList<String>();

	/** 保存生成的键的列名 */
	private String[] generatedKeyNames = new String[0];

	/**
	 * 此操作是否已编译?
	 * 编译意味着至少检查是否已提供DataSource或JdbcTemplate.
	 */
	private volatile boolean compiled = false;

	/** 用于insert语句的生成的字符串 */
	private String insertString;

	/** 插入列的SQL类型信息 */
	private int[] insertTypes;


	/**
	 * @param dataSource 要使用的DataSource
	 */
	protected AbstractJdbcInsert(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * @param jdbcTemplate 要使用的JdbcTemplate
	 */
	protected AbstractJdbcInsert(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
		setNativeJdbcExtractor(jdbcTemplate.getNativeJdbcExtractor());
	}


	//-------------------------------------------------------------------------
	// Methods dealing with configuration properties
	//-------------------------------------------------------------------------

	/**
	 * 获取配置的{@link JdbcTemplate}.
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * 设置要插入的表的名称.
	 */
	public void setTableName(String tableName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setTableName(tableName);
	}

	/**
	 * 获取要插入的表的名称.
	 */
	public String getTableName() {
		return this.tableMetaDataContext.getTableName();
	}

	/**
	 * 设置此插入的schema名称.
	 */
	public void setSchemaName(String schemaName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * 获取此插入的schema名称.
	 */
	public String getSchemaName() {
		return this.tableMetaDataContext.getSchemaName();
	}

	/**
	 * 设置此插入的catalog名称.
	 */
	public void setCatalogName(String catalogName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * 获取此插入的catalog名称.
	 */
	public String getCatalogName() {
		return this.tableMetaDataContext.getCatalogName();
	}

	/**
	 * 设置要使用的列的名称.
	 */
	public void setColumnNames(List<String> columnNames) {
		checkIfConfigurationModificationIsAllowed();
		this.declaredColumns.clear();
		this.declaredColumns.addAll(columnNames);
	}

	/**
	 * 获取要使用的列的名称.
	 */
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(this.declaredColumns);
	}

	/**
	 * 指定单个生成的键列的名称.
	 */
	public void setGeneratedKeyName(String generatedKeyName) {
		checkIfConfigurationModificationIsAllowed();
		this.generatedKeyNames = new String[] {generatedKeyName};
	}

	/**
	 * 设置任何生成的键的名称.
	 */
	public void setGeneratedKeyNames(String... generatedKeyNames) {
		checkIfConfigurationModificationIsAllowed();
		this.generatedKeyNames = generatedKeyNames;
	}

	/**
	 * 获取任何生成的键的名称.
	 */
	public String[] getGeneratedKeyNames() {
		return this.generatedKeyNames;
	}

	/**
	 * 指定是否应使用调用的参数元数据.
	 * 默认{@code true}.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.tableMetaDataContext.setAccessTableColumnMetaData(accessTableColumnMetaData);
	}

	/**
	 * 指定是否应更改包含同义词的默认值.
	 * 默认{@code false}.
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.tableMetaDataContext.setOverrideIncludeSynonymsDefault(override);
	}

	/**
	 * 设置用于在必要时检索本机连接的{@link NativeJdbcExtractor}
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.tableMetaDataContext.setNativeJdbcExtractor(nativeJdbcExtractor);
	}

	/**
	 * 获取要使用的插入字符串.
	 */
	public String getInsertString() {
		return this.insertString;
	}

	/**
	 * 获取要用于插入的{@link java.sql.Types}数组.
	 */
	public int[] getInsertTypes() {
		return this.insertTypes;
	}


	//-------------------------------------------------------------------------
	// Methods handling compilation issues
	//-------------------------------------------------------------------------

	/**
	 * 使用提供的参数和元数据以及其他设置编译此JdbcInsert.
	 * 这将最终确定此对象的配置, 并忽略后续的编译尝试.
	 * 这将在第一次执行未编译的插入时被隐式调用.
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果对象尚未正确初始化, 例如, 如果未提供任何DataSource
	 */
	public final synchronized void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getTableName() == null) {
				throw new InvalidDataAccessApiUsageException("Table name is required");
			}
			try {
				this.jdbcTemplate.afterPropertiesSet();
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(ex.getMessage());
			}
			compileInternal();
			this.compiled = true;
			if (logger.isDebugEnabled()) {
				logger.debug("JdbcInsert for table [" + getTableName() + "] compiled");
			}
		}
	}

	/**
	 * 执行实际编译的委托方法.
	 * <p>子类可以覆盖此模板方法以执行自己的编译.
	 * 在此基类的编译完成后调用.
	 */
	protected void compileInternal() {
		this.tableMetaDataContext.processMetaData(
				getJdbcTemplate().getDataSource(), getColumnNames(), getGeneratedKeyNames());
		this.insertString = this.tableMetaDataContext.createInsertString(getGeneratedKeyNames());
		this.insertTypes = this.tableMetaDataContext.createInsertTypes();
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled insert object: insert string is [" + getInsertString() + "]");
		}
		onCompileInternal();
	}

	/**
	 * 子类可以覆盖的Hook方法, 以响应编译.
	 * <p>此实现为空.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * 这个操作是"编译"的吗?
	 * 
	 * @return 此操作是否已编译并可以使用
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * 检查此操作是否已编译; 如果还没有编译, 延迟地编译它.
	 * <p>由{@code validateParameters}自动调用.
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("JdbcInsert not compiled before execution - invoking compile");
			compile();
		}
	}

	/**
	 * 用于检查此时是否允许更改配置.
	 * 如果已编译该类, 则不允许进一步更改配置.
	 */
	protected void checkIfConfigurationModificationIsAllowed() {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"Configuration cannot be altered once the class has been compiled or used");
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling execution
	//-------------------------------------------------------------------------

	/**
	 * 使用传入的参数Map执行插入的委托方法.
	 * 
	 * @param args 要在insert中使用的参数名称和值的Map
	 * 
	 * @return 受影响的行数
	 */
	protected int doExecute(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertInternal(values);
	}

	/**
	 * 使用传入的{@link SqlParameterSource}执行插入的委托方法.
	 * 
	 * @param parameterSource 要在insert中使用的参数名称和值
	 * 
	 * @return 受影响的行数
	 */
	protected int doExecute(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertInternal(values);
	}

	/**
	 * 执行插入的委托方法.
	 */
	private int executeInsertInternal(List<?> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for insert " + getInsertString() + " with: " + values);
		}
		return getJdbcTemplate().update(getInsertString(), values.toArray(), getInsertTypes());
	}

	/**
	 * 使用传入的参数Map执行插入, 并返回生成的键.
	 * 
	 * @param args 要在insert中使用的参数名称和值的Map
	 * 
	 * @return 插入生成的键
	 */
	protected Number doExecuteAndReturnKey(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertAndReturnKeyInternal(values);
	}

	/**
	 * 使用传入的{@link SqlParameterSource}执行插入, 并返回生成的键.
	 * 
	 * @param parameterSource 要在insert中使用的参数名称和值
	 * 
	 * @return 插入生成的键
	 */
	protected Number doExecuteAndReturnKey(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertAndReturnKeyInternal(values);
	}

	/**
	 * 使用传入的参数Map执行插入, 并返回所有生成的键.
	 * 
	 * @param args 要在insert中使用的参数名称和值的Map
	 * 
	 * @return 包含插入生成的键的KeyHolder
	 */
	protected KeyHolder doExecuteAndReturnKeyHolder(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertAndReturnKeyHolderInternal(values);
	}

	/**
	 * 使用传入的{@link SqlParameterSource}执行插入, 并返回所有生成的键.
	 * 
	 * @param parameterSource 要在insert中使用的参数名称和值
	 * 
	 * @return 包含插入生成的键的KeyHolder
	 */
	protected KeyHolder doExecuteAndReturnKeyHolder(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertAndReturnKeyHolderInternal(values);
	}

	/**
	 * 执行insert并生成单个键的委托方法.
	 */
	private Number executeInsertAndReturnKeyInternal(final List<?> values) {
		KeyHolder kh = executeInsertAndReturnKeyHolderInternal(values);
		if (kh != null && kh.getKey() != null) {
			return kh.getKey();
		}
		else {
			throw new DataIntegrityViolationException(
					"Unable to retrieve the generated key for the insert: " + getInsertString());
		}
	}

	/**
	 * 执行插入并生成任意数量的键的委托方法.
	 */
	private KeyHolder executeInsertAndReturnKeyHolderInternal(final List<?> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for call " + getInsertString() + " with: " + values);
		}
		final KeyHolder keyHolder = new GeneratedKeyHolder();

		if (this.tableMetaDataContext.isGetGeneratedKeysSupported()) {
			getJdbcTemplate().update(
					new PreparedStatementCreator() {
						@Override
						public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
							PreparedStatement ps = prepareStatementForGeneratedKeys(con);
							setParameterValues(ps, values, getInsertTypes());
							return ps;
						}
					},
					keyHolder);
		}

		else {
			if (!this.tableMetaDataContext.isGetGeneratedKeysSimulated()) {
				throw new InvalidDataAccessResourceUsageException(
						"The getGeneratedKeys feature is not supported by this database");
			}
			if (getGeneratedKeyNames().length < 1) {
				throw new InvalidDataAccessApiUsageException("Generated Key Name(s) not specified. " +
						"Using the generated keys features requires specifying the name(s) of the generated column(s)");
			}
			if (getGeneratedKeyNames().length > 1) {
				throw new InvalidDataAccessApiUsageException(
						"Current database only supports retrieving the key for a single column. There are " +
						getGeneratedKeyNames().length  + " columns specified: " + Arrays.asList(getGeneratedKeyNames()));
			}

			final String keyQuery = this.tableMetaDataContext.getSimpleQueryForGetGeneratedKey(
					this.tableMetaDataContext.getTableName(), getGeneratedKeyNames()[0]);
			Assert.notNull(keyQuery, "Query for simulating get generated keys can't be null");

			// 用于从不支持获取生成的键的数据库获取生成的键.
			// HSQL是一个, PostgreSQL是另一个. Postgres 使用RETURNING子句, 而HSQL使用必须使用相同连接执行的第二个查询.

			if (keyQuery.toUpperCase().startsWith("RETURNING")) {
				Long key = getJdbcTemplate().queryForObject(
						getInsertString() + " " + keyQuery, values.toArray(), Long.class);
				Map<String, Object> keys = new HashMap<String, Object>(1);
				keys.put(getGeneratedKeyNames()[0], key);
				keyHolder.getKeyList().add(keys);
			}
			else {
				getJdbcTemplate().execute(new ConnectionCallback<Object>() {
					@Override
					public Object doInConnection(Connection con) throws SQLException, DataAccessException {
						// Do the insert
						PreparedStatement ps = null;
						try {
							ps = con.prepareStatement(getInsertString());
							setParameterValues(ps, values, getInsertTypes());
							ps.executeUpdate();
						}
						finally {
							JdbcUtils.closeStatement(ps);
						}
						//Get the key
						Statement keyStmt = null;
						ResultSet rs = null;
						Map<String, Object> keys = new HashMap<String, Object>(1);
						try {
							keyStmt = con.createStatement();
							rs = keyStmt.executeQuery(keyQuery);
							if (rs.next()) {
								long key = rs.getLong(1);
								keys.put(getGeneratedKeyNames()[0], key);
								keyHolder.getKeyList().add(keys);
							}
						}
						finally {
							JdbcUtils.closeResultSet(rs);
							JdbcUtils.closeStatement(keyStmt);
						}
						return null;
					}
				});
			}
		}

		return keyHolder;
	}

	/**
	 * 创建一个PreparedStatement, 用于使用生成的键进行插入操作.
	 * 
	 * @param con 要使用的连接
	 * 
	 * @return the PreparedStatement
	 */
	private PreparedStatement prepareStatementForGeneratedKeys(Connection con) throws SQLException {
		if (getGeneratedKeyNames().length < 1) {
			throw new InvalidDataAccessApiUsageException("Generated Key Name(s) not specified. " +
					"Using the generated keys features requires specifying the name(s) of the generated column(s).");
		}
		PreparedStatement ps;
		if (this.tableMetaDataContext.isGeneratedKeysColumnNameArraySupported()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using generated keys support with array of column names.");
			}
			ps = con.prepareStatement(getInsertString(), getGeneratedKeyNames());
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Using generated keys support with Statement.RETURN_GENERATED_KEYS.");
			}
			ps = con.prepareStatement(getInsertString(), Statement.RETURN_GENERATED_KEYS);
		}
		return ps;
	}

	/**
	 * 使用传入的参数Map执行批量插入的委托方法.
	 * 
	 * @param batch 要在批量插入中使用的参数名称和值的Map数组
	 * 
	 * @return 受影响的行数数组
	 */
	@SuppressWarnings("unchecked")
	protected int[] doExecuteBatch(Map<String, ?>... batch) {
		checkCompiled();
		List<List<Object>> batchValues = new ArrayList<List<Object>>(batch.length);
		for (Map<String, ?> args : batch) {
			batchValues.add(matchInParameterValuesWithInsertColumns(args));
		}
		return executeBatchInternal(batchValues);
	}

	/**
	 * 使用传入的{@link SqlParameterSource}执行批量插入的委托方法.
	 * 
	 * @param batch 要在insert中使用的参数名称和值的SqlParameterSource数组
	 * 
	 * @return 受影响的行数数组
	 */
	protected int[] doExecuteBatch(SqlParameterSource... batch) {
		checkCompiled();
		List<List<Object>> batchValues = new ArrayList<List<Object>>(batch.length);
		for (SqlParameterSource parameterSource : batch) {
			batchValues.add(matchInParameterValuesWithInsertColumns(parameterSource));
		}
		return executeBatchInternal(batchValues);
	}

	/**
	 * 执行批量插入的委托方法.
	 */
	private int[] executeBatchInternal(final List<List<Object>> batchValues) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing statement " + getInsertString() + " with batch of size: " + batchValues.size());
		}
		return getJdbcTemplate().batchUpdate(getInsertString(),
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						setParameterValues(ps, batchValues.get(i), getInsertTypes());
					}
					@Override
					public int getBatchSize() {
						return batchValues.size();
					}
				});
	}

	/**
	 * 用于设置参数值的内部实现
	 * 
	 * @param preparedStatement the PreparedStatement
	 * @param values 要设置的值
	 */
	private void setParameterValues(PreparedStatement preparedStatement, List<?> values, int... columnTypes)
			throws SQLException {

		int colIndex = 0;
		for (Object value : values) {
			colIndex++;
			if (columnTypes == null || colIndex > columnTypes.length) {
				StatementCreatorUtils.setParameterValue(preparedStatement, colIndex, SqlTypeValue.TYPE_UNKNOWN, value);
			}
			else {
				StatementCreatorUtils.setParameterValue(preparedStatement, colIndex, columnTypes[colIndex - 1], value);
			}
		}
	}

	/**
	 * 将提供的参数值与已注册的参数和通过元数据处理定义的参数进行匹配.
	 * 
	 * @param parameterSource 提供的参数值
	 * 
	 * @return 参数名称和值的Map
	 */
	protected List<Object> matchInParameterValuesWithInsertColumns(SqlParameterSource parameterSource) {
		return this.tableMetaDataContext.matchInParameterValuesWithInsertColumns(parameterSource);
	}

	/**
	 * 将提供的参数值与已注册的参数和通过元数据处理定义的参数进行匹配.
	 * 
	 * @param args 提供的参数值
	 * 
	 * @return 参数名称和值的Map
	 */
	protected List<Object> matchInParameterValuesWithInsertColumns(Map<String, ?> args) {
		return this.tableMetaDataContext.matchInParameterValuesWithInsertColumns(args);
	}
}
