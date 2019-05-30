package org.springframework.orm.jpa.vendor;

/**
 * 公共数据库平台的枚举.
 * 允许在JpaVendorDialect实现之间强类型化数据库类型和可移植配置.
 *
 * <p>如果给定的PersistenceProvider支持此处未列出的数据库, 则仍可使用完全限定的类名指定策略类.
 * 这个枚举仅仅是一种便利. 此处列出的数据库产品与{@code sql-error-codes.xml}中的Spring JDBC异常转换显式支持的数据库产品相同.
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
