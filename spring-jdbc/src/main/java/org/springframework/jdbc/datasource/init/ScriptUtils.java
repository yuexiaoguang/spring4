package org.springframework.jdbc.datasource.init;

import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用SQL脚本的通用工具方法.
 *
 * <p>主要供框架内部使用.
 */
public abstract class ScriptUtils {

	/**
	 * SQL脚本中的默认语句分隔符: {@code ";"}.
	 */
	public static final String DEFAULT_STATEMENT_SEPARATOR = ";";

	/**
	 * SQL脚本中的回退语句分隔符: {@code "\n"}.
	 * <p>如果给定脚本中既没有自定义分隔符也没有{@link #DEFAULT_STATEMENT_SEPARATOR}, 则使用.
	 */
	public static final String FALLBACK_STATEMENT_SEPARATOR = "\n";

	/**
	 * 文件结束 (EOF) SQL语句分隔符: {@code "^^^ END OF SCRIPT ^^^"}.
	 * <p>此值可以作为{@code separator}提供给
	 * {@link #executeSqlScript(Connection, EncodedResource, boolean, boolean, String, String, String, String)},
	 * 以表示SQL脚本包含单个语句(可能跨越多行), 没有明确的语句分隔符.
	 * 请注意, 此类脚本实际上不应包含此值; 它只是一个<em>虚拟</em>语句分隔符.
	 */
	public static final String EOF_STATEMENT_SEPARATOR = "^^^ END OF SCRIPT ^^^";

	/**
	 * SQL脚本中单行注释的默认前缀: {@code "--"}.
	 */
	public static final String DEFAULT_COMMENT_PREFIX = "--";

	/**
	 * SQL脚本中块注释的默认启动分隔符: {@code "/*"}.
	 */
	public static final String DEFAULT_BLOCK_COMMENT_START_DELIMITER = "/*";

	/**
	 * SQL脚本中块注释的默认结束分隔符: <code>"*&#47;"</code>.
	 */
	public static final String DEFAULT_BLOCK_COMMENT_END_DELIMITER = "*/";


	private static final Log logger = LogFactory.getLog(ScriptUtils.class);


	/**
	 * 将SQL脚本拆分为由提供的分隔符分隔的单独语句.
	 * 每个单独的声明都将添加到提供的{@code List}中.
	 * <p>在脚本中, {@value #DEFAULT_COMMENT_PREFIX}将用作注释前缀;
	 * 任何以注释前缀开头并延伸到行尾的文本都将从输出中省略.
	 * 同样, {@value #DEFAULT_BLOCK_COMMENT_START_DELIMITER}和{@value #DEFAULT_BLOCK_COMMENT_END_DELIMITER}
	 * 将用作<em>start</em>和<em>end</em>块注释分隔符:
	 * 块注释中包含的任何文本都将从输出中省略.
	 * 此外, 多个相邻的空白字符将折叠为单个空格.
	 * 
	 * @param script SQL脚本
	 * @param separator 分隔每个语句的字符 &mdash; 通常是 ';'
	 * @param statements 包含单个语句的列表
	 * 
	 * @throws ScriptException 如果在拆分SQL脚本时发生错误
	 */
	public static void splitSqlScript(String script, char separator, List<String> statements) throws ScriptException {
		splitSqlScript(script, String.valueOf(separator), statements);
	}

	/**
	 * 将SQL脚本拆分为由提供的分隔符分隔的单独语句.
	 * 每个单独的声明都将添加到提供的{@code List}中.
	 * <p>在脚本中, {@value #DEFAULT_COMMENT_PREFIX}将用作注释前缀;
	 * 任何以注释前缀开头并延伸到行尾的文本都将从输出中省略.
	 * 同样, {@value #DEFAULT_BLOCK_COMMENT_START_DELIMITER}和{@value #DEFAULT_BLOCK_COMMENT_END_DELIMITER}
	 * 将用作<em>start</em>和<em>end</em>块注释分隔符:
	 * 块注释中包含的任何文本都将从输出中省略.
	 * 此外, 多个相邻的空白字符将折叠为单个空格.
	 * 
	 * @param script SQL脚本
	 * @param separator 分隔每个语句的字符 &mdash; 通常是 ';' 或换行符
	 * @param statements 包含单个语句的列表
	 * 
	 * @throws ScriptException 如果在拆分SQL脚本时发生错误
	 */
	public static void splitSqlScript(String script, String separator, List<String> statements) throws ScriptException {
		splitSqlScript(null, script, separator, DEFAULT_COMMENT_PREFIX, DEFAULT_BLOCK_COMMENT_START_DELIMITER,
				DEFAULT_BLOCK_COMMENT_END_DELIMITER, statements);
	}

