package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 自定义{@link java.beans.PropertyEditor}, 用于String数组.
 *
 * <p>字符串必须为CSV格式, 并带有可自定义的分隔符.
 * 默认情况下, 结果中的值会被修剪空格.
 */
public class StringArrayPropertyEditor extends PropertyEditorSupport {

	/**
	 * 用于拆分String的默认分隔符: a comma (",")
	 */
	public static final String DEFAULT_SEPARATOR = ",";


	private final String separator;

	private final String charsToDelete;

	private final boolean emptyArrayAsNull;

	private final boolean trimValues;


	/**
	 * 使用默认分隔符 (逗号)创建一个新的StringArrayPropertyEditor.
	 * <p>空文本 (没有元素) 将变为空数组.
	 */
	public StringArrayPropertyEditor() {
		this(DEFAULT_SEPARATOR, null, false);
	}

	/**
	 * 使用给定的分隔符创建一个新的StringArrayPropertyEditor.
	 * <p>空文本 (没有元素) 将变为空数组.
	 * 
	 * @param separator 用于拆分{@link String}的分隔符
	 */
	public StringArrayPropertyEditor(String separator) {
		this(separator, null, false);
	}

	/**
	 * 使用给定的分隔符创建一个新的StringArrayPropertyEditor.
	 * 
	 * @param separator 用于拆分{@link String}的分隔符
	 * @param emptyArrayAsNull {@code true} 如果要将一个空的String数组转换为{@code null}
	 */
	public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull) {
		this(separator, null, emptyArrayAsNull);
	}

	/**
	 * 使用给定的分隔符创建一个新的StringArrayPropertyEditor.
	 * 
	 * @param separator 用于拆分{@link String}的分隔符
	 * @param emptyArrayAsNull {@code true} 如果要将一个空的String数组转换为{@code null}
	 * @param trimValues {@code true} 如果要解析的数组中的值要去掉首尾空格 (默认true).
	 */
	public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull, boolean trimValues) {
		this(separator, null, emptyArrayAsNull, trimValues);
	}

	/**
	 * 使用给定的分隔符创建一个新的StringArrayPropertyEditor.
	 * 
	 * @param separator 用于拆分{@link String}的分隔符
	 * @param charsToDelete 除了修剪输入字符串外, 还要删除一组字符.
	 * 用于删除不需要的换行符:
	 * e.g. "\r\n\f" 将删除String中的所有新行和换行符.
	 * @param emptyArrayAsNull {@code true} 如果要将一个空的String数组转换为{@code null}
	 */
	public StringArrayPropertyEditor(String separator, String charsToDelete, boolean emptyArrayAsNull) {
		this(separator, charsToDelete, emptyArrayAsNull, true);
	}

	/**
	 * 使用给定的分隔符创建一个新的StringArrayPropertyEditor.
	 * 
	 * @param separator 用于拆分{@link String}的分隔符
	 * @param charsToDelete 除了修剪输入字符串外, 还要删除一组字符.
	 * 用于删除不需要的换行符:
	 * e.g. "\r\n\f" 将删除String中的所有新行和换行符.
	 * @param emptyArrayAsNull {@code true} 如果要将一个空的String数组转换为{@code null}
	 * @param trimValues {@code true} 如果要解析的数组中的值要去掉首尾空格 (默认true).
	 */
	public StringArrayPropertyEditor(String separator, String charsToDelete, boolean emptyArrayAsNull, boolean trimValues) {
		this.separator = separator;
		this.charsToDelete = charsToDelete;
		this.emptyArrayAsNull = emptyArrayAsNull;
		this.trimValues = trimValues;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] array = StringUtils.delimitedListToStringArray(text, this.separator, this.charsToDelete);
		if (trimValues) {
			array = StringUtils.trimArrayElements(array);
		}
		if (this.emptyArrayAsNull && array.length == 0) {
			setValue(null);
		}
		else {
			setValue(array);
		}
	}

	@Override
	public String getAsText() {
		return StringUtils.arrayToDelimitedString(ObjectUtils.toObjectArray(getValue()), this.separator);
	}

}
