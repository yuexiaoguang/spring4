package org.springframework.test.context.junit4;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AbstractJUnit4SpringContextTests}的抽象{@linkplain Transactional 事务}扩展, 为JDBC访问增加了便利功能.
 * 预计将在Spring {@linkplain ApplicationContext 应用程序上下文}中
 * 定义{@link DataSource} bean和{@link PlatformTransactionManager} bean.
 *
 * <p>这个类公开了一个{@link JdbcTemplate}并提供了一种简单的方法在事务中
 * {@linkplain #countRowsInTable 计算表中的行数}
 * (潜在的{@linkplain #countRowsInTableWhere 使用WHERE子句}),
 * {@linkplain #deleteFromTables 从表中删除},
 * {@linkplain #dropTables 删除表}, 和
 * {@linkplain #executeSqlScript 执行SQL脚本}.
 *
 * <p>具体的子类必须满足{@link AbstractJUnit4SpringContextTests}中列出的相同要求.
 *
 * <p>默认情况下配置以下{@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}:
 *
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * <li>{@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener}
 * </ul>
 *
 * <p>此类仅用于扩展的便利.
 * <ul>
 * <li>如果不希望将测试类绑定到特定于Spring的类层次结构, 则可以使用
 * {@link SpringRunner}, {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}等配置自己的自定义测试类.</li>
 * <li>如果希望扩展此类并使用{@link SpringRunner}之外的运行器, 从Spring Framework 4.2开始, 可以使用
 * {@link org.springframework.test.context.junit4.rules.SpringClassRule SpringClassRule}
 * 和{@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule},
 * 并通过{@link org.junit.runner.RunWith @RunWith(...)}指定选择的运行器.</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> 从Spring Framework 4.3开始, 此类需要JUnit 4.12或更高版本.
 */
@TestExecutionListeners({TransactionalTestExecutionListener.class, SqlScriptsTestExecutionListener.class})
@Transactional
public abstract class AbstractTransactionalJUnit4SpringContextTests extends AbstractJUnit4SpringContextTests {

	/**
	 * 此基类管理的{@code JdbcTemplate}, 可用于子类.
	 */
	protected JdbcTemplate jdbcTemplate;

	private String sqlScriptEncoding;


	/**
	 * 设置{@code DataSource}, 通常通过依赖注入提供.
	 * <p>此方法还实例化{@link #jdbcTemplate}实例变量.
	 */
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 如果与平台编码不同, 指定SQL脚本的编码.
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	/**
	 * 用于计算给定表中的行的便捷方法.
	 * 
	 * @param tableName 用于计算行数的表名
	 * 
	 * @return 表中的行数
	 */
	protected int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

	/**
	 * 使用提供的{@code WHERE}子句计算给定表中行的便捷方法.
	 * <p>有关详细信息, 请参阅{@link JdbcTestUtils#countRowsInTableWhere}的Javadoc.
	 * 
	 * @param tableName 要计算行数的表的名称
	 * @param whereClause 要附加到查询的{@code WHERE}子句
	 * 
	 * @return 表中与提供的{@code WHERE}子句匹配的行数
	 */
	protected int countRowsInTableWhere(String tableName, String whereClause) {
		return JdbcTestUtils.countRowsInTableWhere(this.jdbcTemplate, tableName, whereClause);
	}

	/**
	 * 从指定表中删除所有行的便捷方法.
	 * <p>在事务之外谨慎使用!
	 * 
	 * @param names 要删除的表的名称
	 * 
	 * @return 从所有指定表中删除的总行数
	 */
	protected int deleteFromTables(String... names) {
		return JdbcTestUtils.deleteFromTables(this.jdbcTemplate, names);
	}

	/**
	 * 使用提供的{@code WHERE}子句删除给定表中所有行的便捷方法.
	 * <p>在事务之外谨慎使用!
	 * <p>有关详细信息, 请参阅{@link JdbcTestUtils#deleteFromTableWhere}的Javadoc.
	 * 
	 * @param tableName 要从中删除行的表的名称
	 * @param whereClause 要附加到查询的{@code WHERE}子句
	 * @param args 要绑定到查询的参数 (将其留给{@code PreparedStatement}来猜测相应的SQL类型);
	 * 也可能包含
	 * {@link org.springframework.jdbc.core.SqlParameterValue SqlParameterValue}对象,
	 * 这些对象不仅指示参数值, 还指示SQL类型和可选的比例.
	 * 
	 * @return 从表中删除的行数
	 */
	protected int deleteFromTableWhere(String tableName, String whereClause, Object... args) {
		return JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, tableName, whereClause, args);
	}

	/**
	 * 删除所有指定表的便捷方法.
	 * <p>在事务之外谨慎使用!
	 * 
	 * @param names 要删除的表的名称
	 */
	protected void dropTables(String... names) {
		JdbcTestUtils.dropTables(this.jdbcTemplate, names);
	}

	/**
	 * 执行给定的SQL脚本.
	 * <p>在事务之外谨慎使用!
	 * <p>该脚本通常由classpath加载.
	 * <p><b>如果需要回滚, 不要使用此方法执行DDL.</b>
	 * 
	 * @param sqlResourcePath SQL脚本的Spring资源路径
	 * @param continueOnError 是否在发生错误时继续而不抛出异常
	 * 
	 * @throws DataAccessException 如果执行语句时出错
	 */
	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {
		Resource resource = this.applicationContext.getResource(sqlResourcePath);
		new ResourceDatabasePopulator(continueOnError, false, this.sqlScriptEncoding, resource).execute(jdbcTemplate.getDataSource());
	}

}
