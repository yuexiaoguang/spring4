package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.StringUtils;

/**
 * 用于管理用于配置和执行存储过程调用的上下文元数据的类.
 */
public class CallMetaDataContext {

	// Logger available to subclasses
	protected final Log logger = LogFactory.getLog(getClass());

	// 要调用的过程的名称
	private String procedureName;

	// 要调用的catalog的名称
	private String catalogName;

	// 要调用的schema的名称
	private String schemaName;

	// 要在调用执行中使用的SqlParameter对象的列表
	private List<SqlParameter> callParameters = new ArrayList<SqlParameter>();

	// 用于输出映射中的返回值的实际名称
	private String actualFunctionReturnName;

	// 参数名称中的一组, 用于排除未列出的参数
	private Set<String> limitedInParameterNames = new HashSet<String>();

	// out参数的SqlParameter名称列表
	private List<String> outParameterNames = new ArrayList<String>();

	// 指示这是过程还是函数
	private boolean function = false;

	// 指示是否应包含此过程的返回值
	private boolean returnValueRequired = false;

	// 是否应该访问调用参数元数据信息
	private boolean accessCallParameterMetaData = true;

	// 是否应该按名称绑定参数
	private boolean namedBinding;

	// 调用元数据的提供者
	private CallMetaDataProvider metaDataProvider;


	/**
	 * 指定用于函数返回值的名称.
	 */
	public void setFunctionReturnName(String functionReturnName) {
		this.actualFunctionReturnName = functionReturnName;
	}

	/**
	 * 获取用于函数返回值的名称.
	 */
	public String getFunctionReturnName() {
		return (this.actualFunctionReturnName != null ? this.actualFunctionReturnName : "return");
	}

	/**
	 * 指定要使用的一组限制的参数.
	 */
	public void setLimitedInParameterNames(Set<String> limitedInParameterNames) {
		this.limitedInParameterNames = limitedInParameterNames;
	}

	/**
	 * 获取要使用的限制的参数.
	 */
	public Set<String> getLimitedInParameterNames() {
		return this.limitedInParameterNames;
	}

	/**
	 * 指定out参数的名称.
	 */
	public void setOutParameterNames(List<String> outParameterNames) {
		this.outParameterNames = outParameterNames;
	}

	/**
	 * 获取out参数名称的列表.
	 */
	public List<String> getOutParameterNames() {
		return this.outParameterNames;
	}

	/**
	 * 指定过程的名称.
	 */
	public void setProcedureName(String procedureName) {
		this.procedureName = procedureName;
	}

	/**
	 * 获取过程的名称.
	 */
	public String getProcedureName() {
		return this.procedureName;
	}

	/**
	 * 指定catalog的名称.
	 */
	public void setCatalogName(String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * 获取catalog的名称.
	 */
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * 确定schema的名称.
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * 获取schema的名称.
	 */
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 * 指定此调用是否为函数调用.
	 */
	public void setFunction(boolean function) {
		this.function = function;
	}

	/**
	 * 检查此调用是否为函数调用.
	 */
	public boolean isFunction() {
		return this.function;
	}

	/**
	 * 指定是否需要返回值.
	 */
	public void setReturnValueRequired(boolean returnValueRequired) {
		this.returnValueRequired = returnValueRequired;
	}

	/**
	 * 检查是否需要返回值.
	 */
	public boolean isReturnValueRequired() {
		return this.returnValueRequired;
	}

	/**
	 * 指定是否应访问调用参数元数据.
	 */
	public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
		this.accessCallParameterMetaData = accessCallParameterMetaData;
	}

	/**
	 * 检查是否应该访问调用参数元数据.
	 */
	public boolean isAccessCallParameterMetaData() {
		return this.accessCallParameterMetaData;
	}

	/**
	 * 指定参数是否应按名称绑定.
	 */
	public void setNamedBinding(boolean namedBinding) {
		this.namedBinding = namedBinding;
	}

	/**
	 * 检查参数是否应按名称绑定.
	 */
	public boolean isNamedBinding() {
		return this.namedBinding;
	}


