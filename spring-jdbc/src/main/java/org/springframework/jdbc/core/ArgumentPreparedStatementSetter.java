package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link PreparedStatementSetter}的简单适配器, 它适用于给定的参数数组.
 */
public class ArgumentPreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	private final Object[] args;


	/**
	 * @param args 要设置的参数
	 */
	public ArgumentPreparedStatementSetter(Object[] args) {
		this.args = args;
	}


	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		if (this.args != null) {
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				doSetValue(ps, i + 1, arg);
			}
		}
	}

	/**
	 * 使用传入的值设置预准备语句指定参数索引的值.
	 * 可以通过子类覆盖此方法.
	 * 
	 * @param ps the PreparedStatement
	 * @param parameterPosition 参数位置的索引
	 * @param argValue 要设置的值
	 * 
	 * @throws SQLException 如果由PreparedStatement方法抛出
	 */
	protected void doSetValue(PreparedStatement ps, int parameterPosition, Object argValue) throws SQLException {
		if (argValue instanceof SqlParameterValue) {
			SqlParameterValue paramValue = (SqlParameterValue) argValue;
			StatementCreatorUtils.setParameterValue(ps, parameterPosition, paramValue, paramValue.getValue());
		}
		else {
			StatementCreatorUtils.setParameterValue(ps, parameterPosition, SqlTypeValue.TYPE_UNKNOWN, argValue);
		}
	}

	@Override
	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}
