package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * 指定由提供表元数据的类实现的API的接口.
 * 这适用于Simple JDBC类的内部使用.
 */
public interface TableMetaDataProvider {

	/**
	 * 使用提供的数据库元数据进行初始化.
	 * 
	 * @param databaseMetaData 用于检索数据库特定信息
	 * 
	 * @throws SQLException 在初始化失败的情况下
	 */
	void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

	/**
	 * 使用提供的数据库元数据, 表和列信息进行初始化.
	 * 可以通过指定不应使用列元数据来关闭此初始化.
	 * 
	 * @param databaseMetaData 用于检索数据库特定信息
	 * @param catalogName 要使用的catalog的名称 (或{@code null})
	 * @param schemaName 要使用的schema的名称 (或{@code null})
	 * @param tableName 表名
	 * 
	 * @throws SQLException 在初始化失败的情况下
	 */
	void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, String catalogName,
			String schemaName, String tableName) throws SQLException;

	/**
	 * 获取基于元数据信息格式化的表名.
	 * 这可能包括更改的情况.
	 */
	String tableNameToUse(String tableName);

	/**
	 * 获取基于元数据信息格式化的catalog名称.
	 * 这可能包括更改的情况.
	 */
	String catalogNameToUse(String catalogName);

	/**
	 * 获取基于元数据信息格式化的schema名称.
	 * 这可能包括更改的情况.
	 */
	String schemaNameToUse(String schemaName);

	/**
	 * 提供对传入的catalog名称的任何修改，以匹配当前使用的元数据.
	 * 返回的值将用于元数据查找.
	 * 这可能包括更改使用的情况或提供基本catalog, 如果没有提供.
	 */
	String metaDataCatalogNameToUse(String catalogName) ;

	/**
	 * 提供对传入的schema名称的任何修改，以匹配当前使用的元数据.
	 * 返回的值将用于元数据查找.
	 * 这可能包括更改使用的情况或提供基本schema, 如果没有提供.
	 */
	String metaDataSchemaNameToUse(String schemaName) ;

	/**
	 * 是否使用列的元数据?
	 */
 	boolean isTableColumnMetaDataUsed();

	/**
	 * 此数据库是否支持检索生成的键的JDBC 3.0功能:
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
 	boolean isGetGeneratedKeysSupported();

	/**
	 * 当不支持检索生成的键的JDBC 3.0功能时, 此数据库是否支持简单查询以检索生成的键?
	 */
 	boolean isGetGeneratedKeysSimulated();

	/**
	 * 获取简单查询以检索生成的键.
	 */
	String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName);

	/**
	 * 此数据库是否支持列名String数组以检索生成的键:
	 * {@link java.sql.Connection#createStruct(String, Object[])}?
	 */
 	boolean isGeneratedKeysColumnNameArraySupported();

	/**
	 * 获取当前使用的表参数元数据.
	 * 
	 * @return List of {@link TableParameterMetaData}
	 */
	List<TableParameterMetaData> getTableParameterMetaData();

	/**
	 * 设置{@link NativeJdbcExtractor}以用于在必要时检索本机连接
	 */
	void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor);

}
