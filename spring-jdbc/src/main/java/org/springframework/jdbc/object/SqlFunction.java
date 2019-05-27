package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.core.SingleColumnRowMapper;

/**
 * SQL "function"包装器, 用于返回单行结果的查询.
 * 默认行为是返回一个int, 但是可以通过使用带有额外返回类型参数的构造函数来覆盖它.
 *
 * <p>旨在使用诸如"select user()" 或 "select sysdate from dual"之类的查询来调用返回单个结果的SQL函数.
 * 它不用于调用更复杂的存储函数, 或用于使用CallableStatement调用存储过程或存储函数.
 * 使用StoredProcedure或SqlCall进行此类处理.
 *
 * <p>这是一个具体的类, 通常不需要子类.
 * 使用此包的代码可以创建此类型的对象, 声明SQL和参数, 然后重复调用相应的{@code run}方法来执行该函数.
 * 子类只应为特定参数和返回类型添加专门的{@code run}方法.
 *
 * <p>与所有RdbmsOperation对象一样, SqlFunction对象是线程安全的.
 */
public class SqlFunction<T> extends MappingSqlQuery<T> {

	private final SingleColumnRowMapper<T> rowMapper = new SingleColumnRowMapper<T>();


	/**
	 * 允许用作JavaBean的构造方法.
	 * 在调用{@code compile}方法并使用此对象之前, 必须提供DataSource, SQL和任何参数.
	 */
	public SqlFunction() {
		setRowsExpected(1);
	}

	/**
	 * 使用SQL创建一个新的SqlFunction对象, 但没有参数.
	 * 必须添加参数或无结果.
	 * 
	 * @param ds 从中获取连接的DataSource
	 * @param sql 要执行的SQL
	 */
	public SqlFunction(DataSource ds, String sql) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
	}

	/**
	 * @param ds 从中获取连接的DataSource
	 * @param sql 要执行的SQL
	 * @param types 参数的SQL类型, 如{@code java.sql.Types}类中所定义
	 */
	public SqlFunction(DataSource ds, String sql, int[] types) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
	}

	/**
	 * @param ds 从中获取连接的DataSource
	 * @param sql 要执行的SQL
	 * @param types 参数的SQL类型, 如{@code java.sql.Types}类中所定义
	 * @param resultType 结果对象需要匹配的类型
	 */
	public SqlFunction(DataSource ds, String sql, int[] types, Class<T> resultType) {
		setRowsExpected(1);
		setDataSource(ds);
		setSql(sql);
		setTypes(types);
		setResultType(resultType);
	}


	/**
	 * 指定结果对象需要匹配的类型.
	 * <p>如果未指定, 则结果值将显示为JDBC驱动程序返回的值.
	 */
	public void setResultType(Class<T> resultType) {
		this.rowMapper.setRequiredType(resultType);
	}


	/**
	 * 此方法的此实现从函数返回的单个行中提取单个值.
	 * 如果返回的行数不同, 则将其视为错误.
	 */
	@Override
	protected T mapRow(ResultSet rs, int rowNum) throws SQLException {
		return this.rowMapper.mapRow(rs, rowNum);
	}


	/**
	 * 运行没有参数的函数.
	 * 
	 * @return 函数的值
	 */
	public int run() {
		return run(new Object[0]);
	}

	/**
	 * 使用单个int参数运行函数.
	 * 
	 * @param parameter 单个int参数
	 * 
	 * @return 函数的值
	 */
	public int run(int parameter) {
		return run(new Object[] {parameter});
	}

	/**
	 * 类似于SqlQuery.execute([])方法. 这是执行查询的通用方法, 采用了许多参数.
	 * 
	 * @param parameters 参数数组. 这些将是基础类型的对象或对象包装类型.
	 * 
	 * @return 函数的值
	 */
	public int run(Object... parameters) {
		Object obj = super.findObject(parameters);
		if (!(obj instanceof Number)) {
			throw new TypeMismatchDataAccessException("Couldn't convert result object [" + obj + "] to int");
		}
		return ((Number) obj).intValue();
	}

	/**
	 * 运行没有参数的函数, 将值作为对象返回.
	 * 
	 * @return 函数的值
	 */
	public Object runGeneric() {
		return findObject((Object[]) null);
	}

	/**
	 * 使用单个int参数运行函数.
	 * 
	 * @param parameter 单个int参数
	 * 
	 * @return 函数的值
	 */
	public Object runGeneric(int parameter) {
		return findObject(parameter);
	}

	/**
	 * 类似于{@code SqlQuery.findObject(Object[])}方法.
	 * 这是执行查询的通用方法, 采用了许多参数.
	 * 
	 * @param parameters 参数数组. 这些将是基础类型的对象或对象包装类型.
	 * 
	 * @return 函数的值
	 */
	public Object runGeneric(Object[] parameters) {
		return findObject(parameters);
	}
}
