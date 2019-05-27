package org.springframework.jdbc.core.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.util.Assert;

/**
 * Bean定义读取器, 它根据给定的SQL语句从数据库表中读取值.
 *
 * <p>bean名称, 属性名称和值的列应为String.
 * 每种格式都与PropertiesBeanDefinitionReader识别的属性格式相同.
 *
 * <p><b>NOTE:</b> 这主要是作为自定义的基于JDBC的bean定义读取器的示例.
 * 它的目的不是提供全面的功能.
 */
public class JdbcBeanDefinitionReader {

	private final PropertiesBeanDefinitionReader propReader;

	private JdbcTemplate jdbcTemplate;


	/**
	 * 底层使用默认PropertiesBeanDefinitionReader.
	 * <p>仍然需要设置DataSource或JdbcTemplate.
	 */
	public JdbcBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		this.propReader = new PropertiesBeanDefinitionReader(beanFactory);
	}

	/**
	 * 底层委托给指定的PropertiesBeanDefinitionReader.
	 * <p>仍然需要设置DataSource或JdbcTemplate.
	 */
	public JdbcBeanDefinitionReader(PropertiesBeanDefinitionReader beanDefinitionReader) {
		Assert.notNull(beanDefinitionReader, "Bean definition reader must not be null");
		this.propReader = beanDefinitionReader;
	}


	/**
	 * 设置用于获取数据库连接的DataSource.
	 * 将使用给定的DataSource隐式创建新的JdbcTemplate.
	 */
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 设置此bean工厂使用的JdbcTemplate.
	 * 包含DataSource, SQLExceptionTranslator等的设置.
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}


	/**
	 * 通过给定的SQL字符串从数据库加载bean定义.
	 * 
	 * @param sql 用于加载bean定义的SQL查询.
	 * 前三列必须是bean名称, 属性名称和值.
	 * 允许任何join和任何其他列:
	 * e.g. {@code SELECT BEAN_NAME, PROPERTY, VALUE FROM CONFIG WHERE CONFIG.APP_ID = 1}
	 * 也可以执行 join. 列名不重要 -- 只列出前三列的顺序.
	 */
	public void loadBeanDefinitions(String sql) {
		Assert.notNull(this.jdbcTemplate, "Not fully configured - specify DataSource or JdbcTemplate");
		final Properties props = new Properties();
		this.jdbcTemplate.query(sql, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				String beanName = rs.getString(1);
				String property = rs.getString(2);
				String value = rs.getString(3);
				// 通过组合bean名称和属性来创建属性.
				props.setProperty(beanName + '.' + property, value);
			}
		});
		this.propReader.registerBeanDefinitions(props);
	}

}
