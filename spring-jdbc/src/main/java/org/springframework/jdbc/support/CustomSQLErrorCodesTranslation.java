package org.springframework.jdbc.support;

import org.springframework.dao.DataAccessException;
import org.springframework.util.StringUtils;

/**
 * 用于保存特定数据库的自定义JDBC错误代码转换的JavaBean.
 * "exceptionClass"属性定义将为errorCodes属性中指定的错误代码列表抛出哪个异常.
 */
public class CustomSQLErrorCodesTranslation {

	private String[] errorCodes = new String[0];

	private Class<?> exceptionClass;


	/**
	 * 设置要匹配的SQL错误代码.
	 */
	public void setErrorCodes(String... errorCodes) {
		this.errorCodes = StringUtils.sortStringArray(errorCodes);
	}

	/**
	 * 返回要匹配的SQL错误代码.
	 */
	public String[] getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * 设置指定错误代码的异常类.
	 */
	public void setExceptionClass(Class<?> exceptionClass) {
		if (!DataAccessException.class.isAssignableFrom(exceptionClass)) {
			throw new IllegalArgumentException("Invalid exception class [" + exceptionClass +
					"]: needs to be a subclass of [org.springframework.dao.DataAccessException]");
		}
		this.exceptionClass = exceptionClass;
	}

	/**
	 * 返回指定错误代码的异常类.
	 */
	public Class<?> getExceptionClass() {
		return this.exceptionClass;
	}

}
