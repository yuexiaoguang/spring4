package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;

/**
 * 可重用的RDBMS查询, 其具体的子类必须实现抽象的updateRow(ResultSet, int, context)方法
 * 来更新JDBC ResultSet的每一行, 并可选地将内容映射到对象中.
 *
 * <p>可以构造子类, 提供SQL, 参数类型和DataSource. SQL通常会在子类之间变化.
 */
public abstract class UpdatableSqlQuery<T> extends SqlQuery<T> {

	public UpdatableSqlQuery() {
		setUpdatableResults(true);
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要运行的SQL
	 */
	public UpdatableSqlQuery(DataSource ds, String sql) {
		super(ds, sql);
		setUpdatableResults(true);
	}


	/**
	 * 超类模板方法的实现.
	 * 这将调用子类的{@code updateRow()}方法的实现.
	 */
	@Override
	protected RowMapper<T> newRowMapper(Object[] parameters, Map<?, ?> context) {
		return new RowMapperImpl(context);
	}

	/**
	 * 子类必须实现此方法以更新ResultSet的每一行, 并可选地创建结果类型的对象.
	 * 
	 * @param rs 正在完成的ResultSet
	 * @param rowNum 行号(从0开始)
	 * @param context 传递给execute()方法.
	 * 如果不需要上下文信息, 它可以是{@code null}.
	 * 如果需要为每一行传入数据, 可以传入一个HashMap, 该行的主键是HashMap的键.
	 * 这样, 很容易找到每行的更新
	 * 
	 * @return 结果类型的对象
	 * @throws SQLException 如果更新数据有错误.
	 * 子类根本无法捕获SQLExceptions, 依赖于框架来清理.
	 */
	protected abstract T updateRow(ResultSet rs, int rowNum, Map<?, ?> context) throws SQLException;


	/**
	 * RowMapper的实现, 为每一行调用封闭类的{@code updateRow()}方法.
	 */
	protected class RowMapperImpl implements RowMapper<T> {

		private final Map<?, ?> context;

		public RowMapperImpl(Map<?, ?> context) {
			this.context = context;
		}

		@Override
		public T mapRow(ResultSet rs, int rowNum) throws SQLException {
			T result = updateRow(rs, rowNum, this.context);
			rs.updateRow();
			return result;
		}
	}
}
