package org.springframework.jdbc.support.xml;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * 当底层实现不支持API的请求功能时抛出异常.
 */
@SuppressWarnings("serial")
public class SqlXmlFeatureNotImplementedException extends InvalidDataAccessApiUsageException {

	public SqlXmlFeatureNotImplementedException(String msg) {
		super(msg);
	}

	public SqlXmlFeatureNotImplementedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
