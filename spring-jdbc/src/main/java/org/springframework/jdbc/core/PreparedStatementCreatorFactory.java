package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.Assert;

/**
 * Helper类, 基于SQL语句和一组参数声明, 有效地创建具有不同参数的多个{@link PreparedStatementCreator}对象.
 */
public class PreparedStatementCreatorFactory {

	/** SQL, 在参数更改时不会更改 */
	private final String sql;

	/** SqlParameter对象列表 (可能不是 {@code null}) */
	private final List<SqlParameter> declaredParameters;

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;

	private boolean returnGeneratedKeys = false;

	private String[] generatedKeysColumnNames;

	private NativeJdbcExtractor nativeJdbcExtractor;


	/**
	 * 创建一个新工厂. 需要通过{@link #addParameter}方法添加参数或没有参数.
	 * 
	 * @param sql 要执行的SQL语句
	 */
	public PreparedStatementCreatorFactory(String sql) {
		this.sql = sql;
		this.declaredParameters = new LinkedList<SqlParameter>();
	}

	/**
	 * 使用给定的SQL和JDBC类型创建新工厂.
	 * 
	 * @param sql 要执行的SQL语句
	 * @param types JDBC类型数组
	 */
	public PreparedStatementCreatorFactory(String sql, int... types) {
		this.sql = sql;
		this.declaredParameters = SqlParameter.sqlTypesToAnonymousParameterList(types);
	}

	/**
	 * 使用给定的SQL和参数创建新工厂.
	 * 
	 * @param sql 要执行的SQL语句
	 * @param declaredParameters {@link SqlParameter}对象列表
	 */
	public PreparedStatementCreatorFactory(String sql, List<SqlParameter> declaredParameters) {
		this.sql = sql;
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
	 * 设置是否使用返回特定类型的ResultSet的预准备语句.
	 * 
	 * @param resultSetType ResultSet类型
	 */
	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	/**
	 * 设置是否使用能够返回可更新的ResultSet的预准备语句.
	 */
	public void setUpdatableResults(boolean updatableResults) {
		this.updatableResults = updatableResults;
	}

	/**
	 * 设置预准备语句是否应该能够返回自动生成的键.
	 */
	public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
		this.returnGeneratedKeys = returnGeneratedKeys;
	}

	/**
	 * 设置自动生成的键的列名称.
	 */
	public void setGeneratedKeysColumnNames(String... names) {
		this.generatedKeysColumnNames = names;
	}

	/**
	 * 指定NativeJdbcExtractor, 用于解包PreparedStatement.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}


	/**
	 * 为给定参数返回一个新的PreparedStatementSetter.
	 * 
	 * @param params 参数列表 (may be {@code null})
	 */
	public PreparedStatementSetter newPreparedStatementSetter(List<?> params) {
		return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
	}

	/**
	 * 为给定参数返回一个新的PreparedStatementSetter.
	 * 
	 * @param params 参数数组 (may be {@code null})
	 */
	public PreparedStatementSetter newPreparedStatementSetter(Object[] params) {
		return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
	}

	/**
	 * 为给定参数返回一个新的PreparedStatementCreator.
	 * 
	 * @param params 参数列表 (may be {@code null})
	 */
	public PreparedStatementCreator newPreparedStatementCreator(List<?> params) {
		return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
	}

	/**
	 * 为给定参数返回一个新的PreparedStatementCreator.
	 * 
	 * @param params 参数数组 (may be {@code null})
	 */
	public PreparedStatementCreator newPreparedStatementCreator(Object[] params) {
		return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
	}

