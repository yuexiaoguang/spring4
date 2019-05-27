package org.springframework.jdbc.support.incrementer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * 使用等效的自动增量列递增给定MySQL表的最大值.
 * Note: 如果使用这个类, MySQL键列<i>不应该</i>自动递增, 因为序列表完成了这项工作.
 *
 * <p>序列保存在表中; 每个表应该有一个需要自动生成键的序列表.
 * 序列表使用的存储引擎可以是MYISAM或INNODB, 因为序列是使用单独的连接分配的, 不受可能正在进行的任何其他事务的影响.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int unsigned not null primary key, text varchar(100));
 * create table tab_sequence (value int not null);
 * insert into tab_sequence values(0);</pre>
 *
 * 如果设置了"cacheSize", 则在不查询数据库的情况下提供中间值.
 * 如果服务器或应用程序停止, 或崩溃, 或回滚事务, 则永远不会提供未使用的值.
 * 编号中的最大hole大小因此是cacheSize的值.
 *
 * <p>通过将"useNewConnection"属性设置为false, 可以避免为增量器获取新连接.
 * 在这种情况下, 在定义增量表时, <i>必须</i>使用非事务性存储引擎, 如MYISAM.
 */
public class MySQLMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	/** 用于检索新序列值的SQL字符串 */
	private static final String VALUE_SQL = "select last_insert_id()";

	/** 下一个要服务的ID */
	private long nextId = 0;

	/** 要服务的最大ID */
	private long maxId = 0;

	/** 是否为增量器使用新连接 */
	private boolean useNewConnection = false;


	public MySQLMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列表的名称
	 * @param columnName 要使用的序列表中的列的名称
	 */
	public MySQLMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	/**
	 * 设置是否为增量器使用新连接.
	 * <p>{@code true}是支持事务存储引擎所必需的, 使用隔离的单独事务进行增量操作.
	 * 如果序列表的存储引擎是非事务性的(如 MYISAM), 则{@code false}就足够了, 从而避免为增量操作获取额外的{@code Connection}.
	 * <p>Spring Framework 4.3.x行中的默认值为{@code false}.
	 */
	public void setUseNewConnection(boolean useNewConnection) {
		this.useNewConnection = useNewConnection;
	}


	@Override
	protected synchronized long getNextKey() throws DataAccessException {
		if (this.maxId == this.nextId) {
			/*
			* 如果useNewConnection为 true, 则获得非托管的连接, 因此修改将在单独的事务中处理.
			* 如果是false, 那么使用当前事务的连接, 依赖于使用像MYISAM这样的非事务性存储引擎, 用于增量表.
			* 还使用直接JDBC代码, 因为需要确保在同一连接上执行insert和select (否则无法确定 last_insert_id()是否返回了正确的值).
			*/
			Connection con = null;
			Statement stmt = null;
			boolean mustRestoreAutoCommit = false;
			try {
				if (this.useNewConnection) {
					con = getDataSource().getConnection();
					if (con.getAutoCommit()) {
						mustRestoreAutoCommit = true;
						con.setAutoCommit(false);
					}
				}
				else {
					con = DataSourceUtils.getConnection(getDataSource());
				}
				stmt = con.createStatement();
				if (!this.useNewConnection) {
					DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
				}
				// 增加序列列...
				String columnName = getColumnName();
				try {
					stmt.executeUpdate("update " + getIncrementerName() + " set " + columnName +
							" = last_insert_id(" + columnName + " + " + getCacheSize() + ")");
				}
				catch (SQLException ex) {
					throw new DataAccessResourceFailureException("Could not increment " + columnName + " for " +
							getIncrementerName() + " sequence table", ex);
				}
				// 检索序列列的新最大值...
				ResultSet rs = stmt.executeQuery(VALUE_SQL);
				try {
					if (!rs.next()) {
						throw new DataAccessResourceFailureException("last_insert_id() failed after executing an update");
					}
					this.maxId = rs.getLong(1);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
				}
				this.nextId = this.maxId - getCacheSize() + 1;
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
			}
			finally {
				JdbcUtils.closeStatement(stmt);
				if (con != null) {
					if (this.useNewConnection) {
						try {
							con.commit();
							if (mustRestoreAutoCommit) {
								con.setAutoCommit(true);
							}
						}
						catch (SQLException ignore) {
							throw new DataAccessResourceFailureException(
									"Unable to commit new sequence value changes for " + getIncrementerName());
						}
						JdbcUtils.closeConnection(con);
					}
					else {
						DataSourceUtils.releaseConnection(con, getDataSource());
					}
				}
			}
		}
		else {
			this.nextId++;
		}
		return this.nextId;
	}

}
