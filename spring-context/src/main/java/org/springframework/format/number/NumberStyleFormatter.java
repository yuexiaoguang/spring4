package org.springframework.format.number;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 使用NumberFormat的数字样式的通用数字格式化器.
 *
 * <p>委托给 {@link java.text.NumberFormat#getInstance(Locale)}.
 * 配置BigDecimal解析, 这样就不会有精度损失.
 * 允许配置decimal数字模式.
 * {@link #parse(String, Locale)} 例程总是返回一个BigDecimal.
 */
public class NumberStyleFormatter extends AbstractNumberFormatter {

	private String pattern;


	public NumberStyleFormatter() {
	}

	/**
	 * @param pattern 格式化模式
	 */
	public NumberStyleFormatter(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * 指定用于格式化数值的模式.
	 * 如果未指定, 则使用默认的DecimalFormat模式.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}


	@Override
	public NumberFormat getNumberFormat(Locale locale) {
		NumberFormat format = NumberFormat.getInstance(locale);
		if (!(format instanceof DecimalFormat)) {
			if (this.pattern != null) {
				throw new IllegalStateException("Cannot support pattern for non-DecimalFormat: " + format);
			}
			return format;
		}
		DecimalFormat decimalFormat = (DecimalFormat) format;
		decimalFormat.setParseBigDecimal(true);
		if (this.pattern != null) {
			decimalFormat.applyPattern(this.pattern);
		}
		return decimalFormat;
	}

}