	/**
	 * 将SQL脚本拆分为由提供的分隔符分隔的单独语句.
	 * 每个单独的声明都将添加到提供的{@code List}中.
	 * <p>在脚本中, 所提供的{@code commentPrefix}将受到尊重:
	 * 任何以注释前缀开头并延伸到行尾的文本都将从输出中省略.
	 * 同样, 提供的{@code blockCommentStartDelimiter}和{@code blockCommentEndDelimiter}分隔符将受到尊重:
	 * 块注释中包含的任何文本都将从输出中省略.
	 * 此外, 多个相邻的空白字符将折叠为单个空格.
	 * 
	 * @param resource 从中读取脚本的资源
	 * @param script SQL脚本; never {@code null} or empty
	 * @param separator 分隔每个语句的文字 &mdash; 通常是 ';' 或换行符; never {@code null}
	 * @param commentPrefix 标识SQL行注释的前缀 &mdash; 通常是 "--"; never {@code null} or empty
	 * @param blockCommentStartDelimiter <em>start</em>块注释分隔符; never {@code null} or empty
	 * @param blockCommentEndDelimiter <em>end</em>块注释分隔符; never {@code null} or empty
	 * @param statements 包含单个语句的列表
	 * 
	 * @throws ScriptException 如果在拆分SQL脚本时发生错误
	 */
	public static void splitSqlScript(EncodedResource resource, String script, String separator, String commentPrefix,
			String blockCommentStartDelimiter, String blockCommentEndDelimiter, List<String> statements)
			throws ScriptException {

		Assert.hasText(script, "'script' must not be null or empty");
		Assert.notNull(separator, "'separator' must not be null");
		Assert.hasText(commentPrefix, "'commentPrefix' must not be null or empty");
		Assert.hasText(blockCommentStartDelimiter, "'blockCommentStartDelimiter' must not be null or empty");
		Assert.hasText(blockCommentEndDelimiter, "'blockCommentEndDelimiter' must not be null or empty");

		StringBuilder sb = new StringBuilder();
		boolean inSingleQuote = false;
		boolean inDoubleQuote = false;
		boolean inEscape = false;

		for (int i = 0; i < script.length(); i++) {
			char c = script.charAt(i);
			if (inEscape) {
				inEscape = false;
				sb.append(c);
				continue;
			}
			// MySQL style escapes
			if (c == '\\') {
				inEscape = true;
				sb.append(c);
				continue;
			}
			if (!inDoubleQuote && (c == '\'')) {
				inSingleQuote = !inSingleQuote;
			}
			else if (!inSingleQuote && (c == '"')) {
				inDoubleQuote = !inDoubleQuote;
			}
			if (!inSingleQuote && !inDoubleQuote) {
				if (script.startsWith(separator, i)) {
					// 已经到了当前声明的末尾
					if (sb.length() > 0) {
						statements.add(sb.toString());
						sb = new StringBuilder();
					}
					i += separator.length() - 1;
					continue;
				}
				else if (script.startsWith(commentPrefix, i)) {
					// 跳过注释开头的任何内容到EOL
					int indexOfNextNewline = script.indexOf('\n', i);
					if (indexOfNextNewline > i) {
						i = indexOfNextNewline;
						continue;
					}
					else {
						// 如果没有EOL, 必须在脚本的末尾, 所以停在这里.
						break;
					}
				}
				else if (script.startsWith(blockCommentStartDelimiter, i)) {
					// 跳过任何块注释
					int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
					if (indexOfCommentEnd > i) {
						i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
						continue;
					}
					else {
						throw new ScriptParseException(
								"Missing block comment end delimiter: " + blockCommentEndDelimiter, resource);
					}
				}
				else if (c == ' ' || c == '\n' || c == '\t') {
					// 避免使用多个相邻的空白字符
					if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
						c = ' ';
					}
					else {
						continue;
					}
				}
			}
			sb.append(c);
		}

