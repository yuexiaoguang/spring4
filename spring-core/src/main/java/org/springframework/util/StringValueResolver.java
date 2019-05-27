package org.springframework.util;

/**
 * 用于解析String值的简单策略接口.
 * 由{@link org.springframework.beans.factory.config.ConfigurableBeanFactory}使用.
 */
public interface StringValueResolver {

	/**
	 * 解析给定的String值, 例如解析占位符.
	 * 
	 * @param strVal 原始String值 (never {@code null})
	 * 
	 * @return 已解析的String值 (解析为空值时可能为{@code null}),
	 * 可能是原始String值本身 (如果没有占位符要解析或忽略不可解析的占位符)
	 * @throws IllegalArgumentException 如果是无法解析的String值
	 */
	String resolveStringValue(String strVal);

}
