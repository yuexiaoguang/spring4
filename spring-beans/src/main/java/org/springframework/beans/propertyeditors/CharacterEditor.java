package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * {@link Character}的编辑器, 从String值填充{@code Character}或{@code char}类型的属性.
 *
 * <p>请注意, JDK不包含{@code char}的默认{@link java.beans.PropertyEditor 属性编辑器}!
 * {@link org.springframework.beans.BeanWrapperImpl} 默认会注册此编辑器.
 *
 * <p>还支持Unicode字符序列的转换; e.g. {@code u0041} ('A').
 */
public class CharacterEditor extends PropertyEditorSupport {

	/**
	 * 将字符串标识为Unicode字符序列的前缀.
	 */
	private static final String UNICODE_PREFIX = "\\u";

	/**
	 * Unicode字符序列的长度.
	 */
	private static final int UNICODE_LENGTH = 6;


	private final boolean allowEmpty;


	/**
	 * 创建一个新的CharacterEditor实例.
	 * <p>"allowEmpty" 参数控制是否允许在解析中使用空String,
	 * i.e. 在{@link #setAsText(String) 文本被转换}时, 被解释为 {@code null}值.
	 * 如果为{@code false}, 那时将抛出{@link IllegalArgumentException}.
	 * 
	 * @param allowEmpty 如果允许空字符串
	 */
	public CharacterEditor(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasLength(text)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (text == null) {
			throw new IllegalArgumentException("null String cannot be converted to char type");
		}
		else if (isUnicodeCharacterSequence(text)) {
			setAsUnicode(text);
		}
		else if (text.length() == 1) {
			setValue(Character.valueOf(text.charAt(0)));
		}
		else {
			throw new IllegalArgumentException("String [" + text + "] with length " +
					text.length() + " cannot be converted to char type: neither Unicode nor single character");
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}


	private boolean isUnicodeCharacterSequence(String sequence) {
		return (sequence.startsWith(UNICODE_PREFIX) && sequence.length() == UNICODE_LENGTH);
	}

	private void setAsUnicode(String text) {
		int code = Integer.parseInt(text.substring(UNICODE_PREFIX.length()), 16);
		setValue(Character.valueOf((char) code));
	}

}
