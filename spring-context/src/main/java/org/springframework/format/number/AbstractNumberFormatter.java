package org.springframework.format.number;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * Number的抽象格式化器, 提供{@link #getNumberFormat(java.util.Locale)}模板方法.
 */
public abstract class AbstractNumberFormatter implements Formatter<Number> {

	private boolean lenient = false;


	/**
	 * 指定解析是否宽松. 默认 false.
	 * <p>使用宽松解析, 解析器可能允许与格式不完全匹配的输入.
	 * 使用严格的解析, 输入必须与格式完全匹配.
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	@Override
	public String print(Number number, Locale locale) {
		return getNumberFormat(locale).format(number);
	}

	@Override
	public Number parse(String text, Locale locale) throws ParseException {
		NumberFormat format = getNumberFormat(locale);
		ParsePosition position = new ParsePosition(0);
		Number number = format.parse(text, position);
		if (position.getErrorIndex() != -1) {
			throw new ParseException(text, position.getIndex());
		}
		if (!this.lenient) {
			if (text.length() != position.getIndex()) {
				// 表示未解析的字符串的一部分
				throw new ParseException(text, position.getIndex());
			}
		}
		return number;
	}

	/**
	 * 获取指定区域设置的具体NumberFormat.
	 * 
	 * @param locale 当前区域设置
	 * 
	 * @return NumberFormat实例 (never {@code null})
	 */
	protected abstract NumberFormat getNumberFormat(Locale locale);

}
