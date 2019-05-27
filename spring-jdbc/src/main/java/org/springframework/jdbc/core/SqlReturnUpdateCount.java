package org.springframework.jdbc.core;

import java.sql.Types;

/**
 * 表示存储过程调用返回的更新计数.
 *
 * <p>返回的更新计数 - 与所有存储过程参数一样 - <b>必须</b>具有名称.
 */
public class SqlReturnUpdateCount extends SqlParameter {

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 */
	public SqlReturnUpdateCount(String name) {
		super(name, Types.INTEGER);
	}


	/**
	 * 此实现总是返回{@code false}.
	 */
	@Override
	public boolean isInputValueProvided() {
		return false;
	}

	/**
	 * 此实现总是返回{@code true}.
	 */
	@Override
	public boolean isResultsParameter() {
		return true;
	}

}
