package org.springframework.format.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * 用于货币样式的数值的BigDecimal格式化器.
 *
 * <p>委托给 {@link java.text.NumberFormat#getCurrencyInstance(Locale)}.
 * 配置BigDecimal解析, 这样就不会损失精度.
 * 可以将指定的{@link java.math.RoundingMode}应用于已解析的值.
 */
public class CurrencyStyleFormatter extends AbstractNumberFormatter {

	private int fractionDigits = 2;

	private RoundingMode roundingMode;

	private Currency currency;

	private String pattern;


	/**
	 * 指定所需的小数位数.
	 * 默认 2.
	 */
	public void setFractionDigits(int fractionDigits) {
		this.fractionDigits = fractionDigits;
	}

	/**
	 * 指定用于decimal解析的舍入模式.
	 * 默认 {@link java.math.RoundingMode#UNNECESSARY}.
	 */
	public void setRoundingMode(RoundingMode roundingMode) {
		this.roundingMode = roundingMode;
	}

	/**
	 * 如果已知, 请指定货币.
	 */
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	/**
	 * 指定用于格式化数值的模式.
	 * 如果未指定, 则使用默认的DecimalFormat模式.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}


	@Override
	public BigDecimal parse(String text, Locale locale) throws ParseException {
		BigDecimal decimal = (BigDecimal) super.parse(text, locale);
		if (decimal != null) {
			if (this.roundingMode != null) {
				decimal = decimal.setScale(this.fractionDigits, this.roundingMode);
			}
			else {
				decimal = decimal.setScale(this.fractionDigits);
			}
		}
		return decimal;
	}

	@Override
	protected NumberFormat getNumberFormat(Locale locale) {
		DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
		format.setParseBigDecimal(true);
		format.setMaximumFractionDigits(this.fractionDigits);
		format.setMinimumFractionDigits(this.fractionDigits);
		if (this.roundingMode != null) {
			format.setRoundingMode(this.roundingMode);
		}
		if (this.currency != null) {
			format.setCurrency(this.currency);
		}
		if (this.pattern != null) {
			format.applyPattern(this.pattern);
		}
		return format;
	}

}
