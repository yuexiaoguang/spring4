package org.springframework.jdbc.datasource.embedded;

/**
 * 支持的嵌入式数据库类型.
 */
public enum EmbeddedDatabaseType {

	/** The Hypersonic Embedded Java SQL Database (http://hsqldb.org) */
	HSQL,

	/** The H2 Embedded Java SQL Database Engine (http://h2database.com) */
	H2,

	/** The Apache Derby Embedded SQL Database (http://db.apache.org/derby) */
	DERBY

}
