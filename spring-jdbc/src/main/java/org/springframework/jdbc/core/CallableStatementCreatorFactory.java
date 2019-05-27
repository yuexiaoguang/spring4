package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * 根据SQL语句和一组参数声明, 有效地创建具有不同参数的多个{@link CallableStatementCreator}对象.
 */
public class CallableStatementCreatorFactory {

	/** SQL调用字符串, 在参数更改时不会更改. */
	private final String callString;

	/** SqlParameter对象列表. 可能不是{@code null}. */
	private final List<SqlParameter> declaredParameters;

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;

	private NativeJdbcExtractor nativeJdbcExtractor;


	/**
	 * 需要通过{@link #addParameter}方法添加参数, 或没有参数.
	 * 
	 * @param callString SQL调用字符串
	 */
	public CallableStatementCreatorFactory(String callString) {
		this.callString = callString;
		this.declaredParameters = new LinkedList<SqlParameter>();
	}

	/**
	 * @param callString SQL调用字符串
	 * @param declaredParameters {@link SqlParameter}对象
	 */
	public CallableStatementCreatorFactory(String callString, List<SqlParameter> declaredParameters) {
		this.callString = callString;
		this.declaredParameters = declaredParameters;
	}


	/**
	 * 添加新声明的参数.
	 * <p>参数添加顺序很重要.
	 * 
	 * @param param 要添加到已声明参数列表的参数
	 */
	public void addParameter(SqlParameter param) {
		this.declaredParameters.add(param);
	}

	/**
	 * 设置是否使用返回特定类型ResultSet的预准备语句.
	 * 特定类型的ResultSet.
	 * 
	 * @param resultSetType ResultSet类型
	 */
	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	/**
	 * 设置是否使用能够返回可更新ResultSet的预准备语句.
	 */
	public void setUpdatableResults(boolean updatableResults) {
		this.updatableResults = updatableResults;
	}

	/**
	 * 指定NativeJdbcExtractor用于解包CallableStatements.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}


	/**
	 * 给定此参数, 返回一个新的CallableStatementCreator实例.
	 * 
	 * @param params 参数列表 (may be {@code null})
	 */
	public CallableStatementCreator newCallableStatementCreator(Map<String, ?> params) {
		return new CallableStatementCreatorImpl(params != null ? params : new HashMap<String, Object>());
	}

	/**
	 * 给定此参数映射器, 返回一个新的CallableStatementCreator实例.
	 * 
	 * @param inParamMapper 将返回参数Map的ParameterMapper实现
	 */
	public CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
		return new CallableStatementCreatorImpl(inParamMapper);
	}


	/**
	 * 此类返回的CallableStatementCreator实现.
	 */
	private class CallableStatementCreatorImpl implements CallableStatementCreator, SqlProvider, ParameterDisposer {

		private ParameterMapper inParameterMapper;

		private Map<String, ?> inParameters;

		/**
		 * @param inParamMapper 用于映射输入参数的ParameterMapper实现
		 */
		public CallableStatementCreatorImpl(ParameterMapper inParamMapper) {
			this.inParameterMapper = inParamMapper;
		}

		/**
		 * @param inParams SqlParameter对象的列表
		 */
		public CallableStatementCreatorImpl(Map<String, ?> inParams) {
			this.inParameters = inParams;
		}

		@Override
		public CallableStatement createCallableStatement(Connection con) throws SQLException {
			// 如果给定一个ParameterMapper, 必须让mapper来创建Map.
			if (this.inParameterMapper != null) {
				this.inParameters = this.inParameterMapper.createMap(con);
			}
			else {
				if (this.inParameters == null) {
					throw new InvalidDataAccessApiUsageException(
							"A ParameterMapper or a Map of parameters must be provided");
				}
			}

			CallableStatement cs = null;
			if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
				cs = con.prepareCall(callString);
			}
			else {
				cs = con.prepareCall(callString, resultSetType,
						updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
			}

			// 确定要传递给自定义类型的CallabeStatement.
			CallableStatement csToUse = cs;
			if (nativeJdbcExtractor != null) {
				csToUse = nativeJdbcExtractor.getNativeCallableStatement(cs);
			}

			int sqlColIndx = 1;
			for (SqlParameter declaredParam : declaredParameters) {
				if (!declaredParam.isResultsParameter()) {
					// 所以, 它是一个调用参数 - 调用字符串的一部分.
					// 获取值 - 它可能仍为null.
					Object inValue = this.inParameters.get(declaredParam.getName());
					if (declaredParam instanceof ResultSetSupportingSqlParameter) {
						// 它是一个输出参数: SqlReturnResultSet参数已被排除.
						// 它不需要 (但可能) 由调用者提供.
						if (declaredParam instanceof SqlOutParameter) {
							if (declaredParam.getTypeName() != null) {
								cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getTypeName());
							}
							else {
								if (declaredParam.getScale() != null) {
									cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getScale());
								}
								else {
									cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType());
								}
							}
							if (declaredParam.isInputValueProvided()) {
								StatementCreatorUtils.setParameterValue(csToUse, sqlColIndx, declaredParam, inValue);
							}
						}
					}
					else {
						// 这是一个输入参数; 必须由调用者提供.
						if (!this.inParameters.containsKey(declaredParam.getName())) {
							throw new InvalidDataAccessApiUsageException(
									"Required input parameter '" + declaredParam.getName() + "' is missing");
						}
						StatementCreatorUtils.setParameterValue(csToUse, sqlColIndx, declaredParam, inValue);
					}
					sqlColIndx++;
				}
			}

			return cs;
		}

		@Override
		public String getSql() {
			return callString;
		}

		@Override
		public void cleanupParameters() {
			if (this.inParameters != null) {
				StatementCreatorUtils.cleanupParameters(this.inParameters.values());
			}
		}

		@Override
		public String toString() {
			return "CallableStatementCreator: sql=[" + callString + "]; parameters=" + this.inParameters;
		}
	}
}