		if (StringUtils.hasText(sb)) {
			statements.add(sb.toString());
		}
	}

	/**
	 * 从给定资源中读取脚本, 使用"{@code --}"作为注释前缀, 使用"{@code ;}"作为语句分隔符, 并构建包含这些行的String.
	 * 
	 * @param resource 要读取的{@code EncodedResource}
	 * 
	 * @return 包含脚本行的{@code String}
	 * @throws IOException 在I/O错误的情况下
	 */
	static String readScript(EncodedResource resource) throws IOException {
		return readScript(resource, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR);
	}

	/**
	 * 使用提供的注释前缀和语句分隔符, 从提供的资源中读取脚本, 并构建包含这些行的{@code String}.
	 * <p>具有注释前缀的<em>开头</em>的行将从结果中排除;
	 * 但是, 其它地方的行注释 &mdash; 例如, 在声明中 &mdash; 将包含在结果中.
	 * 
	 * @param resource 包含要处理的脚本的{@code EncodedResource}
	 * @param commentPrefix 标识SQL脚本中注释的前缀 &mdash; 通常是 "--"
	 * @param separator SQL脚本中的语句分隔符 &mdash; 通常是 ";"
	 * 
	 * @return 包含脚本行的{@code String}
	 * @throws IOException 在I/O错误的情况下
	 */
	private static String readScript(EncodedResource resource, String commentPrefix, String separator)
			throws IOException {

		LineNumberReader lnr = new LineNumberReader(resource.getReader());
		try {
			return readScript(lnr, commentPrefix, separator);
		}
		finally {
			lnr.close();
		}
	}

	/**
	 * 使用提供的注释前缀和语句分隔符从提供的{@code LineNumberReader}中读取脚本，并构建包含这些行的{@code String}.
	 * <p>具有注释前缀的<em>开头</em>的行将从结果中排除;
	 * 但是, 其它地方的行注释 &mdash; 例如, 在声明中 &mdash; 将包含在结果中.
	 * 
	 * @param lineNumberReader 包含要处理的脚本的{@code LineNumberReader}
	 * @param commentPrefix 标识SQL脚本中注释的前缀 &mdash; 通常是 "--"
	 * @param separator SQL脚本中的语句分隔符 &mdash; 通常是 ";"
	 * 
	 * @return 包含脚本行的{@code String}
	 * @throws IOException 在I/O错误的情况下
	 */
	public static String readScript(LineNumberReader lineNumberReader, String commentPrefix, String separator)
			throws IOException {

		String currentStatement = lineNumberReader.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (commentPrefix != null && !currentStatement.startsWith(commentPrefix)) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lineNumberReader.readLine();
		}
		appendSeparatorToScriptIfNecessary(scriptBuilder, separator);
		return scriptBuilder.toString();
	}

	private static void appendSeparatorToScriptIfNecessary(StringBuilder scriptBuilder, String separator) {
		if (separator == null) {
			return;
		}
		String trimmed = separator.trim();
		if (trimmed.length() == separator.length()) {
			return;
		}
		// 分隔符以空格结尾, 因此我们可能希望查看脚本是否尝试以相同的方式结束
		if (scriptBuilder.lastIndexOf(trimmed) == scriptBuilder.length() - trimmed.length()) {
			scriptBuilder.append(separator.substring(trimmed.length()));
		}
	}

	/**
	 * 提供的SQL脚本是否包含指定的分隔符?
	 * 
	 * @param script SQL脚本
	 * @param delim 分隔每个语句的字符串 - 通常是';'
	 */
	public static boolean containsSqlScriptDelimiters(String script, String delim) {
		boolean inLiteral = false;
		for (int i = 0; i < script.length(); i++) {
			if (script.charAt(i) == '\'') {
				inLiteral = !inLiteral;
			}
			if (!inLiteral && script.startsWith(delim, i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 使用语句分隔符, 注释分隔符和异常处理标志的缺省设置执行给定的SQL脚本.
	 * <p>在提供的脚本中执行单个语句之前, 将删除语句分隔符和注释.
	 * <p><strong>Warning</strong>: 此方法<em>不</em>释放提供的{@link Connection}.
	 * 
	 * @param connection 用于执行脚本的JDBC连接; 已配置并可以使用
	 * @param resource 从中加载SQL脚本的资源; 使用当前平台的默认编码进行编码
	 * 
	 * @throws ScriptException 如果在执行SQL脚本时发生错误
	 */
	public static void executeSqlScript(Connection connection, Resource resource) throws ScriptException {
		executeSqlScript(connection, new EncodedResource(resource));
	}

	/**
	 * 使用语句分隔符, 注释分隔符和异常处理标志的缺省设置执行给定的SQL脚本.
	 * <p>在提供的脚本中执行单个语句之前, 将删除语句分隔符和注释.
	 * <p><strong>Warning</strong>: 此方法<em>不</em>释放提供的{@link Connection}.
	 * 
	 * @param connection 用于执行脚本的JDBC连接; 已配置并可以使用
	 * @param resource 从中加载SQL脚本的资源 (可能与特定编码相关联)
	 * 
	 * @throws ScriptException 如果在执行SQL脚本时发生错误
	 */
	public static void executeSqlScript(Connection connection, EncodedResource resource) throws ScriptException {
		executeSqlScript(connection, resource, false, false, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR,
				DEFAULT_BLOCK_COMMENT_START_DELIMITER, DEFAULT_BLOCK_COMMENT_END_DELIMITER);
	}

	/**
	 * 执行给定的SQL脚本.
	 * <p>在提供的脚本中执行单个语句之前, 将删除语句分隔符和注释.
	 * <p><strong>Warning</strong>: 此方法<em>不</em>释放提供的{@link Connection}.
	 * 
	 * @param connection 用于执行脚本的JDBC连接; 已配置并可以使用
	 * @param resource 从中加载SQL脚本的资源 (可能与特定编码相关联)
	 * @param continueOnError 是否在发生错误时继续而不抛出异常
	 * @param ignoreFailedDrops 是否在{@code DROP}语句中发生特定错误时继续
	 * @param commentPrefix 标识SQL脚本中单行注释的前缀 &mdash; 通常是 "--"
	 * @param separator 脚本语句分隔符;
	 * 如果未指定, 则默认为{@value #DEFAULT_STATEMENT_SEPARATOR}, 并作为最后的手段回退到{@value #FALLBACK_STATEMENT_SEPARATOR};
	 * 可以设置为{@value #EOF_STATEMENT_SEPARATOR}以表示脚本包含没有分隔符的单个语句
	 * @param blockCommentStartDelimiter <em>start</em>块注释分隔符; never {@code null} or empty
	 * @param blockCommentEndDelimiter <em>end</em>块注释分隔符; never {@code null} or empty
	 * 
	 * @throws ScriptException 如果在执行SQL脚本时发生错误
	 */
	public static void executeSqlScript(Connection connection, EncodedResource resource, boolean continueOnError,
			boolean ignoreFailedDrops, String commentPrefix, String separator, String blockCommentStartDelimiter,
			String blockCommentEndDelimiter) throws ScriptException {

		try {
			if (logger.isInfoEnabled()) {
				logger.info("Executing SQL script from " + resource);
			}
			long startTime = System.currentTimeMillis();

			String script;
			try {
				script = readScript(resource, commentPrefix, separator);
			}
			catch (IOException ex) {
				throw new CannotReadScriptException(resource, ex);
			}

			if (separator == null) {
				separator = DEFAULT_STATEMENT_SEPARATOR;
			}
			if (!EOF_STATEMENT_SEPARATOR.equals(separator) && !containsSqlScriptDelimiters(script, separator)) {
				separator = FALLBACK_STATEMENT_SEPARATOR;
			}

			List<String> statements = new LinkedList<String>();
			splitSqlScript(resource, script, separator, commentPrefix, blockCommentStartDelimiter,
					blockCommentEndDelimiter, statements);

			int stmtNumber = 0;
			Statement stmt = connection.createStatement();
			try {
				for (String statement : statements) {
					stmtNumber++;
					try {
						stmt.execute(statement);
						int rowsAffected = stmt.getUpdateCount();
						if (logger.isDebugEnabled()) {
							logger.debug(rowsAffected + " returned as update count for SQL: " + statement);
							SQLWarning warningToLog = stmt.getWarnings();
							while (warningToLog != null) {
								logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() +
										"', error code '" + warningToLog.getErrorCode() +
										"', message [" + warningToLog.getMessage() + "]");
								warningToLog = warningToLog.getNextWarning();
							}
						}
					}
					catch (SQLException ex) {
						boolean dropStatement = StringUtils.startsWithIgnoreCase(statement.trim(), "drop");
						if (continueOnError || (dropStatement && ignoreFailedDrops)) {
							if (logger.isDebugEnabled()) {
								logger.debug(ScriptStatementFailedException.buildErrorMessage(statement, stmtNumber, resource), ex);
							}
						}
						else {
							throw new ScriptStatementFailedException(statement, stmtNumber, resource, ex);
						}
					}
				}
			}
			finally {
				try {
					stmt.close();
				}
				catch (Throwable ex) {
					logger.debug("Could not close JDBC Statement", ex);
				}
			}

			long elapsedTime = System.currentTimeMillis() - startTime;
			if (logger.isInfoEnabled()) {
				logger.info("Executed SQL script from " + resource + " in " + elapsedTime + " ms.");
			}
		}
		catch (Exception ex) {
			if (ex instanceof ScriptException) {
				throw (ScriptException) ex;
			}
			throw new UncategorizedScriptException(
				"Failed to execute database script from resource [" + resource + "]", ex);
		}
	}
}
