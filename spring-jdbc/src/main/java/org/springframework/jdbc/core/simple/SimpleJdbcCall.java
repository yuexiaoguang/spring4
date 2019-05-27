package org.springframework.jdbc.core.simple;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * SimpleJdbcCall是一个多线程, 可重用的对象, 表示对存储过程或存储函数的调用.
 * 它提供元数据处理, 以简化访问基本存储过程/函数所需的代码.
 * 需要提供的只是过程/函数的名称, 以及执行调用时包含参数的Map.
 * 提供的参数的名称将与创建存储过程时声明的in和out参数匹配.
 *
 * <p>元数据处理基于JDBC驱动程序提供的DatabaseMetaData.
 * 由于我们依赖于JDBC驱动程序, 因此这种"自动检测"只能用于已知可提供准确元数据的数据库.
 * 目前包括Derby, MySQL, Microsoft SQL Server, Oracle, DB2, Sybase和 PostgreSQL.
 * 对于任何其他数据库, 需要明确声明所有参数.
 * 当然, 即使数据库提供了必要的元数据, 也可以显式声明所有参数.
 * 在这种情况下, 您声明的参数将优先.
 * 如果要使用与存储过程编译期间声明的参数名称不匹配的参数名称, 也可以关闭任何元数据处理.
 *
 * <p>使用Spring的{@link JdbcTemplate}处理实际插入.
 *
 * <p>许多配置方法返回SimpleJdbcCall的当前实例, 以便能够以"流畅"的接口风格将多个实例链接在一起.
 */
public class SimpleJdbcCall extends AbstractJdbcCall implements SimpleJdbcCallOperations {

	/**
	 * @param dataSource 要使用的{@code DataSource}
	 */
	public SimpleJdbcCall(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * @param jdbcTemplate 要使用的{@code JdbcTemplate}
	 */
	public SimpleJdbcCall(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}


	@Override
	public SimpleJdbcCall withProcedureName(String procedureName) {
		setProcedureName(procedureName);
		setFunction(false);
		return this;
	}

	@Override
	public SimpleJdbcCall withFunctionName(String functionName) {
		setProcedureName(functionName);
		setFunction(true);
		return this;
	}

	@Override
	public SimpleJdbcCall withSchemaName(String schemaName) {
		setSchemaName(schemaName);
		return this;
	}

	@Override
	public SimpleJdbcCall withCatalogName(String catalogName) {
		setCatalogName(catalogName);
		return this;
	}

	@Override
	public SimpleJdbcCall withReturnValue() {
		setReturnValueRequired(true);
		return this;
	}

	@Override
	public SimpleJdbcCall declareParameters(SqlParameter... sqlParameters) {
		for (SqlParameter sqlParameter : sqlParameters) {
			if (sqlParameter != null) {
				addDeclaredParameter(sqlParameter);
			}
		}
		return this;
	}

	@Override
	public SimpleJdbcCall useInParameterNames(String... inParameterNames) {
		setInParameterNames(new LinkedHashSet<String>(Arrays.asList(inParameterNames)));
		return this;
	}

	@Override
	public SimpleJdbcCall returningResultSet(String parameterName, RowMapper<?> rowMapper) {
		addDeclaredRowMapper(parameterName, rowMapper);
		return this;
	}

	@Override
	public SimpleJdbcCall withoutProcedureColumnMetaDataAccess() {
		setAccessCallParameterMetaData(false);
		return this;
	}

	@Override
	public SimpleJdbcCall withNamedBinding() {
		setNamedBinding(true);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeFunction(Class<T> returnType, Object... args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeFunction(Class<T> returnType, Map<String, ?> args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeFunction(Class<T> returnType, SqlParameterSource args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeObject(Class<T> returnType, Object... args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeObject(Class<T> returnType, Map<String, ?> args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T executeObject(Class<T> returnType, SqlParameterSource args) {
		return (T) doExecute(args).get(getScalarOutParameterName());
	}

	@Override
	public Map<String, Object> execute(Object... args) {
		return doExecute(args);
	}

	@Override
	public Map<String, Object> execute(Map<String, ?> args) {
		return doExecute(args);
	}

	@Override
	public Map<String, Object> execute(SqlParameterSource parameterSource) {
		return doExecute(parameterSource);
	}

}
