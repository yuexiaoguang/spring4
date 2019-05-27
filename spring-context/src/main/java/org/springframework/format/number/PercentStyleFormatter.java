package org.springframework.format.number;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 数字百分比样式的格式化器.
 *
 * <p>委托给 {@link java.text.NumberFormat#getPercentInstance(Locale)}.
 * 配置BigDecimal解析, 这样就不会有精度损失.
 * {@link #parse(String, Locale)}例程总是返回一个BigDecimal.
 */
public class PercentStyleFormatter extends AbstractNumberFormatter {

	@Override
	protected NumberFormat getNumberFormat(Locale locale) {
		NumberFormat format = NumberFormat.getPercentInstance(locale);
		if (format instanceof DecimalFormat) {
			((DecimalFormat) format).setParseBigDecimal(true);
		}
		return format;
	}

}
