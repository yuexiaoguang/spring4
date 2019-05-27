package org.springframework.jdbc.core.simple;

import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * 指定由{@link SimpleJdbcCall}实现的简单JDBC调用的API的接口.
 * 此接口通常不直接使用, 但提供了增强可测试性的选项, 因为它很容易被模拟或存根.
 */
public interface SimpleJdbcCallOperations {

	/**
	 * 指定要使用的过程名称 - 这意味着将调用存储过程.
	 * 
	 * @param procedureName 存储过程的名称
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations withProcedureName(String procedureName);

	/**
	 * 指定要使用的过程名称 - 这意味着将调用存储函数.
	 * 
	 * @param functionName 存储函数的名称
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations withFunctionName(String functionName);

	/**
	 * 指定包含存储过程的schema的名称(可选).
	 * 
	 * @param schemaName schema的名称
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations withSchemaName(String schemaName);

	/**
	 * 指定包含存储过程的catalog的名称(可选).
	 * <p>为了提供与Oracle DatabaseMetaData的一致性, 如果将过程声明为包的一部分, 则用于指定包名.
	 * 
	 * @param catalogName catalog或包名
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations withCatalogName(String catalogName);

	/**
	 * 表示过程的返回值应包含在返回的结果中.
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations withReturnValue();

	/**
	 * 如果需要, 指定一个或多个参数.
	 * 这些参数将补充从数据库元数据中检索的任何参数信息.
	 * <p>请注意, 只有声明为{@code SqlParameter}和{@code SqlInOutParameter}的参数才会用于提供输入值.
	 * 这与{@code StoredProcedure}类不同 - 为了向后兼容性 - 它允许为声明为{@code SqlOutParameter}的参数提供输入值.
	 * 
	 * @param sqlParameters 要使用的参数
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations declareParameters(SqlParameter... sqlParameters);

	/** 尚未使用 */
	SimpleJdbcCallOperations useInParameterNames(String... inParameterNames);

	/**
	 * 用于指定存储过程返回ResultSet的时间, 并希望它由{@link RowMapper}映射.
	 * 将使用指定的参数名称返回结果. 必须以正确的顺序声明多个ResultSet.
	 * <p>如果使用的数据库使用ref游标, 则指定的名称必须与为数据库中的过程声明的参数名称匹配.
	 * 
	 * @param parameterName 返回结果的名称和/或ref游标参数的名称
	 * @param rowMapper 将映射为每行返回的数据的RowMapper实现
	 */
	SimpleJdbcCallOperations returningResultSet(String parameterName, RowMapper<?> rowMapper);

	/**
	 * 关闭通过JDBC获得的参数元数据信息的任何处理.
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations withoutProcedureColumnMetaDataAccess();

	/**
	 * 表示参数应按名称绑定.
	 * 
	 * @return 这个SimpleJdbcCall的实例
	 */
	SimpleJdbcCallOperations withNamedBinding();


	/**
	 * 执行存储函数, 并返回获取的结果.
	 * 
	 * @param returnType 要返回的值的类型
	 * @param args 包含要在调用中使用的in参数值的可选数组.
	 * 必须以与为存储过程定义的参数相同的顺序提供参数值.
	 */
	<T> T executeFunction(Class<T> returnType, Object... args);

	/**
	 * 执行存储函数, 并返回获取的结果.
	 * 
	 * @param returnType 要返回的值的类型
	 * @param args 包含要在调用中使用的参数值的Map
	 */
	<T> T executeFunction(Class<T> returnType, Map<String, ?> args);

	/**
	 * 执行存储函数, 并返回获取的结果.
	 * 
	 * @param returnType 要返回的值的类型
	 * @param args 包含要在调用中使用的参数值的MapSqlParameterSource
	 */
	<T> T executeFunction(Class<T> returnType, SqlParameterSource args);

	/**
	 * 执行存储过程, 并返回单个out参数.
	 * 在存在多个输出参数的情况下, 返回第一个参数并忽略其他输出参数.
	 * 
	 * @param returnType 要返回的值的类型
	 * @param args 包含要在调用中使用的in参数值的可选数组.
	 * 必须以与为存储过程定义的参数相同的顺序提供参数值.
	 */
	<T> T executeObject(Class<T> returnType, Object... args);

	/**
	 * 执行存储过程, 并返回单个out参数.
	 * 在存在多个输出参数的情况下, 返回第一个参数并忽略其他输出参数.
	 * 
	 * @param returnType 要返回的值的类型
	 * @param args 包含要在调用中使用的参数值的Map
	 */
	<T> T executeObject(Class<T> returnType, Map<String, ?> args);

	/**
	 * 执行存储过程, 并返回单个out参数.
	 * 在存在多个输出参数的情况下, 返回第一个参数并忽略其他输出参数.
	 * 
	 * @param returnType 要返回的值的类型
	 * @param args 包含要在调用中使用的参数值的MapSqlParameterSource
	 */
	<T> T executeObject(Class<T> returnType, SqlParameterSource args);

	/**
	 * 执行存储过程, 并返回输出参数的Map, 将参数声明中的名称作为键.
	 * 
	 * @param args 包含要在调用中使用的in参数值的可选数组.
	 * 必须以与为存储过程定义的参数相同的顺序提供参数值.
	 * 
	 * @return 输出参数的Map
	 */
	Map<String, Object> execute(Object... args);

	/**
	 * 执行存储过程, 并返回输出参数的Map, 将参数声明中的名称作为键.
	 * 
	 * @param args 包含要在调用中使用的参数值的Map
	 * 
	 * @return 输出参数的Map
	 */
	Map<String, Object> execute(Map<String, ?> args);

	/**
	 * 执行存储过程, 并返回输出参数的Map, 将参数声明中的名称作为键.
	 * 
	 * @param args 包含要在调用中使用的参数值的SqlParameterSource
	 * 
	 * @return 输出参数的Map
	 */
	Map<String, Object> execute(SqlParameterSource args);

}
