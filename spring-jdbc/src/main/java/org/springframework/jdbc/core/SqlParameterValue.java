package org.springframework.jdbc.core;

/**
 * 用于表示SQL参数值的对象, 包括参数元数据, 如SQL类型和数值的小数.
 *
 * <p>设计用于{@link JdbcTemplate}的操作, 该操作采用一组参数值:
 * 每个这样的参数值可以是{@code SqlParameterValue}, 指示SQL类型 (以及可选的小数), 而不是让模板猜测默认类型.
 * 请注意, 这仅适用于具有'plain'参数数组的操作, 而不适用于具有显式类型数组的重载变体.
 */
public class SqlParameterValue extends SqlParameter {

	private final Object value;


	/**
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 * @param value 值对象
	 */
	public SqlParameterValue(int sqlType, Object value) {
		super(sqlType);
		this.value = value;
	}

	/**
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 * @param typeName 参数的类型名称 (可选)
	 * @param value 值对象
	 */
	public SqlParameterValue(int sqlType, String typeName, Object value) {
		super(sqlType, typeName);
		this.value = value;
	}

	/**
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 * @param scale 小数点后的位数 (对于DECIMAL和NUMERIC类型)
	 * @param value 值对象
	 */
	public SqlParameterValue(int sqlType, int scale, Object value) {
		super(sqlType, scale);
		this.value = value;
	}

	/**
	 * @param declaredParam 为其定义一个值的声明的SqlParameter
	 * @param value 值对象
	 */
	public SqlParameterValue(SqlParameter declaredParam, Object value) {
		super(declaredParam);
		this.value = value;
	}


	/**
	 * 返回此参数值包含的值对象.
	 */
	public Object getValue() {
		return this.value;
	}
}
