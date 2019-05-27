package org.springframework.jdbc.core.namedparam;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * {@link SqlParameterSource}实现的抽象基类.
 * 提供每个参数的SQL类型注册.
 */
public abstract class AbstractSqlParameterSource implements SqlParameterSource {

	private final Map<String, Integer> sqlTypes = new HashMap<String, Integer>();

	private final Map<String, String> typeNames = new HashMap<String, String>();


	/**
	 * 为给定参数注册SQL类型.
	 * 
	 * @param paramName 参数的名称
	 * @param sqlType 参数的SQL类型
	 */
	public void registerSqlType(String paramName, int sqlType) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.sqlTypes.put(paramName, sqlType);
	}

	/**
	 * 为给定参数注册SQL类型.
	 * 
	 * @param paramName 参数的名称
	 * @param typeName 参数的类型名称
	 */
	public void registerTypeName(String paramName, String typeName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.typeNames.put(paramName, typeName);
	}

	/**
	 * 如果已注册, 则返回给定参数的SQL类型.
	 * 
	 * @param paramName 参数的名称
	 * 
	 * @return 参数的SQL类型, 或{@code TYPE_UNKNOWN} 如果未注册
	 */
	@Override
	public int getSqlType(String paramName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		Integer sqlType = this.sqlTypes.get(paramName);
		if (sqlType != null) {
			return sqlType;
		}
		return TYPE_UNKNOWN;
	}

	/**
	 * 如果已注册, 则返回给定参数的类型名称.
	 * 
	 * @param paramName 参数的名称
	 * 
	 * @return 参数的类型名称, 或{@code null}如果未注册
	 */
	@Override
	public String getTypeName(String paramName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		return this.typeNames.get(paramName);
	}
}
