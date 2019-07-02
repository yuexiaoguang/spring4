package org.springframework.test.jdbc;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.StringUtils;

/**
 * {@code JdbcTestUtils}是JDBC相关工具函数的集合, 旨在简化标准数据库测试场景.
 */
public class JdbcTestUtils {

	private static final Log logger = LogFactory.getLog(JdbcTestUtils.class);


	/**
	 * 计算给定表中的行数.
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param tableName 要计算行数的表的名称
	 * 
	 * @return 表中的行数
	 */
	public static int countRowsInTable(JdbcTemplate jdbcTemplate, String tableName) {
		return jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
	}

	/**
	 * 使用提供的{@code WHERE}子句计算给定表中的行数.
	 * <p>如果提供的{@code WHERE}子句包含文本, 则它将以{@code " WHERE "}为前缀, 然后附加到生成的{@code SELECT}语句中.
	 * 例如, 如果提供的表名为{@code "person"}, 且提供的where子句为{@code "name = 'Bob' and age > 25"},
	 * 则生成的结果SQL语句将为 {@code "SELECT COUNT(0) FROM person WHERE name = 'Bob' and age > 25"}.
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param tableName 要计算行数的表的名称
	 * @param whereClause 要附加到查询的{@code WHERE}子句
	 * 
	 * @return 表中与提供的{@code WHERE}子句匹配的行数
	 */
	public static int countRowsInTableWhere(JdbcTemplate jdbcTemplate, String tableName, String whereClause) {
		String sql = "SELECT COUNT(0) FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		return jdbcTemplate.queryForObject(sql, Integer.class);
	}

	/**
	 * 删除指定表中的所有行.
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param tableNames 要删除的表的名称
	 * 
	 * @return 从所有指定表中删除的总行数
	 */
	public static int deleteFromTables(JdbcTemplate jdbcTemplate, String... tableNames) {
		int totalRowCount = 0;
		for (String tableName : tableNames) {
			int rowCount = jdbcTemplate.update("DELETE FROM " + tableName);
			totalRowCount += rowCount;
			if (logger.isInfoEnabled()) {
				logger.info("Deleted " + rowCount + " rows from table " + tableName);
			}
		}
		return totalRowCount;
	}

	/**
	 * 使用提供的{@code WHERE}子句删除给定表中的行.
	 * <p>如果提供的{@code WHERE}子句包含文本, 则它将以{@code " WHERE "}为前缀,
	 * 然后附加到生成的{@code DELETE}语句.
	 * 例如, 如果提供的表名为{@code "person"}, 且提供的where子句为{@code "name = 'Bob' and age > 25"},
	 * 则生成的结果SQL语句将为{@code "DELETE FROM person WHERE name = 'Bob' and age > 25"}.
	 * <p>作为硬编码值的替代, {@code "?"}占位符可以在{@code WHERE}子句中使用, 绑定到给定的参数.
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param tableName 要从中删除行的表的名称
	 * @param whereClause 要附加到查询的{@code WHERE}子句
	 * @param args 绑定到查询的参数 (将其留给PreparedStatement来猜测相应的SQL类型);
	 * 也可能包含{@link SqlParameterValue}对象, 这些对象不仅指示参数值, 还指示SQL类型和可选的范围.
	 * 
	 * @return 从表中删除的行数
	 */
	public static int deleteFromTableWhere(JdbcTemplate jdbcTemplate, String tableName, String whereClause,
			Object... args) {

		String sql = "DELETE FROM " + tableName;
		if (StringUtils.hasText(whereClause)) {
			sql += " WHERE " + whereClause;
		}
		int rowCount = (args != null && args.length > 0 ? jdbcTemplate.update(sql, args) : jdbcTemplate.update(sql));
		if (logger.isInfoEnabled()) {
			logger.info("Deleted " + rowCount + " rows from table " + tableName);
		}
		return rowCount;
	}

	/**
	 * 删除指定的表.
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param tableNames 要删除的表的名称
	 */
	public static void dropTables(JdbcTemplate jdbcTemplate, String... tableNames) {
		for (String tableName : tableNames) {
			jdbcTemplate.execute("DROP TABLE " + tableName);
			if (logger.isInfoEnabled()) {
				logger.info("Dropped table " + tableName);
			}
		}
	}

