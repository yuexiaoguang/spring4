package org.springframework.test.context.jdbc;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.jdbc.SqlConfig.ErrorMode;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.util.Assert;

/**
 * {@code MergedSqlConfig}封装了通过{@link Sql#config}在本地声明的<em>合并</em> {@link SqlConfig @SqlConfig}属性,
 * 并在全局范围内作为类级注解.
 *
 * <p>显式本地配置属性覆盖全局配置属性.
 */
class MergedSqlConfig {

	private final String dataSource;

	private final String transactionManager;

	private final TransactionMode transactionMode;

	private final String encoding;

	private final String separator;

	private final String commentPrefix;

	private final String blockCommentStartDelimiter;

	private final String blockCommentEndDelimiter;

	private final ErrorMode errorMode;


	/**
	 * 通过将提供的本地(可能是方法级别) {@code @SqlConfig}注解中的配置
	 * 与在提供的{@code testClass}上发现的类级配置合并来构造{@code MergedSqlConfig}实例.
	 * <p>本地配置会覆盖类级配置.
	 * <p>如果测试类未使用{@code @SqlConfig}注解, 则不会发生合并, 并且"按原样"使用本地配置.
	 */
	MergedSqlConfig(SqlConfig localSqlConfig, Class<?> testClass) {
		Assert.notNull(localSqlConfig, "Local @SqlConfig must not be null");
		Assert.notNull(testClass, "testClass must not be null");

		// 获取全局属性.
		AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
				testClass, SqlConfig.class.getName(), false, false);

		// 使用本地属性覆盖全局属性.
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				Object value = AnnotationUtils.getValue(localSqlConfig, key);
				if (value != null) {
					// 值是否显式指定 (i.e., 不是'default')?
					if (!value.equals("") && value != TransactionMode.DEFAULT && value != ErrorMode.DEFAULT) {
						attributes.put(key, value);
					}
				}
			}
		}
		else {
			// 否则, 仅使用本地属性.
			attributes = AnnotationUtils.getAnnotationAttributes(localSqlConfig, false, false);
		}

		this.dataSource = attributes.getString("dataSource");
		this.transactionManager = attributes.getString("transactionManager");
		this.transactionMode = getEnum(attributes, "transactionMode", TransactionMode.DEFAULT, TransactionMode.INFERRED);
		this.encoding = attributes.getString("encoding");
		this.separator = getString(attributes, "separator", ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
		this.commentPrefix = getString(attributes, "commentPrefix", ScriptUtils.DEFAULT_COMMENT_PREFIX);
		this.blockCommentStartDelimiter = getString(attributes, "blockCommentStartDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER);
		this.blockCommentEndDelimiter = getString(attributes, "blockCommentEndDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		this.errorMode = getEnum(attributes, "errorMode", ErrorMode.DEFAULT, ErrorMode.FAIL_ON_ERROR);
	}

	/**
	 * @see SqlConfig#dataSource()
	 */
	String getDataSource() {
		return this.dataSource;
	}

	/**
	 * @see SqlConfig#transactionManager()
	 */
	String getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * @see SqlConfig#transactionMode()
	 */
	TransactionMode getTransactionMode() {
		return this.transactionMode;
	}

	/**
	 * @see SqlConfig#encoding()
	 */
	String getEncoding() {
		return this.encoding;
	}

	/**
	 * @see SqlConfig#separator()
	 */
	String getSeparator() {
		return this.separator;
	}

	/**
	 * @see SqlConfig#commentPrefix()
	 */
	String getCommentPrefix() {
		return this.commentPrefix;
	}

	/**
	 * @see SqlConfig#blockCommentStartDelimiter()
	 */
	String getBlockCommentStartDelimiter() {
		return this.blockCommentStartDelimiter;
	}

	/**
	 * @see SqlConfig#blockCommentEndDelimiter()
	 */
	String getBlockCommentEndDelimiter() {
		return this.blockCommentEndDelimiter;
	}

	/**
	 * @see SqlConfig#errorMode()
	 */
	ErrorMode getErrorMode() {
		return this.errorMode;
	}

	/**
	 * 合并的SQL脚本配置的字符串表示.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("dataSource", this.dataSource)
				.append("transactionManager", this.transactionManager)
				.append("transactionMode", this.transactionMode)
				.append("encoding", this.encoding)
				.append("separator", this.separator)
				.append("commentPrefix", this.commentPrefix)
				.append("blockCommentStartDelimiter", this.blockCommentStartDelimiter)
				.append("blockCommentEndDelimiter", this.blockCommentEndDelimiter)
				.append("errorMode", this.errorMode)
				.toString();
	}


	private static <E extends Enum<?>> E getEnum(AnnotationAttributes attributes, String attributeName,
			E inheritedOrDefaultValue, E defaultValue) {

		E value = attributes.getEnum(attributeName);
		if (value == inheritedOrDefaultValue) {
			value = defaultValue;
		}
		return value;
	}

	private static String getString(AnnotationAttributes attributes, String attributeName, String defaultValue) {
		String value = attributes.getString(attributeName);
		if ("".equals(value)) {
			value = defaultValue;
		}
		return value;
	}

}
