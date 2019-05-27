package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

/**
 * 可重用查询, 其中具体子类必须实现抽象 mapRow(ResultSet, int)方法, 以将JDBC ResultSet的每一行转换为对象.
 *
 * <p>通过删除参数和上下文来简化MappingSqlQueryWithParameters API.
 * 大多数子类都不关心参数. 如果不使用上下文信息, 子类化这个, 而不是MappingSqlQueryWithParameter.
 */
public abstract class MappingSqlQuery<T> extends MappingSqlQueryWithParameters<T> {

	/**
	 * 允许用作JavaBean的构造方法.
	 */
	public MappingSqlQuery() {
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要运行的SQL
	 */
	public MappingSqlQuery(DataSource ds, String sql) {
		super(ds, sql);
	}


	/**
	 * 实现此方法是为了调用更简单的mapRow模板方法, 忽略参数.
	 */
	@Override
	protected final T mapRow(ResultSet rs, int rowNum, Object[] parameters, Map<?, ?> context)
			throws SQLException {

		return mapRow(rs, rowNum);
	}

	/**
	 * 子类必须实现此方法, 以将ResultSet的每一行转换为结果类型的对象.
	 * <p>与MappingSqlQueryWithParameters的直接子类相反, 此类的子类不需要关注查询对象的execute方法的参数.
	 * 
	 * @param rs 正在完成的ResultSet
	 * @param rowNum 行号 (从0开始)
	 * 
	 * @return 结果类型的对象
	 * @throws SQLException 如果提取数据有错误.
	 * 子类根本无法捕获SQLExceptions, 依赖于框架来清理.
	 */
	protected abstract T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
