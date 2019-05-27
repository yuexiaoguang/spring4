package org.springframework.core.style;

import org.springframework.util.Assert;

/**
 * 使用可插入样式约定, 构建漂亮打印{@code toString()}方法的实用程序类.
 * 默认情况下, ToStringCreator 遵循Spring的{@code toString()}样式约定.
 */
public class ToStringCreator {

	/**
	 * 此ToStringCreator使用的默认ToStringStyler实例.
	 */
	private static final ToStringStyler DEFAULT_TO_STRING_STYLER =
			new DefaultToStringStyler(StylerUtils.DEFAULT_VALUE_STYLER);


	private final StringBuilder buffer = new StringBuilder(256);

	private final ToStringStyler styler;

	private final Object object;

	private boolean styledFirstField;


	/**
	 * @param obj 要进行字符串化的对象
	 */
	public ToStringCreator(Object obj) {
		this(obj, (ToStringStyler) null);
	}

	/**
	 * @param obj 要进行字符串化的对象
	 * @param styler 封装了漂亮的打印指令的ValueStyler
	 */
	public ToStringCreator(Object obj, ValueStyler styler) {
		this(obj, new DefaultToStringStyler(styler != null ? styler : StylerUtils.DEFAULT_VALUE_STYLER));
	}

	/**
	 * @param obj 要进行字符串化的对象
	 * @param styler 封装了漂亮的打印指令的ToStringStyler
	 */
	public ToStringCreator(Object obj, ToStringStyler styler) {
		Assert.notNull(obj, "The object to be styled must not be null");
		this.object = obj;
		this.styler = (styler != null ? styler : DEFAULT_TO_STRING_STYLER);
		this.styler.styleStart(this.buffer, this.object);
	}


	/**
	 * 附加byte字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, byte value) {
		return append(fieldName, Byte.valueOf(value));
	}

	/**
	 * 附加short字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, short value) {
		return append(fieldName, Short.valueOf(value));
	}

	/**
	 * 附加int字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, int value) {
		return append(fieldName, Integer.valueOf(value));
	}

	/**
	 * 附加long字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, long value) {
		return append(fieldName, Long.valueOf(value));
	}

	/**
	 * 附加float字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, float value) {
		return append(fieldName, Float.valueOf(value));
	}

	/**
	 * 附加double字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, double value) {
		return append(fieldName, Double.valueOf(value));
	}

	/**
	 * 附加boolean字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, boolean value) {
		return append(fieldName, Boolean.valueOf(value));
	}

	/**
	 * 附加字段值.
	 * 
	 * @param fieldName 字段的名称, 通常是成员变量名称
	 * @param value 字段值
	 * 
	 * @return this, 支持调用链
	 */
	public ToStringCreator append(String fieldName, Object value) {
		printFieldSeparatorIfNecessary();
		this.styler.styleField(this.buffer, fieldName, value);
		return this;
	}

	private void printFieldSeparatorIfNecessary() {
		if (this.styledFirstField) {
			this.styler.styleFieldSeparator(this.buffer);
		}
		else {
			this.styledFirstField = true;
		}
	}

	/**
	 * 附加提供的值.
	 * 
	 * @param value 要追加的值
	 * 
	 * @return this, 支持调用链.
	 */
	public ToStringCreator append(Object value) {
		this.styler.styleValue(this.buffer, value);
		return this;
	}


	@Override
	public String toString() {
		this.styler.styleEnd(this.buffer, this.object);
		return this.buffer.toString();
	}
}
