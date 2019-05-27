package org.springframework.jdbc.support;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;

/**
 * {@link org.springframework.jdbc.core.JdbcTemplate}和其他JDBC访问DAO助手的基类,
 * 定义公共属性, 如DataSource和异常翻译器.
 *
 * <p>不打算直接使用.
 * See {@link org.springframework.jdbc.core.JdbcTemplate}.
 */
public abstract class JdbcAccessor implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private DataSource dataSource;

	private volatile SQLExceptionTranslator exceptionTranslator;

	private boolean lazyInit = true;


	/**
	 * 设置从中获取连接的JDBC DataSource.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 返回此模板使用的DataSource.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * 指定此访问器使用的DataSource的数据库产品名称.
	 * 这允许初始化SQLErrorCodeSQLExceptionTranslator, 而无需从DataSource获取连接以获取元数据.
	 * 
	 * @param dbName 标识错误代码条目的数据库产品名称
	 */
	public void setDatabaseProductName(String dbName) {
		this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
	}

	/**
	 * 设置此实例的异常转换程序.
	 * <p>如果没有提供自定义转换器, 则使用默认的{@link SQLErrorCodeSQLExceptionTranslator}来检查SQLException的特定于供应商的错误代码.
	 */
	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
		this.exceptionTranslator = exceptionTranslator;
	}

	/**
	 * 返回此实例的异常转换器.
	 * <p>如果没有设置, 则为指定的DataSource创建默认的{@link SQLErrorCodeSQLExceptionTranslator};
	 * 如果没有DataSource, 则创建{@link SQLStateSQLExceptionTranslator}.
	 */
	public SQLExceptionTranslator getExceptionTranslator() {
		SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
		if (exceptionTranslator != null) {
			return exceptionTranslator;
		}
		synchronized (this) {
			exceptionTranslator = this.exceptionTranslator;
			if (exceptionTranslator == null) {
				DataSource dataSource = getDataSource();
				if (dataSource != null) {
					exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
				}
				else {
					exceptionTranslator = new SQLStateSQLExceptionTranslator();
				}
				this.exceptionTranslator = exceptionTranslator;
			}
			return exceptionTranslator;
		}
	}

	/**
	 * 设置是否在第一次遇到SQLException时, 延迟初始化此访问器的SQLExceptionTranslator.
	 * 默认"true"; 可以在启动时切换为"false"进行初始化.
	 * <p>如果调用{@code afterPropertiesSet()}, 则实时初始化才适用.
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回是否为此访问器延迟初始化SQLExceptionTranslator.
	 */
	public boolean isLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 如果需要, 实时初始化异常转换器, 如果没有设置, 则为指定的DataSource创建默认转换器.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getDataSource() == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		if (!isLazyInit()) {
			getExceptionTranslator();
		}
	}
}
