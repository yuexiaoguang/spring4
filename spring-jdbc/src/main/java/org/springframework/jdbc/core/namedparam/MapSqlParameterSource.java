package org.springframework.jdbc.core.namedparam;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.util.Assert;

/**
 * {@link SqlParameterSource}实现, 它包含给定的参数Map.
 *
 * <p>此类用于将简单的参数值Map传递给{@link NamedParameterJdbcTemplate}类的方法.
 *
 * <p>此类上的{@code addValue}方法将使添加多个值更容易.
 * 这些方法返回对{@link MapSqlParameterSource}本身的引用, 因此可以在一个语句中将多个方法调用链接在一起.
 */
public class MapSqlParameterSource extends AbstractSqlParameterSource {

	private final Map<String, Object> values = new LinkedHashMap<String, Object>();


	/**
	 * 通过{@code addValue}添加值.
	 */
	public MapSqlParameterSource() {
	}

	/**
	 * 创建一个新的MapSqlParameterSource, 其中一个值由提供的参数组成.
	 * 
	 * @param paramName 参数的名称
	 * @param value 参数的值
	 */
	public MapSqlParameterSource(String paramName, Object value) {
		addValue(paramName, value);
	}

	/**
	 * 基于Map创建新的MapSqlParameterSource.
	 * 
	 * @param values 包含现有参数值的Map (can be {@code null})
	 */
	public MapSqlParameterSource(Map<String, ?> values) {
		addValues(values);
	}


	/**
	 * 将参数添加到此参数源.
	 * 
	 * @param paramName 参数的名称
	 * @param value 参数的值
	 * 
	 * @return 对此参数源的引用, 因此可以将多个调用链接在一起
	 */
	public MapSqlParameterSource addValue(String paramName, Object value) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.values.put(paramName, value);
		if (value instanceof SqlParameterValue) {
			registerSqlType(paramName, ((SqlParameterValue) value).getSqlType());
		}
		return this;
	}

	/**
	 * 将参数添加到此参数源.
	 * 
	 * @param paramName 参数的名称
	 * @param value 参数的值
	 * @param sqlType 参数的SQL类型
	 * 
	 * @return 对此参数源的引用, 因此可以将多个调用链接在一起
	 */
	public MapSqlParameterSource addValue(String paramName, Object value, int sqlType) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.values.put(paramName, value);
		registerSqlType(paramName, sqlType);
		return this;
	}

	/**
	 * 将参数添加到此参数源.
	 * 
	 * @param paramName 参数的名称
	 * @param value 参数的值
	 * @param sqlType 参数的SQL类型
	 * @param typeName 参数的类型名称
	 * 
	 * @return 对此参数源的引用, 因此可以将多个调用链接在一起
	 */
	public MapSqlParameterSource addValue(String paramName, Object value, int sqlType, String typeName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.values.put(paramName, value);
		registerSqlType(paramName, sqlType);
		registerTypeName(paramName, typeName);
		return this;
	}

	/**
	 * 将参数Map添加到此参数源.
	 * 
	 * @param values 包含现有参数值的Map (can be {@code null})
	 * 
	 * @return 对此参数源的引用, 因此可以将多个调用链接在一起
	 */
	public MapSqlParameterSource addValues(Map<String, ?> values) {
		if (values != null) {
			for (Map.Entry<String, ?> entry : values.entrySet()) {
				this.values.put(entry.getKey(), entry.getValue());
				if (entry.getValue() instanceof SqlParameterValue) {
					SqlParameterValue value = (SqlParameterValue) entry.getValue();
					registerSqlType(entry.getKey(), value.getSqlType());
				}
			}
		}
		return this;
	}

	/**
	 * 将当前参数值公开为只读Map.
	 */
	public Map<String, Object> getValues() {
		return Collections.unmodifiableMap(this.values);
	}


	@Override
	public boolean hasValue(String paramName) {
		return this.values.containsKey(paramName);
	}

	@Override
	public Object getValue(String paramName) {
		if (!hasValue(paramName)) {
			throw new IllegalArgumentException("No value registered for key '" + paramName + "'");
		}
		return this.values.get(paramName);
	}
}
