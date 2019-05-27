package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;

/**
 * {@link RowMapper}实现, 将单个列转换为每行一个结果值.
 * 期望在仅包含单个列的{@code java.sql.ResultSet}上运行.
 *
 * <p>可以指定每行的结果值的类型.
 * 单列的值将从{@code ResultSet}中提取并转换为指定的目标类型.
 */
public class SingleColumnRowMapper<T> implements RowMapper<T> {

	private Class<?> requiredType;


	/**
	 * 为bean风格的配置创建一个新的{@code SingleColumnRowMapper}.
	 */
	public SingleColumnRowMapper() {
	}

	/**
	 * <p>请考虑使用{@link #newInstance}工厂方法, 这样只允许指定一次所需的类型.
	 * 
	 * @param requiredType 每个结果对象应匹配的类型
	 */
	public SingleColumnRowMapper(Class<T> requiredType) {
		setRequiredType(requiredType);
	}


	/**
	 * 设置每个结果对象应匹配的类型.
	 * <p>如果未指定, 则列值将显示为JDBC驱动程序返回的值.
	 */
	public void setRequiredType(Class<T> requiredType) {
		this.requiredType = ClassUtils.resolvePrimitiveIfNecessary(requiredType);
	}


	/**
	 * 提取当前行中单个列的值.
	 * <p>验证只选择了一列, 然后在必要时委托给{@code getColumnValue()}和 {@code convertValueToRequiredType}.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		// 验证列数.
		ResultSetMetaData rsmd = rs.getMetaData();
		int nrOfColumns = rsmd.getColumnCount();
		if (nrOfColumns != 1) {
			throw new IncorrectResultSetColumnCountException(1, nrOfColumns);
		}

		// 从JDBC ResultSet中提取列值.
		Object result = getColumnValue(rs, 1, this.requiredType);
		if (result != null && this.requiredType != null && !this.requiredType.isInstance(result)) {
			// 提取的值已经不匹配: 尝试转换它.
			try {
				return (T) convertValueToRequiredType(result, this.requiredType);
			}
			catch (IllegalArgumentException ex) {
				throw new TypeMismatchDataAccessException(
						"Type mismatch affecting row number " + rowNum + " and column type '" +
						rsmd.getColumnTypeName(1) + "': " + ex.getMessage());
			}
		}
		return (T) result;
	}

	/**
	 * 检索指定列的JDBC对象值.
	 * <p>默认实现调用 {@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}.
	 * 如果未指定任何必需类型, 则此方法委托给{@code getColumnValue(rs, index)},
	 * 它基本上调用{@code ResultSet.getObject(index)}, 但将一些额外的默认转换应用于适当的值类型.
	 * 
	 * @param rs 保存数据的ResultSet
	 * @param index 列索引
	 * @param requiredType 每个结果对象应匹配的类型 (如果没有指定, 则为{@code null})
	 * 
	 * @return Object值
	 * @throws SQLException 在提取失败的情况下
	 */
	protected Object getColumnValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {
		if (requiredType != null) {
			return JdbcUtils.getResultSetValue(rs, index, requiredType);
		}
		else {
			// 未指定所需类型 -> 执行默认提取.
			return getColumnValue(rs, index);
		}
	}

	/**
	 * 使用最合适的值类型检索指定列的JDBC对象值. 如果未指定所需类型, 则调用.
	 * <p>默认实现委托给{@code JdbcUtils.getResultSetValue()}, 它使用{@code ResultSet.getObject(index)}方法.
	 * 此外, 它包含一个"hack"来绕过Oracle为其TIMESTAMP数据类型返回非标准对象.
	 * 有关详细信息, 请参阅{@code JdbcUtils#getResultSetValue()} javadoc.
	 * 
	 * @param rs 保存数据的ResultSet
	 * @param index 列索引
	 * 
	 * @return Object值
	 * @throws SQLException 在提取失败的情况下
	 */
	protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index);
	}

	/**
	 * 将给定列值转换为指定的必需类型.
	 * 仅在提取的列值不匹配时才调用.
	 * <p>如果所需类型是String, 则该值将通过{@code toString()}简单地进行字符串化.
	 * 如果是Number, 该值将转换为Number, 通过数字转换或字符串解析 (取决于值类型).
	 * 
	 * @param value 从{@code getColumnValue()}中提取的列值 (never {@code null})
	 * @param requiredType 每个结果对象应匹配的类型 (never {@code null})
	 * 
	 * @return 已转换的值
	 */
	@SuppressWarnings("unchecked")
	protected Object convertValueToRequiredType(Object value, Class<?> requiredType) {
		if (String.class == requiredType) {
			return value.toString();
		}
		else if (Number.class.isAssignableFrom(requiredType)) {
			if (value instanceof Number) {
				// 将原始 Number转换为目标 Number类.
				return NumberUtils.convertNumberToTargetClass(((Number) value), (Class<Number>) requiredType);
			}
			else {
				// 将字符串化值转换为目标Number类.
				return NumberUtils.parseNumber(value.toString(),(Class<Number>) requiredType);
			}
		}
		else {
			throw new IllegalArgumentException(
					"Value [" + value + "] is of type [" + value.getClass().getName() +
					"] and cannot be converted to required type [" + requiredType.getName() + "]");
		}
	}


	/**
	 * 静态工厂方法创建一个新的{@code SingleColumnRowMapper} (只需指定一次所需的类型).
	 * 
	 * @param requiredType 每个结果对象应匹配的类型
	 */
	public static <T> SingleColumnRowMapper<T> newInstance(Class<T> requiredType) {
		return new SingleColumnRowMapper<T>(requiredType);
	}
}
