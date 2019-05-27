package org.springframework.orm.jpa.vendor;

/**
 * Enumeration for common database platforms. Allows strong typing of database type
 * and portable configuration between JpaVendorDialect implementations.
 *
 * <p>If a given PersistenceProvider supports a database not listed here,
 * the strategy class can still be specified using the fully-qualified class name.
 * This enumeration is merely a convenience. The database products listed here
 * are the same as those explicitly supported for Spring JDBC exception translation
 * in {@code sql-error-codes.xml}.
 */
public enum Database {

	DEFAULT,

	DB2,

	DERBY,

	H2,

	HSQL,

	INFORMIX,

	MYSQL,

	ORACLE,

	POSTGRESQL,

	SQL_SERVER,

	SYBASE

}
