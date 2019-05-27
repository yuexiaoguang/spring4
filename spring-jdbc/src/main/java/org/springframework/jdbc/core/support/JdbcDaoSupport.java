package org.springframework.jdbc.core.support;

import java.sql.Connection;
import javax.sql.DataSource;

import org.springframework.dao.support.DaoSupport;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * 基于JDBC的数据访问对象的便捷超类.
 *
 * <p>需要设置{@link javax.sql.DataSource},
 * 通过{@link #getJdbcTemplate()}方法提供基于它的{@link org.springframework.jdbc.core.JdbcTemplate}子类.
 *
 * <p>此基类主要用于JdbcTemplate用法, 但也可以在直接使用Connection或使用{@code org.springframework.jdbc.object}操作对象时使用.
 */
public abstract class JdbcDaoSupport extends DaoSupport {

	private JdbcTemplate jdbcTemplate;


	/**
	 * 设置此DAO要使用的JDBC DataSource.
	 */
	public final void setDataSource(DataSource dataSource) {
		if (this.jdbcTemplate == null || dataSource != this.jdbcTemplate.getDataSource()) {
			this.jdbcTemplate = createJdbcTemplate(dataSource);
			initTemplateConfig();
		}
	}

	/**
	 * 为给定的DataSource创建JdbcTemplate.
	 * 仅在使用DataSource引用填充DAO时调用!
	 * <p>可以在子类中重写以提供具有不同配置的JdbcTemplate实例, 或者自定义JdbcTemplate子类.
	 * 
	 * @param dataSource 为其创建JdbcTemplate的JDBC DataSource
	 * 
	 * @return 新的JdbcTemplate实例
	 */
	protected JdbcTemplate createJdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	/**
	 * 返回此DAO使用的JDBC DataSource.
	 */
	public final DataSource getDataSource() {
		return (this.jdbcTemplate != null ? this.jdbcTemplate.getDataSource() : null);
	}

	/**
	 * 显式设置此DAO的JdbcTemplate, 作为指定DataSource的替代方法.
	 */
	public final void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		initTemplateConfig();
	}

	/**
	 * 返回此DAO的JdbcTemplate, 使用DataSource预先初始化或显式设置.
	 */
	public final JdbcTemplate getJdbcTemplate() {
	  return this.jdbcTemplate;
	}

	/**
	 * 初始化此DAO的基于模板的配置.
	 * 在设置新的JdbcTemplate后, 直接或通过DataSource调用.
	 * <p>此实现为空. 子类可以重写此方法以基于JdbcTemplate配置更多对象.
	 */
	protected void initTemplateConfig() {
	}

	@Override
	protected void checkDaoConfig() {
		if (this.jdbcTemplate == null) {
			throw new IllegalArgumentException("'dataSource' or 'jdbcTemplate' is required");
		}
	}


	/**
	 * 返回此DAO的JdbcTemplate的SQLExceptionTranslator, 用于在自定义JDBC访问代码中转换SQLException.
	 */
	protected final SQLExceptionTranslator getExceptionTranslator() {
		return getJdbcTemplate().getExceptionTranslator();
	}

	/**
	 * 从当前事务或新事务获取JDBC连接.
	 * 
	 * @return JDBC连接
	 * @throws CannotGetJdbcConnectionException 如果尝试获取连接失败
	 */
	protected final Connection getConnection() throws CannotGetJdbcConnectionException {
		return DataSourceUtils.getConnection(getDataSource());
	}

	/**
	 * 如果未绑定到线程, 则关闭通过此DAO的DataSource创建的给定JDBC连接.
	 * 
	 * @param con 要关闭的连接
	 */
	protected final void releaseConnection(Connection con) {
		DataSourceUtils.releaseConnection(con, getDataSource());
	}
}
