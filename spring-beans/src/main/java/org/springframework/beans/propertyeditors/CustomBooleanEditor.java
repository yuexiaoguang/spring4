package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * Boolean/boolean属性的属性编辑器.
 *
 * <p>这不是要用作系统PropertyEditor, 而是用作自定义控制器代码中特定于语言环境的Boolean编辑器,
 * 将UI引起的boolean字符串解析为bean的boolean属性, 并在UI表单中检查它们.
 *
 * <p>在Web MVC代码中, 此编辑器通常会在 {@code binder.registerCustomEditor}调用中注册.
 */
public class CustomBooleanEditor extends PropertyEditorSupport {

	public static final String VALUE_TRUE = "true";
	public static final String VALUE_FALSE = "false";

	public static final String VALUE_ON = "on";
	public static final String VALUE_OFF = "off";

	public static final String VALUE_YES = "yes";
	public static final String VALUE_NO = "no";

	public static final String VALUE_1 = "1";
	public static final String VALUE_0 = "0";


	private final String trueString;

	private final String falseString;

	private final boolean allowEmpty;


	/**
	 * 创建一个新的CustomBooleanEditor实例, 将"true"/"on"/"yes" 和 "false"/"off"/"no" 作为识别的String值.
	 * <p>"allowEmpty"参数指出是否应允许空字符串进行解析, i.e. 解释为 null 值.
	 * 否则, 在这种情况下会抛出IllegalArgumentException.
	 * 
	 * @param allowEmpty 是否允许空字符串
	 */
	public CustomBooleanEditor(boolean allowEmpty) {
		this(null, null, allowEmpty);
	}

	/**
	 * 创建一个新的CustomBooleanEditor实例, true和false的String值可配置.
	 * <p>"allowEmpty"参数指出是否应允许空字符串进行解析, i.e. 解释为 null 值.
	 * 否则, 在这种情况下会抛出IllegalArgumentException.
	 * 
	 * @param trueString 表示true的String值:
	 * 例如, "true" (VALUE_TRUE), "on" (VALUE_ON), "yes" (VALUE_YES) 或其它自定义值
	 * @param falseString 表示false的String值:
	 * 例如, "false" (VALUE_FALSE), "off" (VALUE_OFF), "no" (VALUE_NO) 或其它自定义值
	 * @param allowEmpty 是否允许空字符串
	 */
	public CustomBooleanEditor(String trueString, String falseString, boolean allowEmpty) {
		this.trueString = trueString;
		this.falseString = falseString;
		this.allowEmpty = allowEmpty;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String input = (text != null ? text.trim() : null);
		if (this.allowEmpty && !StringUtils.hasLength(input)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (this.trueString != null && this.trueString.equalsIgnoreCase(input)) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString != null && this.falseString.equalsIgnoreCase(input)) {
			setValue(Boolean.FALSE);
		}
		else if (this.trueString == null &&
				(VALUE_TRUE.equalsIgnoreCase(input) || VALUE_ON.equalsIgnoreCase(input) ||
						VALUE_YES.equalsIgnoreCase(input) || VALUE_1.equals(input))) {
			setValue(Boolean.TRUE);
		}
		else if (this.falseString == null &&
				(VALUE_FALSE.equalsIgnoreCase(input) || VALUE_OFF.equalsIgnoreCase(input) ||
						VALUE_NO.equalsIgnoreCase(input) || VALUE_0.equals(input))) {
			setValue(Boolean.FALSE);
		}
		else {
			throw new IllegalArgumentException("Invalid boolean value [" + text + "]");
		}
	}

	@Override
	public String getAsText() {
		if (Boolean.TRUE.equals(getValue())) {
			return (this.trueString != null ? this.trueString : VALUE_TRUE);
		}
		else if (Boolean.FALSE.equals(getValue())) {
			return (this.falseString != null ? this.falseString : VALUE_FALSE);
		}
		else {
			return "";
		}
	}

}
