package org.springframework.jdbc.core.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;

/**
 * 管理{@link LobCreator}的抽象{@link PreparedStatementCallback}实现.
 * 通常用作内部类, 可以访问周围的方法参数.
 *
 * <p>委托给{@code setValues}模板方法, 用于在PreparedStatement上设置值, 使用给定的LobCreator获取 BLOB/CLOB参数.
 *
 * <p>使用{@link org.springframework.jdbc.core.JdbcTemplate}的用法示例:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.execute(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new AbstractLobCreatingPreparedStatementCallback(lobHandler) {
 *       protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException {
 *         ps.setString(1, name);
 *         lobCreator.setBlobAsBinaryStream(ps, 2, contentStream, contentLength);
 *         lobCreator.setClobAsString(ps, 3, description);
 *       }
 *     }
 * );</pre>
 */
public abstract class AbstractLobCreatingPreparedStatementCallback implements PreparedStatementCallback<Integer> {

	private final LobHandler lobHandler;


	/**
	 * @param lobHandler 用于创建LobCreator的LobHandler
	 */
	public AbstractLobCreatingPreparedStatementCallback(LobHandler lobHandler) {
		Assert.notNull(lobHandler, "LobHandler must not be null");
		this.lobHandler = lobHandler;
	}


	@Override
	public final Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
		LobCreator lobCreator = this.lobHandler.getLobCreator();
		try {
			setValues(ps, lobCreator);
			return ps.executeUpdate();
		}
		finally {
			lobCreator.close();
		}
	}

	/**
	 * 给定的PreparedStatement上设置值, 使用给定的LobCreator用于BLOB/CLOB参数.
	 * 
	 * @param ps 要使用的PreparedStatement
	 * @param lobCreator 要使用的LobCreator
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 * @throws DataAccessException 如果是自定义异常
	 */
	protected abstract void setValues(PreparedStatement ps, LobCreator lobCreator)
			throws SQLException, DataAccessException;

}