	/**
	 * 执行给定的SQL脚本.
	 * <p>通常会从类路径加载脚本. 每行应该有一个语句. 将删除任何分号和行注释.
	 * <p><b>如果希望回滚, 不要使用此方法执行DDL.</b>
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param resourceLoader 用于加载SQL脚本的资源加载器
	 * @param sqlResourcePath SQL脚本的Spring资源路径
	 * @param continueOnError 是否在发生错误时继续而不抛出异常
	 * 
	 * @throws DataAccessException 如果执行语句时出错, 并且{@code continueOnError}为{@code false}
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#executeSqlScript}
	 * or {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator}.
	 */
	@Deprecated
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader,
			String sqlResourcePath, boolean continueOnError) throws DataAccessException {

		Resource resource = resourceLoader.getResource(sqlResourcePath);
		executeSqlScript(jdbcTemplate, resource, continueOnError);
	}

	/**
	 * 执行给定的SQL脚本.
	 * <p>通常会从类路径加载脚本. 语句应以分号分隔. 如果语句没有用分号分隔, 那么每行应该有一个语句.
	 * 只有当使用分号分隔时, 才允许语句跨越多行. 任何行注释都将被删除.
	 * <p><b>如果希望回滚, 不要使用此方法执行DDL.</b>
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param resource 从中加载SQL脚本的资源
	 * @param continueOnError 是否在发生错误时继续而不抛出异常
	 * 
	 * @throws DataAccessException 如果执行语句时出错, 并且{@code continueOnError}为{@code false}
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#executeSqlScript}
	 * or {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator}.
	 */
	@Deprecated
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, Resource resource, boolean continueOnError)
			throws DataAccessException {

		executeSqlScript(jdbcTemplate, new EncodedResource(resource), continueOnError);
	}

	/**
	 * 执行给定的SQL脚本.
	 * <p>通常会从类路径加载脚本. 每行应该有一个语句. 将删除任何分号和行注释.
	 * <p><b>如果希望回滚, 不要使用此方法执行DDL.</b>
	 * 
	 * @param jdbcTemplate 用于执行JDBC操作的JdbcTemplate
	 * @param resource 从中加载SQL脚本的资源 (可能与特定编码相关联)
	 * @param continueOnError 是否在发生错误时继续而不抛出异常
	 * 
	 * @throws DataAccessException 如果执行语句时出错, 并且{@code continueOnError}为{@code false}
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#executeSqlScript}
	 * or {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator}.
	 */
	@Deprecated
	public static void executeSqlScript(JdbcTemplate jdbcTemplate, EncodedResource resource, boolean continueOnError)
			throws DataAccessException {

		new ResourceDatabasePopulator(continueOnError, false, resource.getEncoding(), resource.getResource()).execute(jdbcTemplate.getDataSource());
	}

	/**
	 * 从提供的{@code LineNumberReader}中读取脚本, 使用"{@code --}"作为注释前缀, 并构建包含这些行的{@code String}.
	 * 
	 * @param lineNumberReader 包含要处理的脚本的{@code LineNumberReader}
	 * 
	 * @return 包含脚本行的{@code String}
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#readScript(LineNumberReader, String, String)}
	 */
	@Deprecated
	public static String readScript(LineNumberReader lineNumberReader) throws IOException {
		return readScript(lineNumberReader, ScriptUtils.DEFAULT_COMMENT_PREFIX);
	}

	/**
	 * 使用提供的注释前缀从提供的{@code LineNumberReader}中读取脚本, 并构建包含这些行的{@code String}.
	 * <p>具有注释前缀的<em>开头</em>的行将从结果中排除; 但是, 其它地方的行注释 &mdash; 例如, 在声明中 &mdash; 将包含在结果中.
	 * 
	 * @param lineNumberReader 包含要处理的脚本的{@code LineNumberReader}
	 * @param commentPrefix 标识SQL脚本中注释的前缀 &mdash; 通常是 "--"
	 * 
	 * @return 包含脚本行的{@code String}
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#readScript(LineNumberReader, String, String)}
	 */
	@Deprecated
	public static String readScript(LineNumberReader lineNumberReader, String commentPrefix) throws IOException {
		return ScriptUtils.readScript(lineNumberReader, commentPrefix, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
	}

	/**
	 * 确定提供的SQL脚本是否包含指定的分隔符.
	 * 
	 * @param script SQL脚本
	 * @param delim 分隔每个语句的字符 &mdash; 通常是';'字符
	 * 
	 * @return {@code true} 如果脚本包含分隔符; 否则{@code false}
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#containsSqlScriptDelimiters}
	 */
	@Deprecated
	public static boolean containsSqlScriptDelimiters(String script, char delim) {
		return ScriptUtils.containsSqlScriptDelimiters(script, String.valueOf(delim));
	}

	/**
	 * 将SQL脚本拆分为由提供的分隔符分隔的单独语句. 每个单独的语句都将添加到提供的{@code List}中.
	 * <p>在语句中, "{@code --}" 将用作注释前缀; 任何以注释前缀开头并延伸到行尾的文本都将从语句中省略.
	 * 此外, 多个相邻的空白字符将折叠为单个空格.
	 * 
	 * @param script SQL脚本
	 * @param delim 分隔每个语句的字符 &mdash; 通常是';'字符
	 * @param statements 包含单个语句的列表
	 * 
	 * @deprecated as of Spring 4.0.3, in favor of using
	 * {@link org.springframework.jdbc.datasource.init.ScriptUtils#splitSqlScript(String, char, List)}
	 */
	@Deprecated
	public static void splitSqlScript(String script, char delim, List<String> statements) {
		ScriptUtils.splitSqlScript(script, delim, statements);
	}
}
