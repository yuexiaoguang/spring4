package org.springframework.jdbc.object;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

/**
 * 执行批量更新操作的SqlUpdate子类.
 * 封装对要更新的记录进行排队, 并在调用{@code flush}或满足给定批量大小后将它们作为单个批次添加.
 *
 * <p>请注意, 此类是 <b>非线程安全对象</b>, 与此包中的所有其他JDBC操作对象形成对比.
 * 需要为每次使用创建一个新实例, 或者在同一个线程中重用之前调用{@code reset}.
 */
public class BatchSqlUpdate extends SqlUpdate {

	/**
	 * 提交批次之前要累积的默认插入数量 (5000).
	 */
	public static final int DEFAULT_BATCH_SIZE = 5000;


	private int batchSize = DEFAULT_BATCH_SIZE;

	private boolean trackRowsAffected = true;

	private final LinkedList<Object[]> parameterQueue = new LinkedList<Object[]>();

	private final List<Integer> rowsAffected = new ArrayList<Integer>();


	/**
	 * 允许用作JavaBean的构造方法. 在编译和使用之前必须提供DataSource和SQL.
	 */
	public BatchSqlUpdate() {
		super();
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要执行的SQL语句
	 */
	public BatchSqlUpdate(DataSource ds, String sql) {
		super(ds, sql);
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要执行的SQL语句
	 * @param types 参数的SQL类型, 如{@code java.sql.Types}类中所定义
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types) {
		super(ds, sql, types);
	}

	/**
	 * @param ds 用于获取连接的DataSource
	 * @param sql 要执行的SQL语句
	 * @param types 参数的SQL类型, 如{@code java.sql.Types}类中所定义
	 * @param batchSize 将触发自动中间刷新的语句数
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types, int batchSize) {
		super(ds, sql, types);
		setBatchSize(batchSize);
	}


	/**
	 * 设置将触发自动中间刷新的语句数.
	 * {@code update}调用或给定的语句参数将排队, 直到满足批量大小, 此时它将清空队列并执行批处理.
	 * <p>还可以使用显式{@code flush}调用刷新已排队的语句.
	 * 请注意, 在对所有参数进行排队后, 需要这样做, 以保证所有语句都已刷新.
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * 设置是否跟踪受此操作对象执行的批量更新影响的行.
	 * <p>默认"true". 将其关闭以保存行计数列表所需的内存.
	 */
	public void setTrackRowsAffected(boolean trackRowsAffected) {
		this.trackRowsAffected = trackRowsAffected;
	}

	/**
	 * BatchSqlUpdate不支持BLOB或CLOB参数.
	 */
	@Override
	protected boolean supportsLobParameters() {
		return false;
	}


	/**
	 * {@code update}的重写版本, 它将给定的语句参数添加到队列中, 而不是立即执行它们.
	 * SqlUpdate基类的所有其他{@code update}方法都通过此方法, 因此行为类似.
	 * <p>需要调用{@code flush}来实际执行批处理.
	 * 如果达到指定的批处理大小, 则会发生隐式刷新; 仍然需要最终调用{@code flush}来刷新所有语句.
	 * 
	 * @param params 参数对象数组
	 * 
	 * @return 更新影响的行数 (始终为 -1, 表示 "不适用", 因为此方法实际上不执行该语句)
	 */
	@Override
	public int update(Object... params) throws DataAccessException {
		validateParameters(params);
		this.parameterQueue.add(params.clone());

		if (this.parameterQueue.size() == this.batchSize) {
			if (logger.isDebugEnabled()) {
				logger.debug("Triggering auto-flush because queue reached batch size of " + this.batchSize);
			}
			flush();
		}

		return -1;
	}

	/**
	 * 触发任何排队的更新操作作为最终批次添加.
	 * 
	 * @return 每个语句影响的行数的数组
	 */
	public int[] flush() {
		if (this.parameterQueue.isEmpty()) {
			return new int[0];
		}

		int[] rowsAffected = getJdbcTemplate().batchUpdate(
				getSql(),
				new BatchPreparedStatementSetter() {
					@Override
					public int getBatchSize() {
						return parameterQueue.size();
					}
					@Override
					public void setValues(PreparedStatement ps, int index) throws SQLException {
						Object[] params = parameterQueue.removeFirst();
						newPreparedStatementSetter(params).setValues(ps);
					}
				});

		for (int rowCount : rowsAffected) {
			checkRowsAffected(rowCount);
			if (this.trackRowsAffected) {
				this.rowsAffected.add(rowCount);
			}
		}

		return rowsAffected;
	}

	/**
	 * 返回队列中语句或语句参数的当前数量.
	 */
	public int getQueueCount() {
		return this.parameterQueue.size();
	}

	/**
	 * 返回已执行语句的数量.
	 */
	public int getExecutionCount() {
		return this.rowsAffected.size();
	}

	/**
	 * 返回所有已执行语句的受影响行数.
	 * 累积所有{@code flush}的返回值, 直到调用{@code reset}.
	 * 
	 * @return 每个语句影响的行数的数组
	 */
	public int[] getRowsAffected() {
		int[] result = new int[this.rowsAffected.size()];
		int i = 0;
		for (Iterator<Integer> it = this.rowsAffected.iterator(); it.hasNext(); i++) {
			result[i] = it.next();
		}
		return result;
	}

	/**
	 * 重置语句参数队列, 受影响的行缓存和执行计数.
	 */
	public void reset() {
		this.parameterQueue.clear();
		this.rowsAffected.clear();
	}

}
