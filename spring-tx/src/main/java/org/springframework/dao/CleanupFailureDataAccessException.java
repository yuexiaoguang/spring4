package org.springframework.dao;

/**
 * 在数据访问操作后无法清理时抛出异常, 但实际操作正常.
 *
 * <p>例如, 如果在成功使用JDBC连接后无法关闭它, 则可能抛出此异常或子类.
 *
 * <p>请注意, 数据访问代码可能会在finally块中执行资源清理, 因此会记录清理失败而不是重新抛出它, 以保持原始数据访问异常.
 */
@SuppressWarnings("serial")
public class CleanupFailureDataAccessException extends NonTransientDataAccessException {

	/**
	 * @param msg 详细信息
	 * @param cause 来自底层数据访问API的根本原因, 例如JDBC
	 */
	public CleanupFailureDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
