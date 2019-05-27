package org.springframework.jdbc.core.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * 用于管理用于在数据库表上配置和执行操作的上下文元数据的类.
 */
public class TableMetaDataContext {

	// Logger available to subclasses
	protected final Log logger = LogFactory.getLog(getClass());

	// 此上下文的表名
	private String tableName;

	// 此上下文的catalog名称
	private String catalogName;

	// 此上下文的schema名称
	private String schemaName;

	// 要在此上下文中使用的列对象
	private List<String> tableColumns = new ArrayList<String>();

	// 是否应该访问insert参数元数据信息
	private boolean accessTableColumnMetaData = true;

	// 是否应该覆盖默认值以包含元数据查找的同义词
	private boolean overrideIncludeSynonymsDefault = false;

	// 表元数据的提供者
	private TableMetaDataProvider metaDataProvider;

	// 是否使用生成的键列
	private boolean generatedKeyColumnsUsed = false;

	// 用于检索本机连接的NativeJdbcExtractor
	NativeJdbcExtractor nativeJdbcExtractor;


	/**
	 * 设置此上下文的表名称.
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * 获取此上下文的表名称.
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * 设置此上下文的catalog名称.
	 */
	public void setCatalogName(String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * 获取此上下文的catalog名称.
	 */
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * 设置此上下文的schema名称.
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * 获取此上下文的schema名称.
	 */
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 * 指定是否应该访问列的元数据.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.accessTableColumnMetaData = accessTableColumnMetaData;
	}

	/**
	 * 是否正在访问表的元数据?
	 */
	public boolean isAccessTableColumnMetaData() {
		return this.accessTableColumnMetaData;
	}


	/**
	 * 指定是否应覆盖默认值以访问同义词.
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.overrideIncludeSynonymsDefault = override;
	}

	/**
	 * 是否重写包含同义词默认值?
	 */
	public boolean isOverrideIncludeSynonymsDefault() {
		return this.overrideIncludeSynonymsDefault;
	}

	/**
	 * 获取表的列名称.
	 */
	public List<String> getTableColumns() {
		return this.tableColumns;
	}

	/**
	 * 设置用于检索本机连接的{@link NativeJdbcExtractor}.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}


	/**
	 * 使用提供的配置选项处理当前元数据.
	 * 
	 * @param dataSource 正在使用的DataSource
	 * @param declaredColumns 声明的列
	 * @param generatedKeyNames 生成键的名称
	 */
	public void processMetaData(DataSource dataSource, List<String> declaredColumns, String[] generatedKeyNames) {
		this.metaDataProvider =
				TableMetaDataProviderFactory.createMetaDataProvider(dataSource, this, this.nativeJdbcExtractor);
		this.tableColumns = reconcileColumnsToUse(declaredColumns, generatedKeyNames);
	}

