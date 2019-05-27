package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.lang.UsesJava7;
import org.springframework.util.ClassUtils;

/**
 * {@link ResultSetExtractor}实现, 为每个给定的{@link ResultSet}返回Spring {@link SqlRowSet}表示.
 *
 * <p>默认实现使用下面的标准JDBC CachedRowSet.
 * 这意味着JDBC RowSet支持需要在运行时可用:
 * 默认情况下, Sun在Java 6上的{@code com.sun.rowset.CachedRowSetImpl}类,
 * 或Java 7+ / JDBC 4.1+上的{@code javax.sql.rowset.RowSetProvider}机制.
 */
public class SqlRowSetResultSetExtractor implements ResultSetExtractor<SqlRowSet> {

	private static final CachedRowSetFactory cachedRowSetFactory;

	static {
		if (ClassUtils.isPresent("javax.sql.rowset.RowSetProvider",
				SqlRowSetResultSetExtractor.class.getClassLoader())) {
			// using JDBC 4.1 RowSetProvider, available on JDK 7+
			cachedRowSetFactory = new StandardCachedRowSetFactory();
		}
		else {
			// JDBC 4.1 API not available - fall back to Sun CachedRowSetImpl on JDK 6
			cachedRowSetFactory = new SunCachedRowSetFactory();
		}
	}


	@Override
	public SqlRowSet extractData(ResultSet rs) throws SQLException {
		return createSqlRowSet(rs);
	}

	/**
	 * 创建一个包装给定ResultSet的SqlRowSet, 以断开连接的方式表示其数据.
	 * <p>此实现创建一个Spring ResultSetWrappingSqlRowSet实例, 该实例包装标准JDBC CachedRowSet实例.
	 * 可以重写以使用不同的实现.
	 * 
	 * @param rs 原始ResultSet (已连接)
	 * 
	 * @return 断开连接的SqlRowSet
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	protected SqlRowSet createSqlRowSet(ResultSet rs) throws SQLException {
		CachedRowSet rowSet = newCachedRowSet();
		rowSet.populate(rs);
		return new ResultSetWrappingSqlRowSet(rowSet);
	}

	/**
	 * 创建一个新的CachedRowSet实例, 由{@code createSqlRowSet}实现填充.
	 * <p>在JDK 7或更高版本上运行时, 默认实现使用JDBC 4.1的RowSetProvider,
	 * 在旧JDK上回退到Sun的{@code com.sun.rowset.CachedRowSetImpl}类.
	 * 
	 * @return 新的CachedRowSet实例
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	protected CachedRowSet newCachedRowSet() throws SQLException {
		return cachedRowSetFactory.createCachedRowSet();
	}


	/**
	 * 用于创建CachedRowSet实例的内部策略接口.
	 */
	private interface CachedRowSetFactory {

		CachedRowSet createCachedRowSet() throws SQLException;
	}


	/**
	 * 内部类, 避免了对JDBC 4.1 RowSetProvider类的硬依赖.
	 */
	@UsesJava7
	private static class StandardCachedRowSetFactory implements CachedRowSetFactory {

		private final RowSetFactory rowSetFactory;

		public StandardCachedRowSetFactory() {
			try {
				this.rowSetFactory = RowSetProvider.newFactory();
			}
			catch (SQLException ex) {
				throw new IllegalStateException("Cannot create RowSetFactory through RowSetProvider", ex);
			}
		}

		@Override
		public CachedRowSet createCachedRowSet() throws SQLException {
			return this.rowSetFactory.createCachedRowSet();
		}
	}


	/**
	 * 内部类, 以避免对Sun的CachedRowSetImpl类的硬依赖.
	 */
	private static class SunCachedRowSetFactory implements CachedRowSetFactory {

		private static final Class<?> implementationClass;

		static {
			try {
				implementationClass = ClassUtils.forName("com.sun.rowset.CachedRowSetImpl",
						SqlRowSetResultSetExtractor.class.getClassLoader());
			}
			catch (Throwable ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		public CachedRowSet createCachedRowSet() throws SQLException {
			try {
				return (CachedRowSet) implementationClass.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException(ex);
			}
		}
	}
}
