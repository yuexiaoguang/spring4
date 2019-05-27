package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 用于填充, 初始化或清理数据库的策略.
 */
public interface DatabasePopulator {

	/**
	 * 使用提供的JDBC连接填充, 初始化或清理数据库.
	 * <p>如果遇到错误但<em>强烈鼓励</em>抛出特定的{@link ScriptException}, 具体实现<em>可能会</em>抛出{@link SQLException}.
	 * 例如, Spring的{@link ResourceDatabasePopulator}和{@link DatabasePopulatorUtils}
	 * 将所有{@code SQLExceptions}包装在{@code ScriptExceptions}中.
	 * 
	 * @param connection 用于填充数据库的JDBC连接; 已配置好并可以使用; never {@code null}
	 * 
	 * @throws SQLException 如果在数据库填充期间发生不可恢复的数据访问异常
	 * @throws ScriptException 在所有其他错误情况下
	 */
	void populate(Connection connection) throws SQLException, ScriptException;

}
