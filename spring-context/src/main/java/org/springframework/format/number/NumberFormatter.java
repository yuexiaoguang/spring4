package org.springframework.format.number;

/**
 * 通用的数字格式化器.
 *
 * @deprecated as of Spring 4.2, in favor of the more clearly named
 * {@link NumberStyleFormatter}
 */
@Deprecated
public class NumberFormatter extends NumberStyleFormatter {

	public NumberFormatter() {
	}

	/**
	 * @param pattern 格式化模式
	 */
	public NumberFormatter(String pattern) {
		super(pattern);
	}

}