	/**
	 * 为给定参数返回一个新的PreparedStatementCreator.
	 * 
	 * @param sqlToUse 要使用的实际SQL语句 (如果与工厂不同, 例如因为命名参数扩展)
	 * @param params 参数数组 (may be {@code null})
	 */
	public PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, Object[] params) {
		return new PreparedStatementCreatorImpl(
				sqlToUse, params != null ? Arrays.asList(params) : Collections.emptyList());
	}


	/**
	 * 此类返回的PreparedStatementCreator实现.
	 */
	private class PreparedStatementCreatorImpl
			implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider, ParameterDisposer {

		private final String actualSql;

		private final List<?> parameters;

		public PreparedStatementCreatorImpl(List<?> parameters) {
			this(sql, parameters);
		}

		public PreparedStatementCreatorImpl(String actualSql, List<?> parameters) {
			this.actualSql = actualSql;
			Assert.notNull(parameters, "Parameters List must not be null");
			this.parameters = parameters;
			if (this.parameters.size() != declaredParameters.size()) {
				// 多次使用命名参数的情况
				Set<String> names = new HashSet<String>();
				for (int i = 0; i < parameters.size(); i++) {
					Object param = parameters.get(i);
					if (param instanceof SqlParameterValue) {
						names.add(((SqlParameterValue) param).getName());
					}
					else {
						names.add("Parameter #" + i);
					}
				}
				if (names.size() != declaredParameters.size()) {
					throw new InvalidDataAccessApiUsageException(
							"SQL [" + sql + "]: given " + names.size() +
							" parameters but expected " + declaredParameters.size());
				}
			}
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement ps;
			if (generatedKeysColumnNames != null || returnGeneratedKeys) {
				if (generatedKeysColumnNames != null) {
					ps = con.prepareStatement(this.actualSql, generatedKeysColumnNames);
				}
				else {
					ps = con.prepareStatement(this.actualSql, PreparedStatement.RETURN_GENERATED_KEYS);
				}
			}
			else if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
				ps = con.prepareStatement(this.actualSql);
			}
			else {
				ps = con.prepareStatement(this.actualSql, resultSetType,
					updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
			}
			setValues(ps);
			return ps;
		}

		@Override
		public void setValues(PreparedStatement ps) throws SQLException {
			// 确定要传递给自定义类型的PreparedStatement.
			PreparedStatement psToUse = ps;
			if (nativeJdbcExtractor != null) {
				psToUse = nativeJdbcExtractor.getNativePreparedStatement(ps);
			}

			// 设置参数: 如果没有参数, 则不执行任何操作.
			int sqlColIndx = 1;
			for (int i = 0; i < this.parameters.size(); i++) {
				Object in = this.parameters.get(i);
				SqlParameter declaredParameter;
				// SqlParameterValue重写声明的参数元数据, 特别是在命名参数的情况下与声明的参数位置无关.
				if (in instanceof SqlParameterValue) {
					SqlParameterValue paramValue = (SqlParameterValue) in;
					in = paramValue.getValue();
					declaredParameter = paramValue;
				}
				else {
					if (declaredParameters.size() <= i) {
						throw new InvalidDataAccessApiUsageException(
								"SQL [" + sql + "]: unable to access parameter number " + (i + 1) +
								" given only " + declaredParameters.size() + " parameters");

					}
					declaredParameter = declaredParameters.get(i);
				}
				if (in instanceof Collection && declaredParameter.getSqlType() != Types.ARRAY) {
					Collection<?> entries = (Collection<?>) in;
					for (Object entry : entries) {
						if (entry instanceof Object[]) {
							Object[] valueArray = ((Object[])entry);
							for (Object argValue : valueArray) {
								StatementCreatorUtils.setParameterValue(psToUse, sqlColIndx++, declaredParameter, argValue);
							}
						}
						else {
							StatementCreatorUtils.setParameterValue(psToUse, sqlColIndx++, declaredParameter, entry);
						}
					}
				}
				else {
					StatementCreatorUtils.setParameterValue(psToUse, sqlColIndx++, declaredParameter, in);
				}
			}
		}

		@Override
		public String getSql() {
			return sql;
		}

		@Override
		public void cleanupParameters() {
			StatementCreatorUtils.cleanupParameters(this.parameters);
		}

		@Override
		public String toString() {
			return "PreparedStatementCreator: sql=[" + sql + "]; parameters=" + this.parameters;
		}
	}
}
