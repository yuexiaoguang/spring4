package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;

/**
 * 持有用于调用处理的特定参数的元数据.
 */
public class CallParameterMetaData {

	private String parameterName;

	private int parameterType;

	private int sqlType;

	private String typeName;

	private boolean nullable;


	/**
	 * 采取所有属性.
	 */
	public CallParameterMetaData(
			String columnName, int columnType, int sqlType, String typeName, boolean nullable) {

		this.parameterName = columnName;
		this.parameterType = columnType;
		this.sqlType = sqlType;
		this.typeName = typeName;
		this.nullable = nullable;
	}


	/**
	 * 获取参数名称.
	 */
	public String getParameterName() {
		return this.parameterName;
	}

	/**
	 * 获取参数类型.
	 */
	public int getParameterType() {
		return this.parameterType;
	}

	/**
	 * 确定声明的参数是否符合我们的'return'参数:
	 * 类型{@link DatabaseMetaData#procedureColumnReturn} 或 {@link DatabaseMetaData#procedureColumnResult}.
	 */
	public boolean isReturnParameter() {
		return (this.parameterType == DatabaseMetaData.procedureColumnReturn ||
				this.parameterType == DatabaseMetaData.procedureColumnResult);
	}

	/**
	 * 获取参数SQL类型.
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * 获取参数类型名称.
	 */
	public String getTypeName() {
		return this.typeName;
	}

	/**
	 * 获取参数是否可为空.
	 */
	public boolean isNullable() {
		return this.nullable;
	}

}
