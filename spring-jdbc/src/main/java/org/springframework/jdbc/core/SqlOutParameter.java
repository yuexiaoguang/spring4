package org.springframework.jdbc.core;

/**
 * {@link SqlParameter}的子类表示输出参数.
 * 没有其他属性: instanceof 将用于检查此类型.
 *
 * <p>输出参数 - 与所有存储过程参数一样 - 必须具有名称.
 */
public class SqlOutParameter extends ResultSetSupportingSqlParameter {

	private SqlReturnType sqlReturnType;


	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 */
	public SqlOutParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param scale 小数点后的位数 (对于DECIMAL和NUMERIC类型)
	 */
	public SqlOutParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param typeName 参数的类型名称 (可选)
	 */
	public SqlOutParameter(String name, int sqlType, String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param typeName 参数的类型名称 (可选)
	 * @param sqlReturnType 复杂类型的自定义值处理器 (可选)
	 */
	public SqlOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
		super(name, sqlType, typeName);
		this.sqlReturnType = sqlReturnType;
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param rse 用于解析ResultSet的ResultSetExtractor
	 */
	public SqlOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType, rse);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param rch 用于解析ResultSet的RowCallbackHandler
	 */
	public SqlOutParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType, rch);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param rm 用于解析ResultSet的RowMapper
	 */
	public SqlOutParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType, rm);
	}


	/**
	 * 返回自定义返回类型.
	 */
	public SqlReturnType getSqlReturnType() {
		return this.sqlReturnType;
	}

	/**
	 * 返回此参数是否包含自定义返回类型.
	 */
	public boolean isReturnTypeSupported() {
		return (this.sqlReturnType != null);
	}
}
