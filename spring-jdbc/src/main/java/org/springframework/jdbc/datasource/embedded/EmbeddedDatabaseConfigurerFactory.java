package org.springframework.jdbc.datasource.embedded;

import org.springframework.util.Assert;

/**
 * 映射已知的{@linkplain EmbeddedDatabaseType 嵌入式数据库类型}到{@link EmbeddedDatabaseConfigurer}策略.
 */
final class EmbeddedDatabaseConfigurerFactory {

	/**
	 * 返回给定嵌入式数据库类型的配置器实例.
	 * 
	 * @param type HSQL, H2 或 Derby
	 * 
	 * @return 配置器实例
	 * @throws IllegalStateException 如果指定的数据库类型的驱动程序不可用
	 */
	public static EmbeddedDatabaseConfigurer getConfigurer(EmbeddedDatabaseType type) throws IllegalStateException {
		Assert.notNull(type, "EmbeddedDatabaseType is required");
		try {
			switch (type) {
				case HSQL:
					return HsqlEmbeddedDatabaseConfigurer.getInstance();
				case H2:
					return H2EmbeddedDatabaseConfigurer.getInstance();
				case DERBY:
					return DerbyEmbeddedDatabaseConfigurer.getInstance();
				default:
					throw new UnsupportedOperationException("Embedded database type [" + type + "] is not supported");
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Driver for test database type [" + type + "] is not available", ex);
		}
		catch (NoClassDefFoundError err) {
			throw new IllegalStateException("Driver for test database type [" + type + "] is not available", err);
		}
	}

}
