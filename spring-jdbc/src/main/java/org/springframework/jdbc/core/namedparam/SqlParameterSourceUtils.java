package org.springframework.jdbc.core.namedparam;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.SqlParameterValue;

/**
 * 为{@link SqlParameterSource}提供工具方法的类, 特别是{@link NamedParameterJdbcTemplate}.
 */
public class SqlParameterSourceUtils {

	/**
	 * 创建一个{@link MapSqlParameterSource}对象数组, 其中填充了传入值的数据.
	 * 这将定义批处理操作中包含的内容.
	 * 
	 * @param valueMaps 包含要使用的值的{@link Map}实例数组
	 * 
	 * @return {@link SqlParameterSource}数组
	 */
	public static SqlParameterSource[] createBatch(Map<String, ?>[] valueMaps) {
		MapSqlParameterSource[] batch = new MapSqlParameterSource[valueMaps.length];
		for (int i = 0; i < valueMaps.length; i++) {
			batch[i] = new MapSqlParameterSource(valueMaps[i]);
		}
		return batch;
	}

	/**
	 * 创建一个{@link BeanPropertySqlParameterSource}对象数组, 其中填充了传入值的数据. 这将定义批处理操作中包含的内容.
	 * 
	 * @param beans 包含要使用的值的bean的对象数组
	 * 
	 * @return {@link SqlParameterSource}数组
	 */
	public static SqlParameterSource[] createBatch(Object[] beans) {
		BeanPropertySqlParameterSource[] batch = new BeanPropertySqlParameterSource[beans.length];
		for (int i = 0; i < beans.length; i++) {
			batch[i] = new BeanPropertySqlParameterSource(beans[i]);
		}
		return batch;
	}

	/**
	 * 如果参数具有类型信息, 则创建包装值, 否则创建普通对象.
	 * 
	 * @param source 参数值和类型信息的源
	 * @param parameterName 参数的名称
	 * 
	 * @return 值对象
	 */
	public static Object getTypedValue(SqlParameterSource source, String parameterName) {
		int sqlType = source.getSqlType(parameterName);
		if (sqlType != SqlParameterSource.TYPE_UNKNOWN) {
			if (source.getTypeName(parameterName) != null) {
				return new SqlParameterValue(sqlType, source.getTypeName(parameterName), source.getValue(parameterName));
			}
			else {
				return new SqlParameterValue(sqlType, source.getValue(parameterName));
			}
		}
		else {
			return source.getValue(parameterName);
		}
	}

	/**
	 * 使用原始名称创建不区分大小写的参数名称的Map.
	 * 
	 * @param parameterSource 参数名称的源
	 * 
	 * @return 可用于参数名称不区分大小写匹配的Map
	 */
	public static Map<String, String> extractCaseInsensitiveParameterNames(SqlParameterSource parameterSource) {
		Map<String, String> caseInsensitiveParameterNames = new HashMap<String, String>();
		if (parameterSource instanceof BeanPropertySqlParameterSource) {
			String[] propertyNames = ((BeanPropertySqlParameterSource) parameterSource).getReadablePropertyNames();
			for (String name : propertyNames) {
				caseInsensitiveParameterNames.put(name.toLowerCase(), name);
			}
		}
		else if (parameterSource instanceof MapSqlParameterSource) {
			for (String name : ((MapSqlParameterSource) parameterSource).getValues().keySet()) {
				caseInsensitiveParameterNames.put(name.toLowerCase(), name);
			}
		}
		return caseInsensitiveParameterNames;
	}
}
