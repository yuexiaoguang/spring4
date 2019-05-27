package org.springframework.jdbc.core.simple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 抽象类, 用于根据配置选项和数据库元数据为简单的存储过程调用提供基本功能.
 *
 * <p>该类为{@link SimpleJdbcCall}提供基本SPI.
 */
public abstract class AbstractJdbcCall {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 用于执行SQL的低级类 */
	private final JdbcTemplate jdbcTemplate;

	/** 用于检索和管理数据库元数据的上下文 */
	private final CallMetaDataContext callMetaDataContext = new CallMetaDataContext();

	/** SqlParameter对象列表 */
	private final List<SqlParameter> declaredParameters = new ArrayList<SqlParameter>();

	/** RefCursor/ResultSet RowMapper对象列表 */
	private final Map<String, RowMapper<?>> declaredRowMappers = new LinkedHashMap<String, RowMapper<?>>();

	/**
	 * 此操作是否已编译?
	 * 编译意味着至少检查是否已提供DataSource或JdbcTemplate.
	 */
	private volatile boolean compiled = false;

	/** 生成的字符串, 用于调用语句 */
	private String callString;

	/**
	 * 一个委托, 使我们能够根据此类声明的参数有效地创建CallableStatementCreators.
	 */
	private CallableStatementCreatorFactory callableStatementFactory;


	/**
	 * @param dataSource 要使用的DataSource
	 */
	protected AbstractJdbcCall(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * @param jdbcTemplate 要使用的JdbcTemplate
	 */
	protected AbstractJdbcCall(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}


	/**
	 * 获取配置的{@link JdbcTemplate}.
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * 设置存储过程的名称.
	 */
	public void setProcedureName(String procedureName) {
		this.callMetaDataContext.setProcedureName(procedureName);
	}

	/**
	 * 获取存储过程的名称.
	 */
	public String getProcedureName() {
		return this.callMetaDataContext.getProcedureName();
	}

	/**
	 * 设置要使用的in参数的名称.
	 */
	public void setInParameterNames(Set<String> inParameterNames) {
		this.callMetaDataContext.setLimitedInParameterNames(inParameterNames);
	}

	/**
	 * 获取要使用的in参数的名称.
	 */
	public Set<String> getInParameterNames() {
		return this.callMetaDataContext.getLimitedInParameterNames();
	}

	/**
	 * 设置要使用的catalog名称.
	 */
	public void setCatalogName(String catalogName) {
		this.callMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * 获取使用的catalog名称.
	 */
	public String getCatalogName() {
		return this.callMetaDataContext.getCatalogName();
	}

	/**
	 * 设置要使用的schema名称.
	 */
	public void setSchemaName(String schemaName) {
		this.callMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * 获取使用的schema名称.
	 */
	public String getSchemaName() {
		return this.callMetaDataContext.getSchemaName();
	}

	/**
	 * 指定此调用是否为函数调用.
	 * 默认{@code false}.
	 */
	public void setFunction(boolean function) {
		this.callMetaDataContext.setFunction(function);
	}

	/**
	 * 此调用是否为函数调用?
	 */
	public boolean isFunction() {
		return this.callMetaDataContext.isFunction();
	}

	/**
	 * 指定调用是否需要返回值.
	 * 默认{@code false}.
	 */
	public void setReturnValueRequired(boolean returnValueRequired) {
		this.callMetaDataContext.setReturnValueRequired(returnValueRequired);
	}

	/**
	 * 调用是否需要返回值?
	 */
	public boolean isReturnValueRequired() {
		return this.callMetaDataContext.isReturnValueRequired();
	}

	/**
	 * 指定参数是否应按名称绑定.
	 * 默认{@code false}.
	 */
	public void setNamedBinding(boolean namedBinding) {
		this.callMetaDataContext.setNamedBinding(namedBinding);
	}

	/**
	 * 参数是否应按名称绑定?
	 */
	public boolean isNamedBinding() {
		return this.callMetaDataContext.isNamedBinding();
	}

	/**
	 * 指定是否应使用调用的参数元数据.
	 * 默认{@code true}.
	 */
	public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
		this.callMetaDataContext.setAccessCallParameterMetaData(accessCallParameterMetaData);
	}

	/**
	 * 根据参数和元数据, 获取应该使用的调用字符串.
	 */
	public String getCallString() {
		return this.callString;
	}

	/**
	 * 获取正在使用的{@link CallableStatementCreatorFactory}
	 */
	protected CallableStatementCreatorFactory getCallableStatementFactory() {
		return this.callableStatementFactory;
	}


	/**
	 * 将声明的参数添加到调用的参数列表中.
	 * <p>只有声明为{@code SqlParameter}和{@code SqlInOutParameter}的参数才会用于提供输入值.
	 * 这与{@code StoredProcedure}类不同 - 为了向后兼容性 - 它允许为声明为{@code SqlOutParameter}的参数提供输入值.
	 * 
	 * @param parameter 要添加的{@link SqlParameter}
	 */
	public void addDeclaredParameter(SqlParameter parameter) {
		Assert.notNull(parameter, "The supplied parameter must not be null");
		if (!StringUtils.hasText(parameter.getName())) {
			throw new InvalidDataAccessApiUsageException(
					"You must specify a parameter name when declaring parameters for \"" + getProcedureName() + "\"");
		}
		this.declaredParameters.add(parameter);
		if (logger.isDebugEnabled()) {
			logger.debug("Added declared parameter for [" + getProcedureName() + "]: " + parameter.getName());
		}
	}

	/**
	 * 为指定的参数或列添加{@link org.springframework.jdbc.core.RowMapper}.
	 * 
	 * @param parameterName 参数或列的名称
	 * @param rowMapper 要使用的RowMapper实现
	 */
	public void addDeclaredRowMapper(String parameterName, RowMapper<?> rowMapper) {
		this.declaredRowMappers.put(parameterName, rowMapper);
		if (logger.isDebugEnabled()) {
			logger.debug("Added row mapper for [" + getProcedureName() + "]: " + parameterName);
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling compilation issues
	//-------------------------------------------------------------------------

	/**
	 * 使用提供的参数和元数据以及其他设置编译此JdbcCall.
	 * <p>这将最终确定此对象的配置, 并忽略后续的编译尝试.
	 * 这将在第一次执行未编译的调用时被隐式调用.
	 * 
	 * @throws org.springframework.dao.InvalidDataAccessApiUsageException 如果对象尚未正确初始化, 例如, 如果未提供任何DataSource
	 */
	public final synchronized void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getProcedureName() == null) {
				throw new InvalidDataAccessApiUsageException("Procedure or Function name is required");
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
				logger.debug("SqlCall for " + (isFunction() ? "function" : "procedure") +
						" [" + getProcedureName() + "] compiled");
			}
		}
	}

	/**
	 * 委托方法执行实际编译.
	 * <p>子类可以覆盖此模板方法以执行自己的编译.
	 * 在此基类的编译完成后调用.
	 */
	protected void compileInternal() {
		this.callMetaDataContext.initializeMetaData(getJdbcTemplate().getDataSource());

		// 迭代声明的RowMapper, 并注册相应的SqlParameter
		for (Map.Entry<String, RowMapper<?>> entry : this.declaredRowMappers.entrySet()) {
			SqlParameter resultSetParameter =
					this.callMetaDataContext.createReturnResultSetParameter(entry.getKey(), entry.getValue());
			this.declaredParameters.add(resultSetParameter);
		}
		this.callMetaDataContext.processParameters(this.declaredParameters);

		this.callString = this.callMetaDataContext.createCallString();
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled stored procedure. Call string is [" + this.callString + "]");
		}

		this.callableStatementFactory =
				new CallableStatementCreatorFactory(getCallString(), this.callMetaDataContext.getCallParameters());
		this.callableStatementFactory.setNativeJdbcExtractor(getJdbcTemplate().getNativeJdbcExtractor());

		onCompileInternal();
	}

