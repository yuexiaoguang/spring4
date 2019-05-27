package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * 修剪字符串的属性编辑器.
 *
 * <p>允许将空字符串转换为{@code null}值.
 * 需要显式注册, e.g. 用于命令绑定.
 */
public class StringTrimmerEditor extends PropertyEditorSupport {

	private final String charsToDelete;

	private final boolean emptyAsNull;


	/**
	 * @param emptyAsNull {@code true} 如果要将空字符串转换为{@code null}
	 */
	public StringTrimmerEditor(boolean emptyAsNull) {
		this.charsToDelete = null;
		this.emptyAsNull = emptyAsNull;
	}

	/**
	 * @param charsToDelete 除了修剪输入字符串外, 还要删除一组字符.
	 * 用于删除不需要的换行符:
	 * e.g. "\r\n\f" 将删除String中的所有新行和换行符.
	 * @param emptyAsNull {@code true} 如果要将空字符串转换为{@code null}
	 */
	public StringTrimmerEditor(String charsToDelete, boolean emptyAsNull) {
		this.charsToDelete = charsToDelete;
		this.emptyAsNull = emptyAsNull;
	}


	@Override
	public void setAsText(String text) {
		if (text == null) {
			setValue(null);
		}
		else {
			String value = text.trim();
			if (this.charsToDelete != null) {
				value = StringUtils.deleteAny(value, this.charsToDelete);
			}
			if (this.emptyAsNull && "".equals(value)) {
				setValue(null);
			}
			else {
				setValue(value);
			}
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}

}
