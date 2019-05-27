package org.springframework.jdbc.support;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * JdbcUtils类使用的回调接口.
 * 此接口的实现执行提取数据库元数据的实际工作, 但不需要担心异常处理.
 * JdbcUtils类将正确捕获和处理SQLException.
 */
public interface DatabaseMetaDataCallback {

	/**
	 * 实现必须实现此方法来处理传入的元数据. 实现选择要做的完全取决于它.
	 * 
	 * @param dbmd 要处理的DatabaseMetaData
	 * 
	 * @return 从元数据中提取的结果对象 (可以是实现所需的任意对象)
	 * @throws SQLException 如果获取列值时遇到SQLException (也就是说, 不需要捕获SQLException)
	 * @throws MetaDataAccessException 在提取元数据时出现其他故障 (例如, 反射失败)
	 */
	Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException;

}
