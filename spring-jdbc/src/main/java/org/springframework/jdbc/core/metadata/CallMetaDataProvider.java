package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.SqlParameter;

/**
 * 指定由提供调用元数据的类实现的API的接口.
 *
 * <p>供Spring的 {@link org.springframework.jdbc.core.simple.SimpleJdbcCall}内部使用.
 */
public interface CallMetaDataProvider {

	/**
	 * 使用提供的DatabaseMetData进行初始化.
	 * 
	 * @param databaseMetaData 用于检索数据库特定信息
	 * 
	 * @throws SQLException 在初始化失败的情况下
	 */
	void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

	/**
	 * 初始化过程列元数据的数据库特定管理.
	 * 仅针对受支持的数据库调用此方法. 可以通过指定不应使用列元数据来关闭此初始化.
	 * 
	 * @param databaseMetaData 用于检索数据库特定信息
	 * @param catalogName 要使用的catalog的名称 (或{@code null})
	 * @param schemaName 要使用的schema的名称 (或{@code null})
	 * @param procedureName 存储过程的名称
	 * 
	 * @throws SQLException 在初始化失败的情况下
	 */
	void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData, String catalogName,
			String schemaName, String procedureName) throws SQLException;

	/**
	 * 提供对传入的过程名称的任何修改, 以匹配当前使用的元数据.
	 * 这可能包括改变情况.
	 */
	String procedureNameToUse(String procedureName);

	/**
	 * 提供对传入的catalog名称的任何修改, 以匹配当前使用的元数据.
	 * 这可能包括改变情况.
	 */
	String catalogNameToUse(String catalogName);

	/**
	 * 提供对传入的schema名称的任何修改, 以匹配当前使用的元数据.
	 * 这可能包括改变情况.
	 */
	String schemaNameToUse(String schemaName);

	/**
	 * 提供对传入的catalog名称的任何修改, 以匹配当前使用的元数据.
	 * 返回的值将用于元数据查找.
	 * 这可能包括更改使用的情况或提供基本catalog, 如果没有提供.
	 */
	String metaDataCatalogNameToUse(String catalogName);

	/**
	 * 提供对传入的schema名称的任何修改, 以匹配当前使用的元数据.
	 * 返回的值将用于元数据查找.
	 * 这可能包括更改使用的情况或提供基本schema, 如果没有提供.
	 */
	String metaDataSchemaNameToUse(String schemaName);

	/**
	 * 提供对传入的列名称的任何修改, 以匹配当前使用的元数据.
	 * 这可能包括更改情况.
	 * 
	 * @param parameterName 列的参数名称
	 */
	String parameterNameToUse(String parameterName);

	/**
	 * 根据提供的元数据创建默认输出参数.
	 * 在未进行显式参数声明时使用.
	 * 
	 * @param parameterName 参数的名称
	 * @param meta 用于此次调用的元数据
	 * 
	 * @return 配置的SqlOutParameter
	 */
	SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * 根据提供的元数据创建默认的inout参数.
	 * 在未进行显式参数声明时使用.
	 * 
	 * @param parameterName 参数的名称
	 * @param meta 用于此次调用的元数据
	 * 
	 * @return 配置的SqlInOutParameter
	 */
	SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * 根据提供的元数据在参数中创建默认值.
	 * 在未进行显式参数声明时使用.
	 * 
	 * @param parameterName 参数的名称
	 * @param meta 用于此次调用的元数据
	 * 
	 * @return 配置的SqlParameter
	 */
	SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta);

	/**
	 * 获取当前用户的名称. 对元数据查找等有用.
	 * 
	 * @return 数据库连接的当前用户名
	 */
	String getUserName();

	/**
	 * 此数据库是否支持返回应使用JDBC调用检索的ResultSet:
	 * {@link java.sql.Statement#getResultSet()}?
	 */
	boolean isReturnResultSetSupported();

	/**
	 * 此数据库是否支持将ResultSets作为ref游标返回, 以便使用{@link java.sql.CallableStatement#getObject(int)}检索指定列.
	 */
	boolean isRefCursorSupported();

	/**
	 * 如果支持此功能, 获取返回ResultSets作为ref游标的列的{@link java.sql.Types}类型.
	 */
	int getRefCursorSqlType();

	/**
	 * 是否使用过程列的元数据?
	 */
	boolean isProcedureColumnMetaDataUsed();

	/**
	 * 应该绕过具有指定名称的return参数.
	 * 这允许数据库特定实现跳过数据库调用返回的特定结果的处理.
	 */
	boolean byPassReturnParameter(String parameterName);

	/**
	 * 获取当前使用的调用参数元数据.
	 * 
	 * @return {@link CallParameterMetaData}列表
	 */
	List<CallParameterMetaData> getCallParameterMetaData();

	/**
	 * 数据库是否支持在过程调用中使用catalog名称?
	 */
	boolean isSupportsCatalogsInProcedureCalls();

	/**
	 * 数据库是否支持在过程调用中使用schema名称?
	 */
	boolean isSupportsSchemasInProcedureCalls();

}
