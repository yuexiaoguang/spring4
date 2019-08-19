package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;

import org.springframework.util.ObjectUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * 包可见的辅助类, 用于格式化通过表单标记呈现的值.
 * 支持两种格式: 普通的和{@link PropertyEditor}感知的.
 *
 * <p>普通格式化只会阻止字符串'{@code null}'出现, 将其替换为空字符串, 并根据需要添加HTML转义.
 *
 * <p>{@link PropertyEditor}感知格式化将尝试使用提供的{@link PropertyEditor}
 * 在应用普通的格式化的默认规则之前渲染任何非String值.
 */
abstract class ValueFormatter {

	/**
	 * 构建所提供的{@code Object}的显示值, 根据需要HTML转义.
	 * 此版本<strong>不是</strong> {@link PropertyEditor}感知的.
	 */
	public static String getDisplayString(Object value, boolean htmlEscape) {
		String displayValue = ObjectUtils.getDisplayString(value);
		return (htmlEscape ? HtmlUtils.htmlEscape(displayValue) : displayValue);
	}

	/**
	 * 构建所提供的{@code Object}的显示值, 根据需要HTML转义.
	 * 如果提供的值不是{@link String}, 且提供的{@link PropertyEditor}不为null,
	 * 则使用{@link PropertyEditor}获取显示值.
	 */
	public static String getDisplayString(Object value, PropertyEditor propertyEditor, boolean htmlEscape) {
		if (propertyEditor != null && !(value instanceof String)) {
			try {
				propertyEditor.setValue(value);
				String text = propertyEditor.getAsText();
				if (text != null) {
					return getDisplayString(text, htmlEscape);
				}
			}
			catch (Throwable ex) {
				// PropertyEditor可能不支持此值... 通过.
			}
		}
		return getDisplayString(value, htmlEscape);
	}
}
