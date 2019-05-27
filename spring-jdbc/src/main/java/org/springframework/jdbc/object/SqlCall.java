package org.springframework.jdbc.object;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.SqlParameter;

/**
 * RdbmsOperation使用JdbcTemplate, 并表示基于SQL的调用, 如存储过程或存储函数.
 *
 * <p>根据声明的参数配置Call​​ableStatementCreatorFactory.
 */
public abstract class SqlCall extends RdbmsOperation {

	/**
	 * 能够根据此类声明的参数有效地创建CallableStatementCreator的对象.
	 */
	private CallableStatementCreatorFactory callableStatementFactory;

	/**
	 * 表示此调用是否为函数, 并使用 {? = call get_invoice_count(?)}语法.
	 */
	private boolean function = false;

	/**
	 * 表示此调用的sql应该完全按照定义使用.
	 * 无需添加转义语法和参数占位符.
	 */
	private boolean sqlReadyForUse = false;

	/**
	 * 调用java.sql.CallableStatement中定义的字符串.
	 * 如果如果isFunction设置为true, 则为{call add_invoice(?, ?, ?)} 或 {? = call get_invoice_count(?)}形式的字符串.
	 * 添加每个参数后更新.
	 */
	private String callString;


	/**
	 * 允许用作JavaBean的构造方法.
	 * 在调用{@code compile}方法并使用此对象之前, 必须提供DataSource, SQL和任何参数.
	 */
	public SqlCall() {
	}

	/**
	 * 使用SQL创建一个新的SqlCall对象, 但没有参数.
	 * 必须添加参数或无结果.
	 * 
	 * @param ds 从中获取连接的DataSource
	 * @param sql 要执行的SQL
	 */
	public SqlCall(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}


	/**
	 * 设置此调用是否用于函数.
	 */
	public void setFunction(boolean function) {
		this.function = function;
	}

	/**
	 * 返回此调用是否用于函数.
	 */
	public boolean isFunction() {
		return function;
	}

	/**
	 * 设置SQL是否可以按原样使用.
	 */
	public void setSqlReadyForUse(boolean sqlReadyForUse) {
		this.sqlReadyForUse = sqlReadyForUse;
	}

	/**
	 * 返回SQL是否可以按原样使用.
	 */
	public boolean isSqlReadyForUse() {
		return sqlReadyForUse;
	}


	/**
	 * 重写方法, 根据声明的参数配置Call​​ableStatementCreatorFactory.
	 */
	@Override
	protected final void compileInternal() {
		if (isSqlReadyForUse()) {
			this.callString = getSql();
		}
		else {
			List<SqlParameter> parameters = getDeclaredParameters();
			int parameterCount = 0;
			if (isFunction()) {
				this.callString = "{? = call " + getSql() + "(";
				parameterCount = -1;
			}
			else {
				this.callString = "{call " + getSql() + "(";
			}
			for (SqlParameter parameter : parameters) {
				if (!(parameter.isResultsParameter())) {
					if (parameterCount > 0) {
						this.callString += ", ";
					}
					if (parameterCount >= 0) {
						this.callString += "?";
					}
					parameterCount++;
				}
			}
			this.callString += ")}";
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled stored procedure. Call string is [" + getCallString() + "]");
		}

		this.callableStatementFactory = new CallableStatementCreatorFactory(getCallString(), getDeclaredParameters());
		this.callableStatementFactory.setResultSetType(getResultSetType());
		this.callableStatementFactory.setUpdatableResults(isUpdatableResults());
		this.callableStatementFactory.setNativeJdbcExtractor(getJdbcTemplate().getNativeJdbcExtractor());

		onCompileInternal();
	}

	/**
	 * 子类可以覆盖的Hook方法, 以响应编译.
	 * 此实现什么都不做.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * 获取调用字符串.
	 */
	public String getCallString() {
		return this.callString;
	}

	/**
	 * 返回使用此参数执行操作的CallableStatementCreator.
	 * 
	 * @param inParams 参数. May be {@code null}.
	 */
	protected CallableStatementCreator newCallableStatementCreator(Map<String, ?> inParams) {
		return this.callableStatementFactory.newCallableStatementCreator(inParams);
	}

	/**
	 * 返回使用从此ParameterMapper返回的参数执行操作的CallableStatementCreator.
	 * 
	 * @param inParamMapper parametermapper. May not be {@code null}.
	 */
	protected CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
		return this.callableStatementFactory.newCallableStatementCreator(inParamMapper);
	}
}
