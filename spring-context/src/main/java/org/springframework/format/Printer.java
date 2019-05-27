package org.springframework.format;

import java.util.Locale;

/**
 * 打印T类型的对象以供显示.
 *
 * @param <T> 此Printer打印的对象类型
 */
public interface Printer<T> {

	/**
	 * 打印T类型的对象进行显示.
	 * 
	 * @param object 要打印的实例
	 * @param locale 当前用户区域设置
	 * 
	 * @return 打印的文本字符串
	 */
	String print(T object, Locale locale);

}
