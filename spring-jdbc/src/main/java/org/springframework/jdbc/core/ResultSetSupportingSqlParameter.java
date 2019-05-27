package org.springframework.jdbc.core;

/**
 * 支持ResultSet的SqlParameter的公共基类, 如{@link SqlOutParameter}和{@link SqlReturnResultSet}.
 */
public class ResultSetSupportingSqlParameter extends SqlParameter {

	private ResultSetExtractor<?> resultSetExtractor;

	private RowCallbackHandler rowCallbackHandler;

	private RowMapper<?> rowMapper;


	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 根据java.sql.Types的参数的SQL类型
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 根据java.sql.Types的参数的SQL类型
	 * @param scale 小数点后的位数 (对于DECIMAL和NUMERIC类型)
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 根据java.sql.Types的参数的SQL类型
	 * @param typeName 参数的类型名称 (可选)
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 根据java.sql.Types的参数的SQL类型
	 * @param rse 用于解析ResultSet的ResultSetExtractor
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType);
		this.resultSetExtractor = rse;
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 根据java.sql.Types的参数的SQL类型
	 * @param rch 用于解析ResultSet的RowCallbackHandler
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType);
		this.rowCallbackHandler = rch;
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 根据java.sql.Types的参数的SQL类型
	 * @param rm 用于解析ResultSet的RowMapper
	 */
	public ResultSetSupportingSqlParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType);
		this.rowMapper = rm;
	}


	/**
	 * 此参数是否支持 ResultSet, i.e. 它是否包含 ResultSetExtractor, RowCallbackHandler 或RowMapper?
	 */
	public boolean isResultSetSupported() {
		return (this.resultSetExtractor != null || this.rowCallbackHandler != null || this.rowMapper != null);
	}

	/**
	 * 返回此参数持有的ResultSetExtractor.
	 */
	public ResultSetExtractor<?> getResultSetExtractor() {
		return this.resultSetExtractor;
	}

	/**
	 * 返回此参数持有的RowCallbackHandler.
	 */
	public RowCallbackHandler getRowCallbackHandler() {
		return this.rowCallbackHandler;
	}

	/**
	 * 返回此参数持有的RowMapper.
	 */
	public RowMapper<?> getRowMapper() {
		return this.rowMapper;
	}


	/**
	 * 此实现总是返回{@code false}.
	 */
	@Override
	public boolean isInputValueProvided() {
		return false;
	}
}
