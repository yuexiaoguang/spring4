package org.springframework.jdbc.datasource.init;

import org.springframework.core.io.support.EncodedResource;

/**
 * 如果SQL脚本中的语句在目标数据库执行失败, 则由{@link ScriptUtils}抛出.
 */
@SuppressWarnings("serial")
public class ScriptStatementFailedException extends ScriptException {

	/**
	 * @param stmt 失败的实际SQL语句
	 * @param stmtNumber SQL脚本中的语句编号 (i.e., 资源中存在的 n<sup>th</sup>语句)
	 * @param encodedResource 从中读取SQL语句的资源
	 * @param cause 失败的根本原因
	 */
	public ScriptStatementFailedException(String stmt, int stmtNumber, EncodedResource encodedResource, Throwable cause) {
		super(buildErrorMessage(stmt, stmtNumber, encodedResource), cause);
	}


	/**
	 * 根据提供的参数为SQL脚本执行失败构建错误消息.
	 * 
	 * @param stmt 失败的实际SQL语句
	 * @param stmtNumber SQL脚本中的语句编号 (i.e., 资源中存在的 n<sup>th</sup>语句)
	 * @param encodedResource 从中读取SQL语句的资源
	 * 
	 * @return 适用于异常的<em>详细信息</em>或日志记录的错误消息
	 */
	public static String buildErrorMessage(String stmt, int stmtNumber, EncodedResource encodedResource) {
		return String.format("Failed to execute SQL script statement #%s of %s: %s", stmtNumber, encodedResource, stmt);
	}
}
