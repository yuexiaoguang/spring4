package org.springframework.jdbc.core.namedparam;

import org.springframework.jdbc.support.JdbcUtils;

/**
 * 为可以为命名SQL参数提供参数值的对象定义通用功能的接口, 用作{@link NamedParameterJdbcTemplate}操作的参数.
 *
 * <p>除了参数值之外, 此接口还允许指定SQL类型.
 * 通过指定参数的名称来标识所有参数值和类型.
 *
 * <p>旨在使用一致的接口包装各种实现, 如Map或JavaBean.
 */
public interface SqlParameterSource {

	/**
	 * 指示未知 (或未指定)SQL类型的常量.
	 * 当没有已知的特定SQL类型时, 从{@code getType}返回.
	 */
	int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;


	/**
	 * 确定指定的命名参数是否存在值.
	 * 
	 * @param paramName 参数的名称
	 * 
	 * @return 是否有定义的值
	 */
	boolean hasValue(String paramName);

	/**
	 * 返回请求的命名参数的参数值.
	 * 
	 * @param paramName 参数的名称
	 * 
	 * @return 指定参数的值
	 * @throws IllegalArgumentException 如果请求的参数没有值
	 */
	Object getValue(String paramName) throws IllegalArgumentException;

	/**
	 * 确定指定命名参数的SQL类型.
	 * 
	 * @param paramName 参数的名称
	 * 
	 * @return 指定参数的SQL类型, 或{@code TYPE_UNKNOWN}
	 */
	int getSqlType(String paramName);

	/**
	 * 确定指定命名参数的类型名称.
	 * 
	 * @param paramName 参数的名称
	 * 
	 * @return 指定参数的类型名称, 或{@code null}
	 */
	String getTypeName(String paramName);

}
