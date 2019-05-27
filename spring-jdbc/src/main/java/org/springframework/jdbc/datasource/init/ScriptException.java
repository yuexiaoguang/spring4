package org.springframework.jdbc.datasource.init;

import org.springframework.dao.DataAccessException;

/**
 * 与SQL脚本处理相关的数据访问异常层次结构的根.
 */
@SuppressWarnings("serial")
public abstract class ScriptException extends DataAccessException {

	public ScriptException(String message) {
		super(message);
	}

	public ScriptException(String message, Throwable cause) {
		super(message, cause);
	}
}
