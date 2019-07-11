package org.springframework.transaction;

/**
 * 事务超时时抛出的异常.
 *
 * <p>如果在尝试操作时达到事务的截止时间, 则根据为给定事务指定的超时, 由Spring的本地事务策略抛出.
 *
 * <p>除了在每次事务操作之前进行此类检查之外, Spring的本地事务策略还会将适当的超时值传递给资源操作 (例如, JDBC语句, 让JDBC驱动程序遵守超时).
 * 如果操作超时, 此类操作通常会抛出本机资源异常 (例如, JDBC SQLExceptions),
 * 并在相应的DAO中转换为Spring的DataAccessException (例如, 可能使用Spring的JdbcTemplate).
 *
 * <p>在JTA环境中, 由JTA事务协调器决定是否应用事务超时.
 * 通常, 相应的JTA感知连接池将执行超时检查并抛出相应的本机资源异常 (例如, JDBC SQLExceptions).
 */
@SuppressWarnings("serial")
public class TransactionTimedOutException extends TransactionException {

	public TransactionTimedOutException(String msg) {
		super(msg);
	}

	public TransactionTimedOutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
