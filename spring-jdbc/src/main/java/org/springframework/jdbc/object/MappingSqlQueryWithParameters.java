package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;

/**
 * 可重用的RDBMS查询, 其中具体的子类必须实现抽象的mapRow(ResultSet, int)方法, 以将JDBC ResultSet的每一行映射到一个对象中.
 *
 * <p>这种手动映射通常优于使用反射的"自动"映射, 这在非平凡的情况下可能变得复杂.
 * 例如, 当前类允许不同的对象用于不同的行 (例如, 如果指示了子类).
 * 它允许设置计算的字段. 并且ResultSet列不需要与bean属性具有相同的名称.
 * 行动中的Pareto Principle: 加倍努力使提取过程自动化, 使框架变得更加复杂, 几乎没有真正的好处.
 *
 * <p>可以构造子类, 提供SQL, 参数类型和DataSource. SQL通常会在子类之间变化.
 */
public abstract class MappingSqlQueryWithParameters<T> extends SqlQuery<T> {

	/**
	 * 允许用作JavaBean的构造方法
	 */
	public MappingSqlQueryWithParameters() {
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要运行的SQL
	 */
	public MappingSqlQueryWithParameters(DataSource ds, String sql) {
		super(ds, sql);
	}


	/**
	 * 抽象方法的实现. 这将调用子类的mapRow()方法的实现.
	 */
	@Override
	protected RowMapper<T> newRowMapper(Object[] parameters, Map<?, ?> context) {
		return new RowMapperImpl(parameters, context);
	}

	/**
	 * 子类必须实现此方法, 以将ResultSet的每一行转换为结果类型的对象.
	 * 
	 * @param rs 正在完成的ResultSet
	 * @param rowNum 行号 (从0开始)
	 * @param parameters 参数 (传递给 execute() 方法).
	 * 子类很少对这些感兴趣. 如果没有参数, 它可以是{@code null}.
	 * @param context 传递给 execute() 方法.
	 * 如果不需要上下文信息, 它可以是{@code null}.
	 * 
	 * @return 结果类型的对象
	 * @throws SQLException 如果提取数据有错误.
	 * 子类根本无法捕获SQLExceptions, 依赖于框架来清理.
	 */
	protected abstract T mapRow(ResultSet rs, int rowNum, Object[] parameters, Map<?, ?> context)
			throws SQLException;


	/**
	 * RowMapper的实现, 为每一行调用封闭类的{@code mapRow}方法.
	 */
	protected class RowMapperImpl implements RowMapper<T> {

		private final Object[] params;

		private final Map<?, ?> context;

		/**
		 * 使用数组结果. 如果知道预期会有多少结果, 效率会更高.
		 */
		public RowMapperImpl(Object[] parameters, Map<?, ?> context) {
			this.params = parameters;
			this.context = context;
		}

		@Override
		public T mapRow(ResultSet rs, int rowNum) throws SQLException {
			return MappingSqlQueryWithParameters.this.mapRow(rs, rowNum, this.params, this.context);
		}
	}
}
