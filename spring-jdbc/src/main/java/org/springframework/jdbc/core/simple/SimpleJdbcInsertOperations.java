package org.springframework.jdbc.core.simple;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * 指定由{@link SimpleJdbcInsert}实现的简单JDBC插入的API的接口.
 * 此接口通常不直接使用, 但提供了增强可测试性的选项, 因为它很容易被模拟或存根.
 */
public interface SimpleJdbcInsertOperations {

	/**
	 * 指定要插入的表名.
	 * 
	 * @param tableName 表名
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations withTableName(String tableName);

	/**
	 * 指定要用于插入的schema名称.
	 * 
	 * @param schemaName schema名称
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations withSchemaName(String schemaName);

	/**
	 * 指定要用于插入的catalog名称.
	 * 
	 * @param catalogName catalog名称
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations withCatalogName(String catalogName);

	/**
	 * 指定insert语句应限制使用的列名.
	 * 
	 * @param columnNames 一个或多个列名
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations usingColumns(String... columnNames);

	/**
	 * 指定具有自动生成键的列的名称.
	 * 
	 * @param columnNames 一个或多个列名
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations usingGeneratedKeyColumns(String... columnNames);

	/**
	 * 关闭通过JDBC获得的列元数据信息的任何处理.
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations withoutTableColumnMetaDataAccess();

	/**
	 * 包含通过JDBC查找的列元数据的同义词.
	 * <p>Note: 这仅适用于Oracle, 因为支持同义词的其他数据库似乎自动包含同义词.
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations includeSynonymsForTableColumnMetaData();

	/**
	 * 在通过JDBC查找列元数据期间, 使用提供的NativeJdbcExtractor.
	 * <p>Note: 只有在使用包装元数据连接的连接池运行时, 以及使用Oracle等数据库时, 才需要包含此选项, Oracle需要访问本机连接以包含同义词.
	 * 
	 * @return 这个SimpleJdbcInsert的实例
	 */
	SimpleJdbcInsertOperations useNativeJdbcExtractorForMetaData(NativeJdbcExtractor nativeJdbcExtractor);


	/**
	 * 使用传入的值执行插入.
	 * 
	 * @param args 包含列名和相应值的Map
	 * 
	 * @return JDBC驱动程序返回的受影响的行数
	 */
	int execute(Map<String, ?> args);

	/**
	 * 使用传入的值执行插入.
	 * 
	 * @param parameterSource 包含要用于插入的值的SqlParameterSource
	 * 
	 * @return JDBC驱动程序返回的受影响的行数
	 */
	int execute(SqlParameterSource parameterSource);

	/**
	 * 使用传入的值执行插入, 并返回生成的键.
	 * <p>这要求已指定具有自动生成的键的列的名称.
	 * 此方法将始终返回KeyHolder, 但调用者必须验证它实际上是否包含生成的键.
	 * 
	 * @param args 包含列名和相应值的Map
	 * 
	 * @return 生成的键值
	 */
	Number executeAndReturnKey(Map<String, ?> args);

	/**
	 * 使用传入的值执行插入, 并返回生成的键.
	 * <p>这要求已指定具有自动生成的键的列的名称.
	 * 此方法将始终返回KeyHolder, 但调用者必须验证它实际上是否包含生成的键.
	 * 
	 * @param parameterSource 包含要用于插入的值的SqlParameterSource
	 * 
	 * @return 生成的键值
	 */
	Number executeAndReturnKey(SqlParameterSource parameterSource);

	/**
	 * 使用传入的值执行插入, 并返回生成的键.
	 * <p>这要求已指定具有自动生成的键的列的名称.
	 * 此方法将始终返回KeyHolder, 但调用者必须验证它实际上是否包含生成的键.
	 * 
	 * @param args 包含列名和相应值的Map
	 * 
	 * @return 包含所有生成的键的KeyHolder
	 */
	KeyHolder executeAndReturnKeyHolder(Map<String, ?> args);

	/**
	 * 使用传入的值执行插入, 并返回生成的键.
	 * <p>这要求已指定具有自动生成的键的列的名称.
	 * 此方法将始终返回KeyHolder, 但调用者必须验证它实际上是否包含生成的键.
	 * 
	 * @param parameterSource 包含要用于插入的值的SqlParameterSource
	 * 
	 * @return 包含所有生成的键的KeyHolder
	 */
	KeyHolder executeAndReturnKeyHolder(SqlParameterSource parameterSource);

	/**
	 * 使用传入的一批值执行批量插入.
	 * 
	 * @param batch 包含一批列名和相应值的Map数组
	 * 
	 * @return JDBC驱动程序返回的受影响的行数数组
	 */
	@SuppressWarnings("unchecked")
	int[] executeBatch(Map<String, ?>... batch);

	/**
	 * 使用传入的一批值执行批量插入.
	 * 
	 * @param batch 包含批处理值的SqlParameterSource数组
	 * 
	 * @return JDBC驱动程序返回的受影响的行数数组
	 */
	int[] executeBatch(SqlParameterSource... batch);

}
