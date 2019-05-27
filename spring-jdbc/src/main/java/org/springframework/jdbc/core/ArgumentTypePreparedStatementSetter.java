package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * {@link PreparedStatementSetter}的简单适配器, 它适用于给定的参数数组和JDBC参数类型.
 */
public class ArgumentTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

	private final Object[] args;

	private final int[] argTypes;


	/**
	 * @param args 要设置的参数
	 * @param argTypes 参数的相应SQL类型
	 */
	public ArgumentTypePreparedStatementSetter(Object[] args, int[] argTypes) {
		if ((args != null && argTypes == null) || (args == null && argTypes != null) ||
				(args != null && args.length != argTypes.length)) {
			throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
		}
		this.args = args;
		this.argTypes = argTypes;
	}


	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		int parameterPosition = 1;
		if (this.args != null) {
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				if (arg instanceof Collection && this.argTypes[i] != Types.ARRAY) {
					Collection<?> entries = (Collection<?>) arg;
					for (Object entry : entries) {
						if (entry instanceof Object[]) {
							Object[] valueArray = ((Object[]) entry);
							for (Object argValue : valueArray) {
								doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
								parameterPosition++;
							}
						}
						else {
							doSetValue(ps, parameterPosition, this.argTypes[i], entry);
							parameterPosition++;
						}
					}
				}
				else {
					doSetValue(ps, parameterPosition, this.argTypes[i], arg);
					parameterPosition++;
				}
			}
		}
	}

	/**
	 * 使用传入的值和类型, 设置预准备语句的指定参数位置的值.
	 * 可以通过子类覆盖此方法.
	 * 
	 * @param ps the PreparedStatement
	 * @param parameterPosition 参数位置的索引
	 * @param argType 参数类型
	 * @param argValue 参数值
	 * 
	 * @throws SQLException 如果由PreparedStatement方法抛出
	 */
	protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, Object argValue)
			throws SQLException {

		StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
	}

	@Override
	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}
