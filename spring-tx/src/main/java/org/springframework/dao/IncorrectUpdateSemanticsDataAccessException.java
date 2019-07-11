package org.springframework.dao;

/**
 * 当更新发生意外事件时抛出的数据访问异常, 但事务尚未回滚.
 * 例如, 当想要在RDBMS中更新1行但实际更新3时, 抛出.
 */
@SuppressWarnings("serial")
public class IncorrectUpdateSemanticsDataAccessException extends InvalidDataAccessResourceUsageException {

	public IncorrectUpdateSemanticsDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param cause 来自底层API的根本原因, 例如JDBC
	 */
	public IncorrectUpdateSemanticsDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * 返回数据是否已更新.
	 * 如果此方法返回false, 则无法回滚.
	 * <p>默认实现始终返回true. 可以在子类中重写.
	 */
	public boolean wasDataUpdated() {
		return true;
	}

}
