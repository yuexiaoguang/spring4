package org.springframework.jdbc.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

/**
 * 用于检查数据库是否已启动的Bean.
 * 通过依赖于数据库启动的bean的"依赖"引用, 如Hibernate SessionFactory或直接访问DataSource的自定义数据访问对象.
 *
 * <p>用于推迟应用程序初始化直到数据库启动.
 * 特别适合等待缓慢启动的Oracle数据库.
 */
public class DatabaseStartupValidator implements InitializingBean {

	public static final int DEFAULT_INTERVAL = 1;

	public static final int DEFAULT_TIMEOUT = 60;


	protected final Log logger = LogFactory.getLog(getClass());

	private DataSource dataSource;

	private String validationQuery;

	private int interval = DEFAULT_INTERVAL;

	private int timeout = DEFAULT_TIMEOUT;


	/**
	 * 设置要验证的DataSource.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置要用于验证的SQL查询字符串.
	 */
	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	/**
	 * 设置验证运行之间的间隔 (以秒为单位).
	 * 默认{@value #DEFAULT_INTERVAL}.
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}

	/**
	 * 设置超时 (以秒为单位), 之后将引发致命异常.
	 * 默认{@value #DEFAULT_TIMEOUT}.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}


	/**
	 * 检查验证查询是否可以在指定DataSource的Connection上执行, 并在指定的检查间隔内执行, 直到指定的超时.
	 */
	@Override
	public void afterPropertiesSet() {
		DataSource dataSource = this.dataSource;
		if (dataSource == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		if (this.validationQuery == null) {
			throw new IllegalArgumentException("Property 'validationQuery' is required");
		}

		try {
			boolean validated = false;
			long beginTime = System.currentTimeMillis();
			long deadLine = beginTime + this.timeout * 1000;
			SQLException latestEx = null;

			while (!validated && System.currentTimeMillis() < deadLine) {
				Connection con = null;
				Statement stmt = null;
				try {
					con = dataSource.getConnection();
					stmt = con.createStatement();
					stmt.execute(this.validationQuery);
					validated = true;
				}
				catch (SQLException ex) {
					latestEx = ex;
					if (logger.isDebugEnabled()) {
						logger.debug("Validation query [" + this.validationQuery + "] threw exception", ex);
					}
					if (logger.isWarnEnabled()) {
						float rest = ((float) (deadLine - System.currentTimeMillis())) / 1000;
						if (rest > this.interval) {
							logger.warn("Database has not started up yet - retrying in " + this.interval +
									" seconds (timeout in " + rest + " seconds)");
						}
					}
				}
				finally {
					JdbcUtils.closeStatement(stmt);
					JdbcUtils.closeConnection(con);
				}

				if (!validated) {
					Thread.sleep(this.interval * 1000);
				}
			}

			if (!validated) {
				throw new CannotGetJdbcConnectionException(
						"Database has not started up within " + this.timeout + " seconds", latestEx);
			}

			if (logger.isInfoEnabled()) {
				float duration = ((float) (System.currentTimeMillis() - beginTime)) / 1000;
				logger.info("Database startup detected after " + duration + " seconds");
			}
		}
		catch (InterruptedException ex) {
			// 重新中断当前线程, 以允许其他线程做出反应.
			Thread.currentThread().interrupt();
		}
	}

}