	/**
	 * 使用数据库中的元数据初始化此类.
	 * 
	 * @param dataSource 用于检索元数据的DataSource
	 */
	public void initializeMetaData(DataSource dataSource) {
		this.metaDataProvider = CallMetaDataProviderFactory.createMetaDataProvider(dataSource, this);
	}

	/**
	 * 根据用于正在使用的数据库的JDBC驱动程序提供的支持, 创建ReturnResultSetParameter/SqlOutParameter.
	 * 
	 * @param parameterName 参数的名称 (也用作输出中返回的List的名称)
	 * @param rowMapper RowMapper实现, 用于映射结果集中返回的数据
	 * 
	 * @return 适当的SqlParameter
	 */
	public SqlParameter createReturnResultSetParameter(String parameterName, RowMapper<?> rowMapper) {
		if (this.metaDataProvider.isReturnResultSetSupported()) {
			return new SqlReturnResultSet(parameterName, rowMapper);
		}
		else {
			if (this.metaDataProvider.isRefCursorSupported()) {
				return new SqlOutParameter(parameterName, this.metaDataProvider.getRefCursorSqlType(), rowMapper);
			}
			else {
				throw new InvalidDataAccessApiUsageException(
						"Return of a ResultSet from a stored procedure is not supported");
			}
		}
	}

	/**
	 * 获取此调用的单个输出参数的名称.
	 * 如果有多个参数, 则返回第一个参数的名称.
	 */
	public String getScalarOutParameterName() {
		if (isFunction()) {
			return getFunctionReturnName();
		}
		else {
			if (this.outParameterNames.size() > 1) {
				logger.warn("Accessing single output value when procedure has more than one output parameter");
			}
			return (!this.outParameterNames.isEmpty() ? this.outParameterNames.get(0) : null);
		}
	}

	/**
	 * 获取要在调用执行中使用的SqlParameter对象的列表.
	 */
	public List<SqlParameter> getCallParameters() {
		return this.callParameters;
	}

	/**
	 * 处理提供的参数列表, 如果使用过程列元数据, 则参数将与元数据信息匹配, 并且将自动包含任何缺失的参数.
	 * 
	 * @param parameters 用作基础的参数列表
	 */
	public void processParameters(List<SqlParameter> parameters) {
		this.callParameters = reconcileParameters(parameters);
	}

