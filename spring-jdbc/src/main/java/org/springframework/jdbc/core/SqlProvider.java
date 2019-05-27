package org.springframework.jdbc.core;

/**
 * 由可以提供SQL字符串的对象实现的接口.
 *
 * <p>通常由PreparedStatementCreators, CallableStatementCreators 和 StatementCallbacks实现,
 * 它们希望公开用于创建语句的SQL, 以便在出现异常时提供更好的上下文信息.
 */
public interface SqlProvider {

	/**
	 * 返回此对象的SQL字符串, i.e. 通常用于创建语句的SQL.
	 * 
	 * @return SQL字符串, 或{@code null}
	 */
	String getSql();

}
