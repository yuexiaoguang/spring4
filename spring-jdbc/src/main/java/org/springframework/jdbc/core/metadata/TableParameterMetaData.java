package org.springframework.jdbc.core.metadata;

/**
 * 持有用于表处理的特定参数的元数据.
 */
public class TableParameterMetaData {

	private final String parameterName;

	private final int sqlType;

	private final boolean nullable;


	public TableParameterMetaData(String columnName, int sqlType, boolean nullable) {
		this.parameterName = columnName;
		this.sqlType = sqlType;
		this.nullable = nullable;
	}


	/**
	 * 获取参数名称.
	 */
	public String getParameterName() {
		return this.parameterName;
	}

	/**
	 * 获取参数SQL类型.
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * 获取参数/列是否可为空.
	 */
	public boolean isNullable() {
		return this.nullable;
	}

}