	/**
	 * 将从元数据创建的列与声明的列进行比较, 并返回已协调的列表.
	 * 
	 * @param declaredColumns 声明的列名
	 * @param generatedKeyNames 生成的键列的名称
	 */
	protected List<String> reconcileColumnsToUse(List<String> declaredColumns, String[] generatedKeyNames) {
		if (generatedKeyNames.length > 0) {
			this.generatedKeyColumnsUsed = true;
		}
		if (!declaredColumns.isEmpty()) {
			return new ArrayList<String>(declaredColumns);
		}
		Set<String> keys = new LinkedHashSet<String>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase());
		}
		List<String> columns = new ArrayList<String>();
		for (TableParameterMetaData meta : this.metaDataProvider.getTableParameterMetaData()) {
			if (!keys.contains(meta.getParameterName().toUpperCase())) {
				columns.add(meta.getParameterName());
			}
		}
		return columns;
	}

	/**
	 * 将提供的列名称和值与使用的列列表进行匹配.
	 * 
	 * @param parameterSource 参数名称和值
	 */
	public List<Object> matchInParameterValuesWithInsertColumns(SqlParameterSource parameterSource) {
		List<Object> values = new ArrayList<Object>();
		// 对于参数源查找, 需要提供不区分大小写的查找支持, 因为数据库元数据不一定提供区分大小写的列名
		Map<String, String> caseInsensitiveParameterNames =
				SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);
		for (String column : this.tableColumns) {
			if (parameterSource.hasValue(column)) {
				values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, column));
			}
			else {
				String lowerCaseName = column.toLowerCase();
				if (parameterSource.hasValue(lowerCaseName)) {
					values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
				}
				else {
					String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(column);
					if (parameterSource.hasValue(propertyName)) {
						values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
					}
					else {
						if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
							values.add(SqlParameterSourceUtils.getTypedValue(
									parameterSource, caseInsensitiveParameterNames.get(lowerCaseName)));
						}
						else {
							values.add(null);
						}
					}
				}
			}
		}
		return values;
	}

	/**
	 * 将提供的列名称和值与使用的列列表进行匹配.
	 * 
	 * @param inParameters 参数名称和值
	 */
	public List<Object> matchInParameterValuesWithInsertColumns(Map<String, ?> inParameters) {
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> source = new LinkedHashMap<String, Object>(inParameters.size());
		for (String key : inParameters.keySet()) {
			source.put(key.toLowerCase(), inParameters.get(key));
		}
		for (String column : this.tableColumns) {
			values.add(source.get(column.toLowerCase()));
		}
		return values;
	}


	/**
	 * 根据配置和元数据信息构建插入字符串.
	 * 
	 * @return 要使用的插入字符串
	 */
	public String createInsertString(String... generatedKeyNames) {
		Set<String> keys = new LinkedHashSet<String>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase());
		}
		StringBuilder insertStatement = new StringBuilder();
		insertStatement.append("INSERT INTO ");
		if (getSchemaName() != null) {
			insertStatement.append(getSchemaName());
			insertStatement.append(".");
		}
		insertStatement.append(getTableName());
		insertStatement.append(" (");
		int columnCount = 0;
		for (String columnName : getTableColumns()) {
			if (!keys.contains(columnName.toUpperCase())) {
				columnCount++;
				if (columnCount > 1) {
					insertStatement.append(", ");
				}
				insertStatement.append(columnName);
			}
		}
		insertStatement.append(") VALUES(");
		if (columnCount < 1) {
			if (this.generatedKeyColumnsUsed) {
				if (logger.isInfoEnabled()) {
					logger.info("Unable to locate non-key columns for table '" +
							getTableName() + "' so an empty insert statement is generated");
				}
			}
			else {
				throw new InvalidDataAccessApiUsageException("Unable to locate columns for table '" +
						getTableName() + "' so an insert statement can't be generated");
			}
		}
		for (int i = 0; i < columnCount; i++) {
			if (i > 0) {
				insertStatement.append(", ");
			}
			insertStatement.append("?");
		}
		insertStatement.append(")");
		return insertStatement.toString();
	}

	/**
	 * 根据配置和元数据信息构建{@link java.sql.Types}数组.
	 * 
	 * @return 要使用的类型数组
	 */
	public int[] createInsertTypes() {
		int[] types = new int[getTableColumns().size()];
		List<TableParameterMetaData> parameters = this.metaDataProvider.getTableParameterMetaData();
		Map<String, TableParameterMetaData> parameterMap =
				new LinkedHashMap<String, TableParameterMetaData>(parameters.size());
		for (TableParameterMetaData tpmd : parameters) {
			parameterMap.put(tpmd.getParameterName().toUpperCase(), tpmd);
		}
		int typeIndx = 0;
		for (String column : getTableColumns()) {
			if (column == null) {
				types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
			}
			else {
				TableParameterMetaData tpmd = parameterMap.get(column.toUpperCase());
				if (tpmd != null) {
					types[typeIndx] = tpmd.getSqlType();
				}
				else {
					types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
				}
			}
			typeIndx++;
		}
		return types;
	}


	/**
	 * 此数据库是否支持检索生成的键的JDBC 3.0功能:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSupported() {
		return this.metaDataProvider.isGetGeneratedKeysSupported();
	}

	/**
	 * 当不支持JDBC 3.0功能时, 此数据库是否支持简单查询以检索生成的键:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSimulated() {
		return this.metaDataProvider.isGetGeneratedKeysSimulated();
	}

	/**
	 * 当不支持JDBC 3.0功能时，此数据库是否支持简单查询以检索生成的键:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 * @deprecated as of 4.3.15, in favor of {@link #getSimpleQueryForGetGeneratedKey}
	 */
	@Deprecated
	public String getSimulationQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * 当不支持JDBC 3.0功能时, 此数据库是否支持简单查询以检索生成的键:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return this.metaDataProvider.getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * 是否支持列名称String数组, 用于检索生成的键?
	 * {@link java.sql.Connection#createStruct(String, Object[])}?
	 */
	public boolean isGeneratedKeysColumnNameArraySupported() {
		return this.metaDataProvider.isGeneratedKeysColumnNameArraySupported();
	}

}