	/**
	 * 将提供的参数与可用的元数据协调, 并在适当的位置添加新的元数据.
	 */
	protected List<SqlParameter> reconcileParameters(List<SqlParameter> parameters) {
		final List<SqlParameter> declaredReturnParams = new ArrayList<SqlParameter>();
		final Map<String, SqlParameter> declaredParams = new LinkedHashMap<String, SqlParameter>();
		boolean returnDeclared = false;
		List<String> outParamNames = new ArrayList<String>();
		List<String> metaDataParamNames = new ArrayList<String>();

		// 获取元数据参数的名称
		for (CallParameterMetaData meta : this.metaDataProvider.getCallParameterMetaData()) {
			if (!meta.isReturnParameter()) {
				metaDataParamNames.add(meta.getParameterName().toLowerCase());
			}
		}

		// 从显式参数中分离隐式返回参数...
		for (SqlParameter param : parameters) {
			if (param.isResultsParameter()) {
				declaredReturnParams.add(param);
			}
			else {
				String paramName = param.getName();
				if (paramName == null) {
					throw new IllegalArgumentException("Anonymous parameters not supported for calls - " +
							"please specify a name for the parameter of SQL type " + param.getSqlType());
				}
				String paramNameToMatch = this.metaDataProvider.parameterNameToUse(paramName).toLowerCase();
				declaredParams.put(paramNameToMatch, param);
				if (param instanceof SqlOutParameter) {
					outParamNames.add(paramName);
					if (isFunction() && !metaDataParamNames.contains(paramNameToMatch) && !returnDeclared) {
						if (logger.isDebugEnabled()) {
							logger.debug("Using declared out parameter '" + paramName + "' for function return value");
						}
						setFunctionReturnName(paramName);
						returnDeclared = true;
					}
				}
			}
		}
		setOutParameterNames(outParamNames);

		List<SqlParameter> workParams = new ArrayList<SqlParameter>();
		workParams.addAll(declaredReturnParams);
		if (!this.metaDataProvider.isProcedureColumnMetaDataUsed()) {
			workParams.addAll(declaredParams.values());
			return workParams;
		}

		Map<String, String> limitedInParamNamesMap = new HashMap<String, String>(this.limitedInParameterNames.size());
		for (String limitedParamName : this.limitedInParameterNames) {
			limitedInParamNamesMap.put(
					this.metaDataProvider.parameterNameToUse(limitedParamName).toLowerCase(), limitedParamName);
		}

		for (CallParameterMetaData meta : this.metaDataProvider.getCallParameterMetaData()) {
			String paramNameToCheck = null;
			if (meta.getParameterName() != null) {
				paramNameToCheck = this.metaDataProvider.parameterNameToUse(meta.getParameterName()).toLowerCase();
			}
			String paramNameToUse = this.metaDataProvider.parameterNameToUse(meta.getParameterName());
			if (declaredParams.containsKey(paramNameToCheck) || (meta.isReturnParameter() && returnDeclared)) {
				SqlParameter param;
				if (meta.isReturnParameter()) {
					param = declaredParams.get(getFunctionReturnName());
					if (param == null && !getOutParameterNames().isEmpty()) {
						param = declaredParams.get(getOutParameterNames().get(0).toLowerCase());
					}
					if (param == null) {
						throw new InvalidDataAccessApiUsageException(
								"Unable to locate declared parameter for function return value - " +
								" add a SqlOutParameter with name '" + getFunctionReturnName() + "'");
					}
					else {
						setFunctionReturnName(param.getName());
					}
				}
				else {
					param = declaredParams.get(paramNameToCheck);
				}
				if (param != null) {
					workParams.add(param);
					if (logger.isDebugEnabled()) {
						logger.debug("Using declared parameter for '" +
								(paramNameToUse != null ? paramNameToUse : getFunctionReturnName()) + "'");
					}
				}
			}
			else {
				if (meta.isReturnParameter()) {
					// DatabaseMetaData.procedureColumnReturn 或 procedureColumnResult
					if (!isFunction() && !isReturnValueRequired() &&
							this.metaDataProvider.byPassReturnParameter(meta.getParameterName())) {
						if (logger.isDebugEnabled()) {
							logger.debug("Bypassing meta-data return parameter for '" + meta.getParameterName() + "'");
						}
					}
					else {
						String returnNameToUse =
								(StringUtils.hasLength(meta.getParameterName()) ? paramNameToUse : getFunctionReturnName());
						workParams.add(this.metaDataProvider.createDefaultOutParameter(returnNameToUse, meta));
						if (isFunction()) {
							setFunctionReturnName(returnNameToUse);
							outParamNames.add(returnNameToUse);
						}
						if (logger.isDebugEnabled()) {
							logger.debug("Added meta-data return parameter for '" + returnNameToUse + "'");
						}
					}
				}
				else {
					if (meta.getParameterType() == DatabaseMetaData.procedureColumnOut) {
						workParams.add(this.metaDataProvider.createDefaultOutParameter(paramNameToUse, meta));
						outParamNames.add(paramNameToUse);
						if (logger.isDebugEnabled()) {
							logger.debug("Added meta-data out parameter for '" + paramNameToUse + "'");
						}
					}
					else if (meta.getParameterType() == DatabaseMetaData.procedureColumnInOut) {
						workParams.add(this.metaDataProvider.createDefaultInOutParameter(paramNameToUse, meta));
						outParamNames.add(paramNameToUse);
						if (logger.isDebugEnabled()) {
							logger.debug("Added meta-data in-out parameter for '" + paramNameToUse + "'");
						}
					}
					else {
						if (this.limitedInParameterNames.isEmpty() ||
								limitedInParamNamesMap.containsKey(paramNameToUse.toLowerCase())) {
							workParams.add(this.metaDataProvider.createDefaultInParameter(paramNameToUse, meta));
							if (logger.isDebugEnabled()) {
								logger.debug("Added meta-data in parameter for '" + paramNameToUse + "'");
							}
						}
						else {
							if (logger.isDebugEnabled()) {
								logger.debug("Limited set of parameters " + limitedInParamNamesMap.keySet() +
										" skipped parameter for '" + paramNameToUse + "'");
							}
						}
					}
				}
			}
		}

		return workParams;
	}

