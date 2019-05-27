package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.regex.Pattern;

/**
 * {@code java.util.regex.Pattern}的编辑器, 直接填充Pattern属性.
 * 期望与Pattern的{@code compile}方法相同的语法.
 */
public class PatternEditor extends PropertyEditorSupport {

	private final int flags;


	/**
	 * 使用默认设置创建新的PatternEditor.
	 */
	public PatternEditor() {
		this.flags = 0;
	}

	/**
	 * 使用给定的设置创建一个新的PatternEditor.
	 * 
	 * @param flags 要应用的{@code java.util.regex.Pattern}标志
	 */
	public PatternEditor(int flags) {
		this.flags = flags;
	}


	@Override
	public void setAsText(String text) {
		setValue(text != null ? Pattern.compile(text, this.flags) : null);
	}

	@Override
	public String getAsText() {
		Pattern value = (Pattern) getValue();
		return (value != null ? value.pattern() : "");
	}

}
