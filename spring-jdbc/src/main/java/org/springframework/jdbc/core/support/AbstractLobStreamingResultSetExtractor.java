package org.springframework.jdbc.core.support;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.LobRetrievalFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * 抽象ResultSetExtractor实现, 假定流式传输LOB数据.
 * 通常用作内部类, 可以访问周围的方法参数.
 *
 * <p>委托给{@code streamData}模板方法, 将LOB内容流式传输到某些OutputStream, 通常使用LobHandler.
 * 将流式传输期间抛出的IOException转换为LobRetrievalFailureException.
 *
 * <p>JdbcTemplate的用法示例:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * final LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.query(
 *		 "SELECT content FROM imagedb WHERE image_name=?", new Object[] {name},
 *		 new AbstractLobStreamingResultSetExtractor() {
 *			 public void streamData(ResultSet rs) throws SQLException, IOException {
 *				 FileCopyUtils.copy(lobHandler.getBlobAsBinaryStream(rs, 1), contentStream);
 *             }
 *         }
 * );</pre>
 */
public abstract class AbstractLobStreamingResultSetExtractor<T> implements ResultSetExtractor<T> {

	/**
	 * 根据ResultSet状态, 委托给 handleNoRowFound, handleMultipleRowsFound 和 streamData.
	 * 将streamData抛出的IOException转换为LobRetrievalFailureException.
	 */
	@Override
	public final T extractData(ResultSet rs) throws SQLException, DataAccessException {
		if (!rs.next()) {
			handleNoRowFound();
		}
		else {
			try {
				streamData(rs);
				if (rs.next()) {
					handleMultipleRowsFound();
				}
			}
			catch (IOException ex) {
				throw new LobRetrievalFailureException("Couldn't stream LOB content", ex);
			}
		}
		return null;
	}

	/**
	 * 处理ResultSet不包含行的情况.
	 * 
	 * @throws DataAccessException 相应的异常, 默认情况下为 EmptyResultDataAccessException
	 */
	protected void handleNoRowFound() throws DataAccessException {
		throw new EmptyResultDataAccessException(
				"LobStreamingResultSetExtractor did not find row in database", 1);
	}

	/**
	 * 处理ResultSet包含多行的情况.
	 * 
	 * @throws DataAccessException 相应的异常, 默认情况下为IncorrectResultSizeDataAccessException
	 */
	protected void handleMultipleRowsFound() throws DataAccessException {
		throw new IncorrectResultSizeDataAccessException(
				"LobStreamingResultSetExtractor found multiple rows in database", 1);
	}

	/**
	 * 将给定ResultSet中的LOB内容流式传输到某个OutputStream.
	 * <p>通常用作内部类, 可以访问外围的方法参数, 和外围类的LobHandler实例变量.
	 * 
	 * @param rs 从中获取LOB内容的ResultSet
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 * @throws IOException 如果由流访问方法抛出
	 * @throws DataAccessException 如果是自定义异常
	 */
	protected abstract void streamData(ResultSet rs) throws SQLException, IOException, DataAccessException;

}
