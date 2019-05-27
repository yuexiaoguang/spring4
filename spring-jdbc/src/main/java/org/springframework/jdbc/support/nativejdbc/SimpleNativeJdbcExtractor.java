package org.springframework.jdbc.support.nativejdbc;

/**
 * {@link NativeJdbcExtractor}接口的简单实现.
 * 假设一个包装Connection句柄而不是DatabaseMetaData的池:
 * 在这种情况下, 只需调用{@code conHandle.getMetaData().getConnection()}即可检索底层本机Connection.
 * 所有其他JDBC对象将在传入时返回.
 *
 * <p>此提取器应该与任何不包装DatabaseMetaData的池一起使用, 并且也可以使用任何普通的JDBC驱动程序.
 * 请注意, 池仍然可以包装Statements, PreparedStatements, 等:
 * 这个提取器的唯一要求是{@code java.sql.DatabaseMetaData}没有被包装,
 * 在{@code metaData.getConnection()}上返回JDBC驱动程序的本机Connection.
 *
 * <p>通过相应地设置"nativeConnectionNecessaryForXxx"标志来自定义此提取器:
 * 如果池包装了Statement, PreparedStatement, 和/或 CallableStatement,
 * 将相应的"nativeConnectionNecessaryForXxx"标志设置为"true".
 * 如果没有包装任何语句类型 - 或者首先只需要 Connection解包 -, 那么默认值就可以了.
 *
 * <p>SimpleNativeJdbcExtractor是与OracleLobHandler一起使用的常见选择,
 * 它只需要通过{@link #getNativeConnectionFromStatement}方法进行Connection解包.
 * 此用法几乎适用于任何连接池.
 *
 * <p>用于JdbcTemplate的完全使用, i.e. 还提供Statement解包:
 * <ul>
 * <li>对Resin和SJSAS使用默认的SimpleNativeJdbcExtractor (没有包装JDBC Statement对象, 因此不需要特殊的解包).
 * <li>使用SimpleNativeJdbcExtractor将C3P0的所有"nativeConnectionNecessaryForXxx"标志设置为"true"
 * (所有JDBC Statement对象都被包装, 但没有包装器允许解包).
 * <li>对Apache Commons DBCP使用CommonsDbcpNativeJdbcExtractor, 或对JBoss使用JBossNativeJdbcExtractor
 * (所有JDBC Statement对象都被包装, 但所有这些对象都可以通过强制转换为实现类来提取).
 * </ul>
 */
public class SimpleNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private boolean nativeConnectionNecessaryForNativeStatements = false;

	private boolean nativeConnectionNecessaryForNativePreparedStatements = false;

	private boolean nativeConnectionNecessaryForNativeCallableStatements = false;


	/**
	 * 设置是否有必要在本机Connection上工作以接收本机Statement.
	 * 默认"false". 如果为true, 则首先打开Connection以创建Statement.
	 * <p>如果需要使用池中的本机Statement, 该语句不允许从其包装器中提取本机JDBC对象,
	 * 但其在DatabaseMetaData.getConnection上返回本机Connection, 这是有意义的.
	 * <p>标准的SimpleNativeJdbcExtractor无法解包语句, 因此如果连接池包装Statement, 请将此设置为true.
	 */
	public void setNativeConnectionNecessaryForNativeStatements(boolean nativeConnectionNecessaryForNativeStatements) {
		this.nativeConnectionNecessaryForNativeStatements = nativeConnectionNecessaryForNativeStatements;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return this.nativeConnectionNecessaryForNativeStatements;
	}

	/**
	 * 设置是否有必要在本机Connection上工作以接收本机PreparedStatement.
	 * 默认"false". 如果为true, 则首先解包Connection以创建PreparedStatement.
	 * <p>如果需要使用池中的本机PreparedStatement, 该语句不允许从其包装器中提取本机JDBC对象,
	 * 但返回Statement.getConnection上的本机Connection, 这是有意义的.
	 * <p>标准的SimpleNativeJdbcExtractor无法解包语句, 因此如果连接池包装PreparedStatement, 请将此属性设置为true.
	 */
	public void setNativeConnectionNecessaryForNativePreparedStatements(boolean nativeConnectionNecessary) {
		this.nativeConnectionNecessaryForNativePreparedStatements = nativeConnectionNecessary;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return this.nativeConnectionNecessaryForNativePreparedStatements;
	}

	/**
	 * 设置是否有必要在本机Connection上工作以接收本机CallableStatement.
	 * 默认"false". 如果为true, 则首先解包Connection以创建CallableStatement.
	 * <p>如果需要使用池中的本机CallableStatement, 该语句不允许从其包装器中提取本机JDBC对象,
	 * 但返回Statement.getConnection上的本机Connection, 这是有意义的.
	 * <p>标准的SimpleNativeJdbcExtractor无法解包语句, 因此如果连接池包装CallableStatement, 请将此设置为true.
	 */
	public void setNativeConnectionNecessaryForNativeCallableStatements(boolean nativeConnectionNecessary) {
		this.nativeConnectionNecessaryForNativeCallableStatements = nativeConnectionNecessary;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return this.nativeConnectionNecessaryForNativeCallableStatements;
	}
}
