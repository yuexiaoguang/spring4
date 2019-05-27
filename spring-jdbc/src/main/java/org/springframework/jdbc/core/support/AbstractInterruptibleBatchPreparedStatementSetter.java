package org.springframework.jdbc.core.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.InterruptibleBatchPreparedStatementSetter;

/**
 * {@link InterruptibleBatchPreparedStatementSetter}接口的抽象实现,
 * 将检查可用值, 并将它们设置为单个回调方法{@link #setValuesIfAvailable}.
 */
public abstract class AbstractInterruptibleBatchPreparedStatementSetter
		implements InterruptibleBatchPreparedStatementSetter {

	private boolean exhausted;


	/**
	 * 此实现调用{@link #setValuesIfAvailable}, 并相应地设置此实例的耗尽标志.
	 */
	@Override
	public final void setValues(PreparedStatement ps, int i) throws SQLException {
		this.exhausted = !setValuesIfAvailable(ps, i);
	}

	/**
	 * 此实现返回此实例的当前耗尽标志.
	 */
	@Override
	public final boolean isBatchExhausted(int i) {
		return this.exhausted;
	}

	/**
	 * 此实现返回{@code Integer.MAX_VALUE}.
	 * 可以在子类中重写以降低最大批量大小.
	 */
	@Override
	public int getBatchSize() {
		return Integer.MAX_VALUE;
	}


	/**
	 * 检查可用值, 并在给定的PreparedStatement上设置它们.
	 * 如果没有值可用, 请返回{@code false}.
	 * 
	 * @param ps 将调用setter方法的PreparedStatement
	 * @param i 在批处理中发出的语句的索引, 从0开始
	 * 
	 * @return 是否存在要应用的值 (即, 是否应将应用的参数添加到批处理中, 并且应该调用此方法以进行进一步的迭代)
	 * @throws SQLException 如果遇到SQLException (i.e. 不需要捕获SQLException)
	 */
	protected abstract boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException;

}
