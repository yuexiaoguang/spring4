package org.springframework.jdbc.core;

/**
 * 由可以关闭由{@code SqlLobValue}对象等参数分配的资源的对象实现的接口.
 *
 * <p>通常由{@code PreparedStatementCreators}和{@code PreparedStatementSetters}实现,
 * 支持{@link DisposableSqlTypeValue}对象 (e.g. {@code SqlLobValue})作为参数.
 */
public interface ParameterDisposer {

	/**
	 * 关闭由实现对象保存的参数分配的资源, 例如在 DisposableSqlTypeValue (如 SqlLobValue)的情况下.
	 */
	void cleanupParameters();

}
