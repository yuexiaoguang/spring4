package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.util.Assert;

/**
 * "RDBMS 操作"是表示查询, 更新或存储过程调用的多线程可重用对象.
 * RDBMS操作<b>不是</b>命令, 因为命令不可重用.
 * 但是, execute方法可以将命令作为参数. 子类应该是JavaBeans, 允许轻松配置.
 *
 * <p>此类和子类抛出运行时异常, 在<code>org.springframework.dao 包</code>中定义
 * (并且由{@code org.springframework.jdbc.core}包引发, 该包中的类用于执行原始JDBC操作).
 *
 * <p>子类应在调用{@link #compile()}方法之前设置SQL并添加参数.
 * 添加参数的顺序非常重要. 然后可以调用适当的{@code execute}或{@code update}方法.
 */
public abstract class RdbmsOperation implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 用于执行SQL的低级类 */
	private JdbcTemplate jdbcTemplate = new JdbcTemplate();

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;

	private boolean returnGeneratedKeys = false;

	private String[] generatedKeysColumnNames = null;

	private String sql;

	private final List<SqlParameter> declaredParameters = new LinkedList<SqlParameter>();

	/**
	 * 此操作是否已编译?
	 * 编译意味着至少检查是否已提供DataSource和sql, 但子类也可以实现自己的自定义验证.
	 */
	private volatile boolean compiled;


	/**
	 * 当在多个{@code RdbmsOperations}中使用相同的{@link JdbcTemplate}时, 可以选择更常用的{@link #setDataSource}.
	 * 如果{@code JdbcTemplate}具有特殊配置可以重用,
	 * 例如{@link org.springframework.jdbc.support.SQLExceptionTranslator}, 这是合适的.
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 返回此操作对象使用的{@link JdbcTemplate}.
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * 设置获取连接的JDBC {@link DataSource}.
	 */
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate.setDataSource(dataSource);
	}

	/**
	 * 设置此RDBMS操作的获取大小.
	 * 这对于处理大型结果集很重要:
	 * 将其设置为高于默认值将以内存消耗为代价提高处理速度;
	 * 将此值设置得较低可以避免传输应用程序永远不会读取的行数据.
	 * <p>默认值为0, 表示使用驱动程序的默认值.
	 */
	public void setFetchSize(int fetchSize) {
		this.jdbcTemplate.setFetchSize(fetchSize);
	}

	/**
	 * 设置此RDBMS操作的最大行数.
	 * 这对于处理大型结果集的子集很重要, 避免在数据库或JDBC驱动程序中读取和保存整个结果集.
	 * <p>默认值为0, 表示使用驱动程序的默认值.
	 */
	public void setMaxRows(int maxRows) {
		this.jdbcTemplate.setMaxRows(maxRows);
	}

	/**
	 * 设置此RDBMS操作执行的语句的查询超时.
	 * <p>默认值为0, 表示使用驱动程序的默认值.
	 * <p>Note: 在事务级别指定超时的事务中执行时, 此处指定的任何超时都将被剩余的事务超时覆盖.
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.jdbcTemplate.setQueryTimeout(queryTimeout);
	}

	/**
	 * 设置是否使用返回特定类型ResultSet的语句.
	 * 
	 * @param resultSetType ResultSet类型
	 */
	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	/**
	 * 返回语句是否将返回特定类型的ResultSet.
	 */
	public int getResultSetType() {
		return this.resultSetType;
	}

	/**
	 * 设置是否使用能够返回可更新ResultSet的语句.
	 */
	public void setUpdatableResults(boolean updatableResults) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The updateableResults flag must be set before the operation is compiled");
		}
		this.updatableResults = updatableResults;
	}

	/**
	 * 返回语句是否将返回可更新的ResultSet.
	 */
	public boolean isUpdatableResults() {
		return this.updatableResults;
	}

	/**
	 * 设置预准备语句是否应该能够返回自动生成的键.
	 */
	public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The returnGeneratedKeys flag must be set before the operation is compiled");
		}
		this.returnGeneratedKeys = returnGeneratedKeys;
	}

	/**
	 * 返回语句是否应该能够返回自动生成的键.
	 */
	public boolean isReturnGeneratedKeys() {
		return this.returnGeneratedKeys;
	}

	/**
	 * 设置自动生成的键的列名称.
	 */
	public void setGeneratedKeysColumnNames(String... names) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The column names for the generated keys must be set before the operation is compiled");
		}
		this.generatedKeysColumnNames = names;
	}

	/**
	 * 返回自动生成的键的列名称.
	 */
	public String[] getGeneratedKeysColumnNames() {
		return this.generatedKeysColumnNames;
	}

	/**
	 * 设置此操作执行的SQL.
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	/**
	 * 如果需要, 子类可以覆盖它以提供动态SQL, 但通常通过调用{@link #setSql}方法或子类构造函数来设置SQL.
	 */
	public String getSql() {
		return this.sql;
	}

	/**
	 * 添加匿名参数, 仅指定{@code java.sql.Types}类中定义的SQL类型.
	 * <p>参数排序很重要. 此方法是{@link #declareParameter}方法的替代方法, 通常应该首选.
	 * 
	 * @param types {@code java.sql.Types}类中定义的SQL类型数组
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果操作已经编译
	 */
	public void setTypes(int[] types) throws InvalidDataAccessApiUsageException {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once query is compiled");
		}
		if (types != null) {
			for (int type : types) {
				declareParameter(new SqlParameter(type));
			}
		}
	}

	/**
	 * 声明此操作的参数.
	 * <p>使用位置参数时, 调用此方法的顺序非常重要.
	 * 在此处使用带有命名SqlParameter对象的命名参数时, 这并不重要;
	 * 在此处将命名参数与未命名的SqlParameter对象结合使用时, 它仍然很重要.
	 * 
	 * @param param 要添加的SqlParameter. 这将指定SQL类型和(可选)参数的名称.
	 * 请注意, 通常在此处使用{@link SqlParameter}类, 而不是其任何子类.
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果操作已经编译, 因此无法进一步配置
	 */
	public void declareParameter(SqlParameter param) throws InvalidDataAccessApiUsageException {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
		}
		this.declaredParameters.add(param);
	}

	/**
	 * 添加一个或多个声明的参数.
	 * 用于在bean工厂中使用时配置此操作.
	 * 每个参数都将指定SQL类型和 (可选)参数的名称.
	 * 
	 * @param parameters 包含声明的{@link SqlParameter}对象的数组
	 */
	public void setParameters(SqlParameter... parameters) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
		}
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i] != null) {
				this.declaredParameters.add(parameters[i]);
			}
			else {
				throw new InvalidDataAccessApiUsageException("Cannot add parameter at index " + i + " from " +
						Arrays.asList(parameters) + " since it is 'null'");
			}
		}
	}

	/**
	 * 返回声明的{@link SqlParameter}对象的列表.
	 */
	protected List<SqlParameter> getDeclaredParameters() {
		return this.declaredParameters;
	}


	/**
	 * 如果在bean工厂中使用, 则确保编译.
	 */
	@Override
	public void afterPropertiesSet() {
		compile();
	}

	/**
	 * 编译此查询.
	 * 忽略后续的编译尝试.
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果对象尚未正确初始化, 例如, 如果未提供任何DataSource
	 */
	public final void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getSql() == null) {
				throw new InvalidDataAccessApiUsageException("Property 'sql' is required");
			}

			try {
				this.jdbcTemplate.afterPropertiesSet();
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(ex.getMessage());
			}

			compileInternal();
			this.compiled = true;

			if (logger.isDebugEnabled()) {
				logger.debug("RdbmsOperation with SQL [" + getSql() + "] compiled");
			}
		}
	}

	/**
	 * 这个操作是"编译的"吗? 与在JDO中一样, 编译意味着操作已完全配置并可以使用.
	 * 编译的确切含义因子类而异.
	 * 
	 * @return 此操作是否已编译并可以使用
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * 检查此操作是否已编译; 如果还没有编译, 延迟地编译它.
	 * <p>由{@code validateParameters}自动调用.
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("SQL operation not compiled before execution - invoking compile");
			compile();
		}
	}

	/**
	 * 根据声明的参数验证传递给execute方法的参数.
	 * 子类应在每个{@code executeQuery()} 或 {@code update()}方法之前调用此方法.
	 * 
	 * @param parameters 提供的参数 (may be {@code null})
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果参数无效
	 */
	protected void validateParameters(Object[] parameters) throws InvalidDataAccessApiUsageException {
		checkCompiled();
		int declaredInParameters = 0;
		for (SqlParameter param : this.declaredParameters) {
			if (param.isInputValueProvided()) {
				if (!supportsLobParameters() &&
						(param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
					throw new InvalidDataAccessApiUsageException(
							"BLOB or CLOB parameters are not allowed for this kind of operation");
				}
				declaredInParameters++;
			}
		}
		validateParameterCount((parameters != null ? parameters.length : 0), declaredInParameters);
	}

	/**
	 * 根据声明的参数验证传递给execute方法的命名参数.
	 * 子类应在每个{@code executeQuery()} 或 {@code update()}方法之前调用此方法.
	 * 
	 * @param parameters 提供的参数Map (may be {@code null})
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果参数无效
	 */
	protected void validateNamedParameters(Map<String, ?> parameters) throws InvalidDataAccessApiUsageException {
		checkCompiled();
		Map<String, ?> paramsToUse = (parameters != null ? parameters : Collections.<String, Object> emptyMap());
		int declaredInParameters = 0;
		for (SqlParameter param : this.declaredParameters) {
			if (param.isInputValueProvided()) {
				if (!supportsLobParameters() &&
						(param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
					throw new InvalidDataAccessApiUsageException(
							"BLOB or CLOB parameters are not allowed for this kind of operation");
				}
				if (param.getName() != null && !paramsToUse.containsKey(param.getName())) {
					throw new InvalidDataAccessApiUsageException("The parameter named '" + param.getName() +
							"' was not among the parameters supplied: " + paramsToUse.keySet());
				}
				declaredInParameters++;
			}
		}
		validateParameterCount(paramsToUse.size(), declaredInParameters);
	}

	/**
	 * 根据给定的声明参数验证给定的参数计数.
	 * 
	 * @param suppliedParamCount 给出的实际参数的数量
	 * @param declaredInParamCount 声明的输入参数的数量
	 */
	private void validateParameterCount(int suppliedParamCount, int declaredInParamCount) {
		if (suppliedParamCount < declaredInParamCount) {
			throw new InvalidDataAccessApiUsageException(suppliedParamCount + " parameters were supplied, but " +
					declaredInParamCount + " in parameters were declared in class [" + getClass().getName() + "]");
		}
		if (suppliedParamCount > this.declaredParameters.size() && !allowsUnusedParameters()) {
			throw new InvalidDataAccessApiUsageException(suppliedParamCount + " parameters were supplied, but " +
					declaredInParamCount + " parameters were declared in class [" + getClass().getName() + "]");
		}
	}


	/**
	 * 子类必须实现此模板方法才能执行自己的编译.
	 * 在此基类的编译完成后调用.
	 * <p>子类可以假定已经提供了SQL和DataSource.
	 * 
	 * @throws InvalidDataAccessApiUsageException 如果子类未正确配置
	 */
	protected abstract void compileInternal() throws InvalidDataAccessApiUsageException;

	/**
	 * 返回此类操作是否支持BLOB/CLOB参数.
	 * <p>默认{@code true}.
	 */
	protected boolean supportsLobParameters() {
		return true;
	}

	/**
	 * 返回此操作是否接受给定但未实际使用的其他参数.
	 * 特别适用于参数Map.
	 * <p>默认{@code false}.
	 */
	protected boolean allowsUnusedParameters() {
		return false;
	}
}
