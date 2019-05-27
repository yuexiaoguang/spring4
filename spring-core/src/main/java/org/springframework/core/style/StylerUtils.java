package org.springframework.core.style;

/**
 * 简单的实用程序类, 允许方便地访问值样式逻辑, 主要用于支持描述性日志消息.
 *
 * <p>对于更复杂的需求, 直接使用{@link ValueStyler}抽象.
 * 该类只使用下面的共享{@link DefaultValueStyler}实例.
 */
public abstract class StylerUtils {

	/**
	 * {@code style}方法使用的默认ValueStyler实例.
	 * 也可用于此包中的{@link ToStringCreator}类.
	 */
	static final ValueStyler DEFAULT_VALUE_STYLER = new DefaultValueStyler();

	/**
	 * 根据默认约定, 设置指定值的样式.
	 * 
	 * @param value 要设置样式的Object值
	 * 
	 * @return 样式
	 */
	public static String style(Object value) {
		return DEFAULT_VALUE_STYLER.style(value);
	}

}
