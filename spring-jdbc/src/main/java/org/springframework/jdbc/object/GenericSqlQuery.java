package org.springframework.jdbc.object;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * {@link SqlQuery}的具体变体, 可以使用{@link RowMapper}进行配置.
 */
public class GenericSqlQuery<T> extends SqlQuery<T> {

	private RowMapper<T> rowMapper;

	@SuppressWarnings("rawtypes")
	private Class<? extends RowMapper> rowMapperClass;


	/**
	 * 设置用于此查询的一个特定的{@link RowMapper}实例.
	 */
	public void setRowMapper(RowMapper<T> rowMapper) {
		this.rowMapper = rowMapper;
	}

	/**
	 * 为此查询设置{@link RowMapper}类, 每次执行创建一个新的{@link RowMapper}实例.
	 */
	@SuppressWarnings("rawtypes")
	public void setRowMapperClass(Class<? extends RowMapper> rowMapperClass) {
		this.rowMapperClass = rowMapperClass;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.isTrue(this.rowMapper != null || this.rowMapperClass != null,
				"'rowMapper' or 'rowMapperClass' is required");
	}


	@Override
	@SuppressWarnings("unchecked")
	protected RowMapper<T> newRowMapper(Object[] parameters, Map<?, ?> context) {
		return (this.rowMapper != null ? this.rowMapper : BeanUtils.instantiateClass(this.rowMapperClass));
	}

}
