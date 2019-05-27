package org.springframework.format.number.money;

import java.util.Locale;
import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;

import org.springframework.format.Formatter;

/**
 * JSR-354 {@link javax.money.MonetaryAmount}值的格式化器,
 * 委托给{@link javax.money.format.MonetaryAmountFormat#format}
 * 和{@link javax.money.format.MonetaryAmountFormat#parse}.
 */
public class MonetaryAmountFormatter implements Formatter<MonetaryAmount> {

	private String formatName;


	/**
	 * 创建一个由区域设置驱动的MonetaryAmountFormatter.
	 */
	public MonetaryAmountFormatter() {
	}

	/**
	 * @param formatName 格式化名称, 由JSR-354提供者在运行时解析
	 */
	public MonetaryAmountFormatter(String formatName) {
		this.formatName = formatName;
	}


	/**
	 * 指定格式化名称, 由JSR-354提供者在运行时解析.
	 * <p>默认无, 根据当前区域设置获取 {@link MonetaryAmountFormat}.
	 */
	public void setFormatName(String formatName) {
		this.formatName = formatName;
	}


	@Override
	public String print(MonetaryAmount object, Locale locale) {
		return getMonetaryAmountFormat(locale).format(object);
	}

	@Override
	public MonetaryAmount parse(String text, Locale locale) {
		return getMonetaryAmountFormat(locale).parse(text);
	}


	/**
	 * 获取给定区域设置的MonetaryAmountFormat.
	 * <p>默认实现使用配置的格式名称或给定的区域设置调用 {@link javax.money.format.MonetaryFormats#getAmountFormat}.
	 * 
	 * @param locale 当前区域
	 * 
	 * @return MonetaryAmountFormat (never {@code null})
	 */
	protected MonetaryAmountFormat getMonetaryAmountFormat(Locale locale) {
		if (this.formatName != null) {
			return MonetaryFormats.getAmountFormat(this.formatName);
		}
		else {
			return MonetaryFormats.getAmountFormat(locale);
		}
	}
}