	/**
	 * 将输入参数值与声明要在调用中使用的参数匹配.
	 * 
	 * @param parameterSource 输入值
	 * 
	 * @return 包含匹配参数名称的Map, 其值为从输入中获取的值
	 */
	public Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
		// 对于参数源查找, 需要提供不区分大小写的查找支持, 因为数据库元数据不一定提供区分大小写的参数名称.
		Map<String, String> caseInsensitiveParameterNames =
				SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);

		Map<String, String> callParameterNames = new HashMap<String, String>(this.callParameters.size());
		Map<String, Object> matchedParameters = new HashMap<String, Object>(this.callParameters.size());
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName = parameter.getName();
				String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
				if (parameterNameToMatch != null) {
					callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
				}
				if (parameterName != null) {
					if (parameterSource.hasValue(parameterName)) {
						matchedParameters.put(parameterName,
								SqlParameterSourceUtils.getTypedValue(parameterSource, parameterName));
					}
					else {
						String lowerCaseName = parameterName.toLowerCase();
						if (parameterSource.hasValue(lowerCaseName)) {
							matchedParameters.put(parameterName,
									SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
						}
						else {
							String englishLowerCaseName = parameterName.toLowerCase(Locale.ENGLISH);
							if (parameterSource.hasValue(englishLowerCaseName)) {
								matchedParameters.put(parameterName,
										SqlParameterSourceUtils.getTypedValue(parameterSource, englishLowerCaseName));
							}
							else {
								String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(parameterName);
								if (parameterSource.hasValue(propertyName)) {
									matchedParameters.put(parameterName,
											SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
								}
								else {
									if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
										String sourceName = caseInsensitiveParameterNames.get(lowerCaseName);
										matchedParameters.put(parameterName,
												SqlParameterSourceUtils.getTypedValue(parameterSource, sourceName));
									}
									else if (logger.isWarnEnabled()) {
										logger.warn("Unable to locate the corresponding parameter value for '" +
												parameterName + "' within the parameter values provided: " +
												caseInsensitiveParameterNames.values());
									}
								}
							}
						}
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Matching " + caseInsensitiveParameterNames.values() + " with " + callParameterNames.values());
			logger.debug("Found match for " + matchedParameters.keySet());
		}
		return matchedParameters;
	}

	/**
	 * 将输入参数值与声明要在调用中使用的参数匹配.
	 * 
	 * @param inParameters 输入值
	 * 
	 * @return 包含匹配参数名称的Map, 其值为从输入中获取的值
	 */
	public Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> inParameters) {
		if (!this.metaDataProvider.isProcedureColumnMetaDataUsed()) {
			return inParameters;
		}

		Map<String, String> callParameterNames = new HashMap<String, String>(this.callParameters.size());
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName =  parameter.getName();
				String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
				if (parameterNameToMatch != null) {
					callParameterNames.put(parameterNameToMatch.toLowerCase(), parameterName);
				}
			}
		}

		Map<String, Object> matchedParameters = new HashMap<String, Object>(inParameters.size());
		for (String parameterName : inParameters.keySet()) {
			String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
			String callParameterName = callParameterNames.get(parameterNameToMatch.toLowerCase());
			if (callParameterName == null) {
				if (logger.isDebugEnabled()) {
					Object value = inParameters.get(parameterName);
					if (value instanceof SqlParameterValue) {
						value = ((SqlParameterValue) value).getValue();
					}
					if (value != null) {
						logger.debug("Unable to locate the corresponding IN or IN-OUT parameter for \"" +
								parameterName + "\" in the parameters used: " + callParameterNames.keySet());
					}
				}
			}
			else {
				matchedParameters.put(callParameterName, inParameters.get(parameterName));
			}
		}

		if (matchedParameters.size() < callParameterNames.size()) {
			for (String parameterName : callParameterNames.keySet()) {
				String parameterNameToMatch = this.metaDataProvider.parameterNameToUse(parameterName);
				String callParameterName = callParameterNames.get(parameterNameToMatch.toLowerCase());
				if (!matchedParameters.containsKey(callParameterName) && logger.isWarnEnabled()) {
					logger.warn("Unable to locate the corresponding parameter value for '" + parameterName +
							"' within the parameter values provided: " + inParameters.keySet());
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Matching " + inParameters.keySet() + " with " + callParameterNames.values());
			logger.debug("Found match for " + matchedParameters.keySet());
		}
		return matchedParameters;
	}

	public Map<String, ?> matchInParameterValuesWithCallParameters(Object[] parameterValues) {
		Map<String, Object> matchedParameters = new HashMap<String, Object>(parameterValues.length);
		int i = 0;
		for (SqlParameter parameter : this.callParameters) {
			if (parameter.isInputValueProvided()) {
				String parameterName =  parameter.getName();
				matchedParameters.put(parameterName, parameterValues[i++]);
			}
		}
		return matchedParameters;
	}

	/**
	 * 根据配置和元数据信息构建调用字符串.
	 * 
	 * @return 要使用的调用字符串
	 */
	public String createCallString() {
		StringBuilder callString;
		int parameterCount = 0;
		String catalogNameToUse;
		String schemaNameToUse;

		// 对于不支持catalog的Oracle, 需要反转schema名称和catalog名称, 因为cataog用于包名称
		if (this.metaDataProvider.isSupportsSchemasInProcedureCalls() &&
				!this.metaDataProvider.isSupportsCatalogsInProcedureCalls()) {
			schemaNameToUse = this.metaDataProvider.catalogNameToUse(getCatalogName());
			catalogNameToUse = this.metaDataProvider.schemaNameToUse(getSchemaName());
		}
		else {
			catalogNameToUse = this.metaDataProvider.catalogNameToUse(getCatalogName());
			schemaNameToUse = this.metaDataProvider.schemaNameToUse(getSchemaName());
		}

		String procedureNameToUse = this.metaDataProvider.procedureNameToUse(getProcedureName());
		if (isFunction() || isReturnValueRequired()) {
			callString = new StringBuilder().append("{? = call ").
					append(StringUtils.hasLength(catalogNameToUse) ? catalogNameToUse + "." : "").
					append(StringUtils.hasLength(schemaNameToUse) ? schemaNameToUse + "." : "").
					append(procedureNameToUse).append("(");
			parameterCount = -1;
		}
		else {
			callString = new StringBuilder().append("{call ").
					append(StringUtils.hasLength(catalogNameToUse) ? catalogNameToUse + "." : "").
					append(StringUtils.hasLength(schemaNameToUse) ? schemaNameToUse + "." : "").
					append(procedureNameToUse).append("(");
		}

		for (SqlParameter parameter : this.callParameters) {
			if (!parameter.isResultsParameter()) {
				if (parameterCount > 0) {
					callString.append(", ");
				}
				if (parameterCount >= 0) {
					callString.append(createParameterBinding(parameter));
				}
				parameterCount++;
			}
		}
		callString.append(")}");

		return callString.toString();
	}

	/**
	 * 构建参数绑定片段.
	 * 
	 * @param parameter 调用参数
	 * 
	 * @return 绑定片段的参数
	 */
	protected String createParameterBinding(SqlParameter parameter) {
		return (isNamedBinding() ? parameter.getName() + " => ?" : "?");
	}
}