	/**
	 * 子类可以覆盖的Hook方法, 以响应编译.
	 * 此实现为空.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * 这个操作是"编译"的吗?
	 * 
	 * @return 此操作是否已编译并可以使用
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * 检查此操作是否已编译; 如果还没有编译, 延迟地编译它.
	 * <p>由{@code doExecute}自动调用.
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("JdbcCall call not compiled before execution - invoking compile");
			compile();
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling execution
	//-------------------------------------------------------------------------

	/**
	 * 使用传入的{@link SqlParameterSource}执行调用的委托方法.
	 * 
	 * @param parameterSource 要在调用中使用的参数名称和值
	 * 
	 * @return out参数的Map
	 */
	protected Map<String, Object> doExecute(SqlParameterSource parameterSource) {
		checkCompiled();
		Map<String, Object> params = matchInParameterValuesWithCallParameters(parameterSource);
		return executeCallInternal(params);
	}

	/**
	 * 使用传入的参数数组执行调用的委托方法.
	 * 
	 * @param args 参数值数组. 值的顺序必须与为存储过程声明的顺序匹配.
	 * 
	 * @return out参数的Map
	 */
	protected Map<String, Object> doExecute(Object... args) {
		checkCompiled();
		Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
		return executeCallInternal(params);
	}

	/**
	 * 使用传入的参数Map执行调用的委托方法.
	 * 
	 * @param args 参数名称和值的Map
	 * 
	 * @return out参数的Map
	 */
	protected Map<String, Object> doExecute(Map<String, ?> args) {
		checkCompiled();
		Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
		return executeCallInternal(params);
	}

	/**
	 * 执行实际的调用处理的委托方法.
	 */
	private Map<String, Object> executeCallInternal(Map<String, ?> args) {
		CallableStatementCreator csc = getCallableStatementFactory().newCallableStatementCreator(args);
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for call " + getCallString() + " with " + args);
			int i = 1;
			for (SqlParameter param : getCallParameters()) {
				logger.debug(i + ": " +  param.getName() + ", SQL type "+ param.getSqlType() + ", type name " +
						param.getTypeName() + ", parameter class [" + param.getClass().getName() + "]");
				i++;
			}
		}
		return getJdbcTemplate().call(csc, getCallParameters());
	}


	/**
	 * 获取单个out参数或返回值的名称.
	 * 用于具有一个out参数的函数或过程.
	 */
	protected String getScalarOutParameterName() {
		return this.callMetaDataContext.getScalarOutParameterName();
	}

	/**
	 * 获取要用于调用的所有调用参数的列表.
	 * 这包括基于元数据处理添加的任何参数.
	 */
	protected List<SqlParameter> getCallParameters() {
		return this.callMetaDataContext.getCallParameters();
	}

	/**
	 * 将提供的参数值与已注册的参数和通过元数据处理定义的参数进行匹配.
	 * 
	 * @param parameterSource 提供的参数值
	 * 
	 * @return 参数名称和值的Map
	 */
	protected Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(parameterSource);
	}

	/**
	 * 将提供的参数值与已注册的参数和通过元数据处理定义的参数进行匹配.
	 * 
	 * @param args 提供的参数值
	 * 
	 * @return 参数名称和值的Map
	 */
	private Map<String, ?> matchInParameterValuesWithCallParameters(Object[] args) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
	}

	/**
	 * 将提供的参数值与已注册的参数和通过元数据处理定义的参数进行匹配.
	 * 
	 * @param args 提供的参数值
	 * 
	 * @return 参数名称和值的Map
	 */
	protected Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> args) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
	}

}
