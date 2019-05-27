package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.Assert;

/**
 * 一种构建器, 提供用于构建嵌入式数据库的便捷API.
 *
 * <h3>用法实例</h3>
 * <pre class="code">
 * EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
 *     .generateUniqueName(true)
 *     .setType(H2)
 *     .setScriptEncoding("UTF-8")
 *     .ignoreFailedDrops(true)
 *     .addScript("schema.sql")
 *     .addScripts("user_data.sql", "country_data.sql")
 *     .build();
 *
 * // perform actions against the db (EmbeddedDatabase extends javax.sql.DataSource)
 *
 * db.shutdown();
 * </pre>
 */
public class EmbeddedDatabaseBuilder {

	private final EmbeddedDatabaseFactory databaseFactory;

	private final ResourceDatabasePopulator databasePopulator;

	private final ResourceLoader resourceLoader;


	/**
	 * 使用{@link DefaultResourceLoader}创建新的嵌入式数据库构建器.
	 */
	public EmbeddedDatabaseBuilder() {
		this(new DefaultResourceLoader());
	}

	/**
	 * @param resourceLoader 要委托给的{@code ResourceLoader}
	 */
	public EmbeddedDatabaseBuilder(ResourceLoader resourceLoader) {
		this.databaseFactory = new EmbeddedDatabaseFactory();
		this.databasePopulator = new ResourceDatabasePopulator();
		this.databaseFactory.setDatabasePopulator(this.databasePopulator);
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 指定是否应生成唯一ID, 并将其用作数据库名称.
	 * <p>如果在单个JVM中跨多个应用程序上下文重用此构建器的配置, 则此标志应<em>启用</em> (i.e., 设置为{@code true}),
	 * 以确保每个应用程序上下文都有自己的嵌入式数据库.
	 * <p>启用此标志将覆盖通过{@link #setName}设置的任何显式名称.
	 * 
	 * @param flag {@code true} 如果应生成唯一的数据库名称
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder generateUniqueName(boolean flag) {
		this.databaseFactory.setGenerateUniqueDatabaseName(flag);
		return this;
	}

	/**
	 * 设置嵌入式数据库的名称.
	 * <p>如果未调用, 则默认为{@link EmbeddedDatabaseFactory#DEFAULT_DATABASE_NAME}.
	 * <p>如果{@code generateUniqueName}标志已设置为{@code true}, 将被覆盖.
	 * 
	 * @param databaseName 要构建的嵌入式数据库的名称
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setName(String databaseName) {
		this.databaseFactory.setDatabaseName(databaseName);
		return this;
	}

	/**
	 * 设置嵌入式数据库的类型.
	 * <p>如果未调用, 则默认为HSQL.
	 * 
	 * @param databaseType 要构建的嵌入式数据库的类型
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setType(EmbeddedDatabaseType databaseType) {
		this.databaseFactory.setDatabaseType(databaseType);
		return this;
	}

	/**
	 * 设置用于创建连接到嵌入式数据库的{@link DataSource}实例的工厂.
	 * <p>默认为{@link SimpleDriverDataSourceFactory}, 但可以覆盖, 例如引入连接池.
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
		this.databaseFactory.setDataSourceFactory(dataSourceFactory);
		return this;
	}

	/**
	 * 添加要执行的默认SQL脚本以填充数据库.
	 * <p>默认脚本是{@code "schema.sql"}来创建数据库模式, {@code "data.sql"}用数据填充数据库.
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder addDefaultScripts() {
		return addScripts("schema.sql", "data.sql");
	}

	/**
	 * 添加要执行的SQL脚本以初始化或填充数据库.
	 * 
	 * @param script 要执行的脚本
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder addScript(String script) {
		this.databasePopulator.addScript(this.resourceLoader.getResource(script));
		return this;
	}

	/**
	 * 添加要执行的多个SQL脚本以初始化或填充数据库.
	 * 
	 * @param scripts 要执行的脚本
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder addScripts(String... scripts) {
		for (String script : scripts) {
			addScript(script);
		}
		return this;
	}

	/**
	 * 如果与平台编码不同, 请指定所有SQL脚本中使用的字符编码.
	 * 
	 * @param scriptEncoding 脚本中使用的编码
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setScriptEncoding(String scriptEncoding) {
		this.databasePopulator.setSqlScriptEncoding(scriptEncoding);
		return this;
	}

	/**
	 * 指定所有SQL脚本中使用的语句分隔符, 如果是自定义分隔符.
	 * <p>默认为{@code ";"}, 如果未指定, 则作为最后的手段退回到 {@code "\n"};
	 * 可以设置为{@link ScriptUtils#EOF_STATEMENT_SEPARATOR}以表示每个脚本包含没有分隔符的单个语句.
	 * 
	 * @param separator 语句分隔符
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setSeparator(String separator) {
		this.databasePopulator.setSeparator(separator);
		return this;
	}

	/**
	 * 指定所有SQL脚本中使用的单行注释前缀.
	 * <p>默认{@code "--"}.
	 * 
	 * @param commentPrefix 单行注释的前缀
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setCommentPrefix(String commentPrefix) {
		this.databasePopulator.setCommentPrefix(commentPrefix);
		return this;
	}

	/**
	 * 指定所有SQL脚本中块注释的起始分隔符.
	 * <p>默认 {@code "/*"}.
	 * 
	 * @param blockCommentStartDelimiter 块注释的起始分隔符
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
		this.databasePopulator.setBlockCommentStartDelimiter(blockCommentStartDelimiter);
		return this;
	}

	/**
	 * 指定所有SQL脚本中块注释的结束分隔符.
	 * <p>默认<code>"*&#47;"</code>.
	 * 
	 * @param blockCommentEndDelimiter 块注释的结束分隔符
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
		this.databasePopulator.setBlockCommentEndDelimiter(blockCommentEndDelimiter);
		return this;
	}

	/**
	 * 指定应记录执行SQL脚本时发生的所有故障, 但不应导致故障.
	 * <p>默认{@code false}.
	 * 
	 * @param flag {@code true} 如果脚本执行出错时应该继续
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder continueOnError(boolean flag) {
		this.databasePopulator.setContinueOnError(flag);
		return this;
	}

	/**
	 * 指定可以忽略已执行脚本中的失败SQL {@code DROP}语句.
	 * <p>这对于SQL方言不支持{@code DROP}语句中的{@code IF EXISTS}子句的数据库很有用.
	 * <p>默认值为{@code false}, 因此如果脚本以{@code DROP}语句启动, {@link #build building}将快速失败.
	 * 
	 * @param flag {@code true} 如果失败的drop语句应该被忽略
	 * 
	 * @return {@code this}, 方便方法调用链
	 */
	public EmbeddedDatabaseBuilder ignoreFailedDrops(boolean flag) {
		this.databasePopulator.setIgnoreFailedDrops(flag);
		return this;
	}

	/**
	 * 构建嵌入式数据库.
	 * 
	 * @return 嵌入式数据库
	 */
	public EmbeddedDatabase build() {
		return this.databaseFactory.getDatabase();
	}
}
