package org.springframework.format;

import java.text.ParseException;
import java.util.Locale;

/**
 * 解析文本字符串以生成T的实例.
 *
 * @param <T> 这个Parser产生的对象类型
 */
public interface Parser<T> {

	/**
	 * 解析文本字符串以生成T.
	 * 
	 * @param text 文本字符串
	 * @param locale 当前用户区域设置
	 * 
	 * @return T的实例
	 * @throws ParseException 当java.text解析库中发生解析异常时
	 * @throws IllegalArgumentException 发生解析异常
	 */
	T parse(String text, Locale locale) throws ParseException;

}
