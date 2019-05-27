package org.springframework.core.style;

/**
 * 用于漂亮打印{@code toString()}方法的策略接口.
 * 封装打印算法; 其他一些对象(如构建器)应提供工作流程.
 */
public interface ToStringStyler {

	/**
	 * 在为其字段设置样式之前设置{@code toString()}的对象的样式.
	 * 
	 * @param buffer 要打印的缓冲区
	 * @param obj 要设置样式的对象
	 */
	void styleStart(StringBuilder buffer, Object obj);

	/**
	 * 在设置字段样式后设置{@code toString()}的对象的样式.
	 * 
	 * @param buffer 要打印的缓冲区
	 * @param obj 要设置样式的对象
	 */
	void styleEnd(StringBuilder buffer, Object obj);

	/**
	 * 设置字段值的样式.
	 * 
	 * @param buffer 要打印的缓冲区
	 * @param fieldName 字段名称
	 * @param value 字段值
	 */
	void styleField(StringBuilder buffer, String fieldName, Object value);

	/**
	 * 设置给定值的样式.
	 * 
	 * @param buffer 要打印的缓冲区
	 * @param value 字段值
	 */
	void styleValue(StringBuilder buffer, Object value);

	/**
	 * 设置字段分隔符的样式.
	 * 
	 * @param buffer 要打印的缓冲区
	 */
	void styleFieldSeparator(StringBuilder buffer);

}
