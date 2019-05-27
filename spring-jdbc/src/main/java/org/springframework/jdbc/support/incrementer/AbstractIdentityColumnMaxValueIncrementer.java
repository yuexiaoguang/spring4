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
 * {@link DataFieldMaxValueIncrementer}实现的抽象基类, 它基于类序列表中的标识列.
 */
public abstract class AbstractIdentityColumnMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	private boolean deleteSpecificValues = false;

	/** 当前值的缓存 */
	private long[] valueCache;

	/** 从值缓存中提供的下一个ID */
	private int nextValueIndex = -1;


	public AbstractIdentityColumnMaxValueIncrementer() {
	}

	public AbstractIdentityColumnMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	/**
	 * 指定是否删除当前最大键值以下的整个范围 (默认{@code false}), 或特定生成的值 ({@code true}).
	 * 前一种模式将使用where range子句, 而后者将使用以最低值减1开头的in子句, 只保留最大值.
	 */
	public void setDeleteSpecificValues(boolean deleteSpecificValues) {
		this.deleteSpecificValues = deleteSpecificValues;
	}

	/**
	 * 返回是否删除当前最大键值以下的整个范围 (默认{@code false}), 或特定生成的值 ({@code true}).
	 */
	public boolean isDeleteSpecificValues() {
		return this.deleteSpecificValues;
	}


	@Override
	protected synchronized long getNextKey() throws DataAccessException {
		if (this.nextValueIndex < 0 || this.nextValueIndex >= getCacheSize()) {
			/*
			* 需要使用直接JDBC代码, 因为需要确保在同一连接上执行insert和select
			* (否则无法确定 @@identity 是否返回正确的值)
			*/
			Connection con = DataSourceUtils.getConnection(getDataSource());
			Statement stmt = null;
			try {
				stmt = con.createStatement();
				DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
				this.valueCache = new long[getCacheSize()];
				this.nextValueIndex = 0;
				for (int i = 0; i < getCacheSize(); i++) {
					stmt.executeUpdate(getIncrementStatement());
					ResultSet rs = stmt.executeQuery(getIdentityStatement());
					try {
						if (!rs.next()) {
							throw new DataAccessResourceFailureException("Identity statement failed after inserting");
						}
						this.valueCache[i] = rs.getLong(1);
					}
					finally {
						JdbcUtils.closeResultSet(rs);
					}
				}
				stmt.executeUpdate(getDeleteStatement(this.valueCache));
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not increment identity", ex);
			}
			finally {
				JdbcUtils.closeStatement(stmt);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
		return this.valueCache[this.nextValueIndex++];
	}


	/**
	 * 用于递增"sequence"值的语句.
	 * 
	 * @return 要使用的SQL语句
	 */
	protected abstract String getIncrementStatement();

	/**
	 * 用于获取当前标识值的语句.
	 * 
	 * @return 要使用的SQL语句
	 */
	protected abstract String getIdentityStatement();

	/**
	 * 用于清除"sequence"值的语句.
	 * <p>默认实现删除当前最大值以下的整个范围, 或特定生成的值 (从最低减1开始, 只保留最大值)
	 * - 根据{@link #isDeleteSpecificValues()}的设置.
	 * 
	 * @param values 当前生成的键值 (对应于{@link #getCacheSize()}的值的数量)
	 * 
	 * @return 要使用的SQL语句
	 */
	protected String getDeleteStatement(long[] values) {
		StringBuilder sb = new StringBuilder(64);
		sb.append("delete from ").append(getIncrementerName()).append(" where ").append(getColumnName());
		if (isDeleteSpecificValues()) {
			sb.append(" in (").append(values[0] - 1);
			for (int i = 0; i < values.length - 1; i++) {
				sb.append(", ").append(values[i]);
			}
			sb.append(")");
		}
		else {
			long maxValue = values[values.length - 1];
			sb.append(" < ").append(maxValue);
		}
		return sb.toString();
	}
}
