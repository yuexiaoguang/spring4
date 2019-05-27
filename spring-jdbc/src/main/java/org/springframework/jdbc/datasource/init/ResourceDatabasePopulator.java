package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用外部资源中定义的SQL脚本填充, 初始化或清理数据库.
 *
 * <ul>
 * <li>调用{@link #addScript}添加单个SQL脚本位置.
 * <li>调用{@link #addScripts}添加多个SQL脚本位置.
 * <li>有关更多配置选项, 请参阅此类中的setter方法.
 * <li>调用{@link #populate}或{@link #execute}使用配置的脚本初始化或清理数据库.
 * </ul>
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

	List<Resource> scripts = new ArrayList<Resource>();

	private String sqlScriptEncoding;

	private String separator = ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;

	private String commentPrefix = ScriptUtils.DEFAULT_COMMENT_PREFIX;

	private String blockCommentStartDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;

	private String blockCommentEndDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;

	private boolean continueOnError = false;

	private boolean ignoreFailedDrops = false;


	/**
	 * 使用默认设置构造新的{@code ResourceDatabasePopulator}.
	 */
	public ResourceDatabasePopulator() {
		/* no-op */
	}

	/**
	 * @param scripts 要执行以初始化或清理数据库的脚本 (never {@code null})
	 */
	public ResourceDatabasePopulator(Resource... scripts) {
		this();
		setScripts(scripts);
	}

	/**
	 * @param continueOnError 应记录SQL中的所有故障但不会导致故障
	 * @param ignoreFailedDrops 可以忽略失败的SQL {@code DROP}语句
	 * @param sqlScriptEncoding 提供的SQL脚本的编码; 可能是{@code null}或<em>空</em>以指示平台编码
	 * @param scripts 要执行以初始化或清理数据库的脚本 (never {@code null})
	 */
	public ResourceDatabasePopulator(boolean continueOnError, boolean ignoreFailedDrops,
			String sqlScriptEncoding, Resource... scripts) {

		this(scripts);
		this.continueOnError = continueOnError;
		this.ignoreFailedDrops = ignoreFailedDrops;
		setSqlScriptEncoding(sqlScriptEncoding);
	}


	/**
	 * 添加要执行的脚本以初始化或清理数据库.
	 * 
	 * @param script SQL脚本的路径 (never {@code null})
	 */
	public void addScript(Resource script) {
		Assert.notNull(script, "Script must not be null");
		this.scripts.add(script);
	}

	/**
	 * 添加多个脚本以执行以初始化或清理数据库.
	 * 
	 * @param scripts 要执行的脚本 (never {@code null})
	 */
	public void addScripts(Resource... scripts) {
		assertContentsOfScriptArray(scripts);
		this.scripts.addAll(Arrays.asList(scripts));
	}

	/**
	 * 设置要执行的脚本以初始化或清理数据库, 替换以前添加的任何脚本.
	 * 
	 * @param scripts 要执行的脚本 (never {@code null})
	 */
	public void setScripts(Resource... scripts) {
		assertContentsOfScriptArray(scripts);
		// 确保列表可以修改
		this.scripts = new ArrayList<Resource>(Arrays.asList(scripts));
	}

	private void assertContentsOfScriptArray(Resource... scripts) {
		Assert.notNull(scripts, "Scripts array must not be null");
		Assert.noNullElements(scripts, "Scripts array must not contain null elements");
	}

	/**
	 * 如果与平台编码不同, 指定已配置的SQL脚本的编码.
	 * 
	 * @param sqlScriptEncoding 脚本中使用的编码; 可能是{@code null}或为空以指示平台编码
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = (StringUtils.hasText(sqlScriptEncoding) ? sqlScriptEncoding : null);
	}

	/**
	 * 指定语句分隔符, 如果要自定义分隔符.
	 * <p>默认为{@code ";"}, 如果未指定, 则作为最后的手段退回到{@code "\n"};
	 * 可以设置为{@link ScriptUtils#EOF_STATEMENT_SEPARATOR}以表示每个脚本包含没有分隔符的单个语句.
	 * 
	 * @param separator 脚本语句分隔符
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * 设置标识SQL脚本中单行注释的前缀.
	 * <p>默认{@code "--"}.
	 * 
	 * @param commentPrefix 单行注释的前缀
	 */
	public void setCommentPrefix(String commentPrefix) {
		this.commentPrefix = commentPrefix;
	}

	/**
	 * 设置标识SQL脚本中的块注释的起始分隔符.
	 * <p>默认 {@code "/*"}.
	 * 
	 * @param blockCommentStartDelimiter 块注释的起始分隔符 (never {@code null} or empty)
	 */
	public void setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
		Assert.hasText(blockCommentStartDelimiter, "BlockCommentStartDelimiter must not be null or empty");
		this.blockCommentStartDelimiter = blockCommentStartDelimiter;
	}

	/**
	 * 设置标识SQL脚本中的块注释的结束分隔符.
	 * <p>默认<code>"*&#47;"</code>.
	 * 
	 * @param blockCommentEndDelimiter 块注释的结束分隔符 (never {@code null} or empty)
	 */
	public void setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
		Assert.hasText(blockCommentEndDelimiter, "BlockCommentEndDelimiter must not be null or empty");
		this.blockCommentEndDelimiter = blockCommentEndDelimiter;
	}

	/**
	 * 指示应记录SQL中的所有故障但不会导致故障.
	 * <p>默认{@code false}.
	 * 
	 * @param continueOnError {@code true}如果脚本执行出错时应该继续
	 */
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	/**
	 * 指示可以忽略失败的SQL {@code DROP}语句.
	 * <p>这对于非嵌入式数据库非常有用, 该数据库的SQL方言不支持{@code DROP}语句中的{@code IF EXISTS}子句.
	 * <p>默认值为{@code false}, 这样如果填充程序意外运行, 如果脚本以{@code DROP}语句启动, 它将快速失败.
	 * 
	 * @param ignoreFailedDrops {@code true} 如果失败的drop语句应该被忽略
	 */
	public void setIgnoreFailedDrops(boolean ignoreFailedDrops) {
		this.ignoreFailedDrops = ignoreFailedDrops;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void populate(Connection connection) throws ScriptException {
		Assert.notNull(connection, "Connection must not be null");
		for (Resource script : this.scripts) {
			EncodedResource encodedScript = new EncodedResource(script, this.sqlScriptEncoding);
			ScriptUtils.executeSqlScript(connection, encodedScript, this.continueOnError, this.ignoreFailedDrops,
					this.commentPrefix, this.separator, this.blockCommentStartDelimiter, this.blockCommentEndDelimiter);
		}
	}

	/**
	 * 针对给定的{@link DataSource}执行此{@code ResourceDatabasePopulator}.
	 * <p>委托给{@link DatabasePopulatorUtils#execute}.
	 * 
	 * @param dataSource 要执行的{@code DataSource} (never {@code null})
	 * 
	 * @throws ScriptException 如果发生错误
	 */
	public void execute(DataSource dataSource) throws ScriptException {
		DatabasePopulatorUtils.execute(this, dataSource);
	}

}
