package org.springframework.jdbc.object;

import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

/**
 * 操作对象表示基于SQL的操作, 例如查询或更新, 而不是存储过程.
 *
 * <p>根据声明的参数配置{@link org.springframework.jdbc.core.PreparedStatementCreatorFactory}.
 */
public abstract class SqlOperation extends RdbmsOperation {

	/**
	 * 能够根据此类的声明参数有效地创建PreparedStatementCreator的对象.
	 */
	private PreparedStatementCreatorFactory preparedStatementFactory;

	/** SQL语句的解析表示 */
	private ParsedSql cachedSql;

	/** 用于锁定已解析的SQL语句的缓存表示形式的监视器 */
	private final Object parsedSqlMonitor = new Object();


	/**
	 * 重写方法, 根据声明的参数配置PreparedStatementCreatorFactory.
	 */
	@Override
	protected final void compileInternal() {
		this.preparedStatementFactory = new PreparedStatementCreatorFactory(getSql(), getDeclaredParameters());
		this.preparedStatementFactory.setResultSetType(getResultSetType());
		this.preparedStatementFactory.setUpdatableResults(isUpdatableResults());
		this.preparedStatementFactory.setReturnGeneratedKeys(isReturnGeneratedKeys());
		if (getGeneratedKeysColumnNames() != null) {
			this.preparedStatementFactory.setGeneratedKeysColumnNames(getGeneratedKeysColumnNames());
		}
		this.preparedStatementFactory.setNativeJdbcExtractor(getJdbcTemplate().getNativeJdbcExtractor());

		onCompileInternal();
	}

	/**
	 * 子类可以覆盖到后处理编译的钩子方法.
	 * 此实现什么都不做.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * 获取此操作的SQL语句的已解析表示形式.
	 * <p>通常用于命名参数解析.
	 */
	protected ParsedSql getParsedSql() {
		synchronized (this.parsedSqlMonitor) {
			if (this.cachedSql == null) {
				this.cachedSql = NamedParameterUtils.parseSqlStatement(getSql());
			}
			return this.cachedSql;
		}
	}


	/**
	 * 返回使用给定参数执行操作的PreparedStatementSetter.
	 * 
	 * @param params 参数数组 (may be {@code null})
	 */
	protected final PreparedStatementSetter newPreparedStatementSetter(Object[] params) {
		return this.preparedStatementFactory.newPreparedStatementSetter(params);
	}

	/**
	 * 返回使用给定参数执行操作的PreparedStatementCreator.
	 * 
	 * @param params 参数数组 (may be {@code null})
	 */
	protected final PreparedStatementCreator newPreparedStatementCreator(Object[] params) {
		return this.preparedStatementFactory.newPreparedStatementCreator(params);
	}

	/**
	 * 返回使用给定参数执行操作的PreparedStatementCreator.
	 * 
	 * @param sqlToUse 要使用的实际SQL语句 (如果与工厂不同, 例如因为命名参数扩展)
	 * @param params 参数数组 (may be {@code null})
	 */
	protected final PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, Object[] params) {
		return this.preparedStatementFactory.newPreparedStatementCreator(sqlToUse, params);
	}
}
