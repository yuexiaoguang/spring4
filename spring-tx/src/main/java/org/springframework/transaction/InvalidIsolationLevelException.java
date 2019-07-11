package org.springframework.transaction;

/**
 * 指定无效隔离级别时抛出的异常, i.e. 事务管理器实现不支持的隔离级别.
 */
@SuppressWarnings("serial")
public class InvalidIsolationLevelException extends TransactionUsageException {

	public InvalidIsolationLevelException(String msg) {
		super(msg);
	}

}
