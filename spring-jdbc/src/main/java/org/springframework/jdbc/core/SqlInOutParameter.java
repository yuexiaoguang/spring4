package org.springframework.jdbc.core;

/**
 * {@link SqlOutParameter}的子类表示INOUT参数.
 * 对于SqlParameter的{@link #isInputValueProvided}测试, 将返回{@code true}, 与标准SqlOutParameter相反.
 *
 * <p>输出参数 - 与所有存储过程参数一样 - 必须具有名称.
 */
public class SqlInOutParameter extends SqlOutParameter {

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 */
	public SqlInOutParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param scale 小数点后的位数 (对于DECIMAL和NUMERIC类型)
	 */
	public SqlInOutParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param typeName 参数的类型名称 (可选)
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param typeName 参数的类型名称 (可选)
	 * @param sqlReturnType 复杂类型的自定义值处理器 (可选)
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
		super(name, sqlType, typeName, sqlReturnType);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param rse 用于解析ResultSet的ResultSetExtractor
	 */
	public SqlInOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType, rse);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param rch 用于解析ResultSet的RowCallbackHandler
	 */
	public SqlInOutParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType, rch);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据java.sql.Types
	 * @param rm 用于解析ResultSet的RowMapper
	 */
	public SqlInOutParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType, rm);
	}


	/**
	 * 此实现总是返回{@code true}.
	 */
	@Override
	public boolean isInputValueProvided() {
		return true;
	}

}
