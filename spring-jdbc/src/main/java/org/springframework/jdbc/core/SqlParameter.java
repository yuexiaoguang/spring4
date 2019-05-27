package org.springframework.jdbc.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * 用于表示SQL参数定义的对象.
 *
 * <p>参数可以是匿名的, 在这种情况下, "name"是{@code null}.
 * 但是, 所有参数都必须根据{@link java.sql.Types}定义SQL类型.
 */
public class SqlParameter {

	// 参数的名称
	private String name;

	// 来自{@code java.sql.Types}的SQL类型常量
	private final int sqlType;

	// 用于用户命名的类型, 例如: STRUCT, DISTINCT, JAVA_OBJECT, 命名数组类型
	private String typeName;

	// 在NUMERIC或DECIMAL类型的情况下, 应用的小数
	private Integer scale;


	/**
	 * 创建一个新的匿名SqlParameter, 提供SQL类型.
	 * 
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 */
	public SqlParameter(int sqlType) {
		this.sqlType = sqlType;
	}

	/**
	 * 创建一个新的匿名SqlParameter, 提供SQL类型.
	 * 
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 * @param typeName 参数的类型名称 (可选)
	 */
	public SqlParameter(int sqlType, String typeName) {
		this.sqlType = sqlType;
		this.typeName = typeName;
	}

	/**
	 * 创建一个新的匿名SqlParameter, 提供SQL类型.
	 * 
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 * @param scale 小数点后的位数 (对于DECIMAL和NUMERIC类型)
	 */
	public SqlParameter(int sqlType, int scale) {
		this.sqlType = sqlType;
		this.scale = scale;
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 */
	public SqlParameter(String name, int sqlType) {
		this.name = name;
		this.sqlType = sqlType;
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 * @param typeName 参数的类型名称 (可选)
	 */
	public SqlParameter(String name, int sqlType, String typeName) {
		this.name = name;
		this.sqlType = sqlType;
		this.typeName = typeName;
	}

	/**
	 * @param name 输入和输出映射中使用的参数名称
	 * @param sqlType 参数的SQL类型, 根据{@code java.sql.Types}
	 * @param scale 小数点后的位数 (对于DECIMAL和NUMERIC类型)
	 */
	public SqlParameter(String name, int sqlType, int scale) {
		this.name = name;
		this.sqlType = sqlType;
		this.scale = scale;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param otherParam 要从中复制的SqlParameter对象
	 */
	public SqlParameter(SqlParameter otherParam) {
		Assert.notNull(otherParam, "SqlParameter object must not be null");
		this.name = otherParam.name;
		this.sqlType = otherParam.sqlType;
		this.typeName = otherParam.typeName;
		this.scale = otherParam.scale;
	}


	/**
	 * 返回参数的名称, 或{@code null}.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回参数的SQL类型.
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * 返回参数的类型名称.
	 */
	public String getTypeName() {
		return this.typeName;
	}

	/**
	 * 返回参数的小数.
	 */
	public Integer getScale() {
		return this.scale;
	}


	/**
	 * 返回此参数是否包含应在执行前设置的输入值, 即使它们是{@code null}.
	 * <p>此实现总是返回{@code true}.
	 */
	public boolean isInputValueProvided() {
		return true;
	}

	/**
	 * 返回此参数是否是在{@code CallableStatement.getMoreResults/getUpdateCount}的结果处理期间使用的隐式返回参数.
	 * <p>此实现总是返回 {@code false}.
	 */
	public boolean isResultsParameter() {
		return false;
	}


	/**
	 * 将{@code java.sql.Types}中定义的JDBC类型列表, 转换为此包中使用的SqlParameter对象列表.
	 */
	public static List<SqlParameter> sqlTypesToAnonymousParameterList(int... types) {
		List<SqlParameter> result;
		if (types != null) {
			result = new ArrayList<SqlParameter>(types.length);
			for (int type : types) {
				result.add(new SqlParameter(type));
			}
		}
		else {
			result = new LinkedList<SqlParameter>();
		}
		return result;
	}
}
