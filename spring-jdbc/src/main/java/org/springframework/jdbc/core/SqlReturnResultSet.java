package org.springframework.jdbc.core;

/**
 * 表示从存储过程调用返回的{@link java.sql.ResultSet}.
 *
 * <p>必须提供{@link ResultSetExtractor}, {@link RowCallbackHandler}或 {@link RowMapper}来处理任何返回的行.
 *
 * <p>返回的{@link java.sql.ResultSet ResultSets} - 与所有存储过程参数一样 - 必须具有名称.
 */
public class SqlReturnResultSet extends ResultSetSupportingSqlParameter {

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param extractor 用于解析{@link java.sql.ResultSet}的ResultSetExtractor
	 */
	public SqlReturnResultSet(String name, ResultSetExtractor<?> extractor) {
		super(name, 0, extractor);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param handler 用于解析{@link java.sql.ResultSet}的RowCallbackHandler
	 */
	public SqlReturnResultSet(String name, RowCallbackHandler handler) {
		super(name, 0, handler);
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param mapper 用于解析{@link java.sql.ResultSet}的RowMapper
	 */
	public SqlReturnResultSet(String name, RowMapper<?> mapper) {
		super(name, 0, mapper);
	}


	/**
	 * 此实现总是返回{@code true}.
	 */
	@Override
	public boolean isResultsParameter() {
		return true;
	}
}
