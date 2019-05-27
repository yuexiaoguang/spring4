package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * 当需要根据连接自定义参数时, 实现此接口.
 * 可能需要这样做才能使用专有功能, 仅适用于特定的连接类型.
 */
public interface ParameterMapper {

	/**
	 * 创建一个输入参数的Map, 名称作为键.
	 * 
	 * @param con JDBC连接.
	 * 如果需要使用专有的Connection实现类来执行特定于RDBMS的操作, 那么这很有用 (以及此接口的用途).
	 * 这个类隐藏了这些专有细节. 但是, 如果可能, 最好避免使用此类专有RDBMS功能.
	 * 
	 * @throws SQLException 如果遇到设置参数值的SQLException (也就是说, 不需要捕获SQLException)
	 * @return 输入参数的Map, 名称作为键 (never {@code null})
	 */
	Map<String, ?> createMap(Connection con) throws SQLException;

}
