package org.springframework.format.support;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.Formatter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 在{@link Formatter} 和 {@link PropertyEditor}之间桥接的适配器.
 */
public class FormatterPropertyEditorAdapter extends PropertyEditorSupport {

	private final Formatter<Object> formatter;


	/**
	 * @param formatter 要包装的{@link Formatter}
	 */
	@SuppressWarnings("unchecked")
	public FormatterPropertyEditorAdapter(Formatter<?> formatter) {
		Assert.notNull(formatter, "Formatter must not be null");
		this.formatter = (Formatter<Object>) formatter;
	}


	/**
	 * 确定声明的 {@link Formatter}字段类型.
	 * 
	 * @return 在包装的{@link Formatter}实现中声明的字段类型 (never {@code null})
	 * @throws IllegalArgumentException 如果无法推断声明的{@link Formatter}字段类型
	 */
	public Class<?> getFieldType() {
		return FormattingConversionService.getFieldType(this.formatter);
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			try {
				setValue(this.formatter.parse(text, LocaleContextHolder.getLocale()));
			}
			catch (IllegalArgumentException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? this.formatter.print(value, LocaleContextHolder.getLocale()) : "");
	}

}
