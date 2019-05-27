package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.springframework.util.StringUtils;

/**
 * {@code java.util.Date}的属性编辑器, 支持自定义 {@code java.text.DateFormat}.
 *
 * <p>这不是要用作系统PropertyEditor, 而是用作自定义控制器代码中的特定于语言环境的日期编辑器,
 * 将用户输入的数字字符串解析为Bean的Date属性, 并以UI形式呈现它们.
 *
 * <p>在Web MVC代码中, 此编辑器通常会在{@code binder.registerCustomEditor}中注册.
 */
public class CustomDateEditor extends PropertyEditorSupport {

	private final DateFormat dateFormat;

	private final boolean allowEmpty;

	private final int exactDateLength;


	/**
	 * 创建一个新的CustomDateEditor实例, 使用给定的DateFormat进行解析和呈现.
	 * <p>"allowEmpty"参数指出是否应允许空字符串进行解析, i.e. 解释为 null值.
	 * 否则, 在这种情况下会抛出IllegalArgumentException.
	 * 
	 * @param dateFormat 用于解析和呈现的DateFormat
	 * @param allowEmpty 是否允许空字符串
	 */
	public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty) {
		this.dateFormat = dateFormat;
		this.allowEmpty = allowEmpty;
		this.exactDateLength = -1;
	}

	/**
	 * 创建一个新的CustomDateEditor实例, 使用给定的DateFormat进行解析和呈现.
	 * <p>"allowEmpty"参数指出是否应允许空字符串进行解析, i.e. 解释为 null值.
	 * 否则, 在这种情况下会抛出IllegalArgumentException.
	 * <p>"exactDateLength" 参数声明如果String与指定的长度不完全匹配, 则抛出IllegalArgumentException.
	 * 这很有用, 因为SimpleDateFormat不强制对年份部分进行严格的解析, 甚至不使用 {@code setLenient(false)}.
	 * 如果未指定"exactDateLength", 则"01/01/05" 将被解析为 "01/01/0005".
	 * 但是, 即使指定了 "exactDateLength", 在日期或月份部分中的前置零也可能允许更短的年份部分,
	 * 所以请考虑这只是一个让你更接近预期日期格式的断言.
	 * 
	 * @param dateFormat 用于解析和呈现的DateFormat
	 * @param allowEmpty 是否允许空字符串
	 * @param exactDateLength 日期字符串的精确预期长度
	 */
	public CustomDateEditor(DateFormat dateFormat, boolean allowEmpty, int exactDateLength) {
		this.dateFormat = dateFormat;
		this.allowEmpty = allowEmpty;
		this.exactDateLength = exactDateLength;
	}


	/**
	 * 使用指定的DateFormat从给定文本中解析Date.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (text != null && this.exactDateLength >= 0 && text.length() != this.exactDateLength) {
			throw new IllegalArgumentException(
					"Could not parse date: it is not exactly" + this.exactDateLength + "characters long");
		}
		else {
			try {
				setValue(this.dateFormat.parse(text));
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Could not parse date: " + ex.getMessage(), ex);
			}
		}
	}

	/**
	 * 使用指定的DateFormat将Date格式化为String.
	 */
	@Override
	public String getAsText() {
		Date value = (Date) getValue();
		return (value != null ? this.dateFormat.format(value) : "");
	}

}
