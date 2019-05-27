package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

/**
 * {@code EmbeddedDatabase}用作嵌入式数据库实例的句柄.
 *
 * <p>{@code EmbeddedDatabase}也是{@link DataSource}, 并添加{@link #shutdown}操作,
 * 以便可以优雅地关闭嵌入式数据库实例.
 */
public interface EmbeddedDatabase extends DataSource {

	/**
	 * 关闭此嵌入式数据库.
	 */
	void shutdown();

}
