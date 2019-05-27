package org.springframework.jdbc.support;

import org.springframework.core.NestedCheckedException;

/**
 * 在JDBC元数据查找期间出现问题.
 *
 * <p>这是一个受检异常, 因为希望它被捕获, 记录和处理, 而不是导致应用程序失败.
 * 无法读取JDBC元数据通常不是致命的问题.
 */
@SuppressWarnings("serial")
public class MetaDataAccessException extends NestedCheckedException {

	public MetaDataAccessException(String msg) {
		super(msg);
	}

	public MetaDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
