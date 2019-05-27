package org.springframework.format.number.money;

import java.text.ParseException;
import java.util.Collections;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.annotation.NumberFormat.Style;
import org.springframework.format.number.CurrencyStyleFormatter;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.format.number.PercentStyleFormatter;
import org.springframework.util.StringUtils;

/**
 * 格式化带Spring的常用{@link NumberFormat}注解的{@link javax.money.MonetaryAmount}字段.
 */
public class Jsr354NumberFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<NumberFormat> {

	private static final String CURRENCY_CODE_PATTERN = "\u00A4\u00A4";


	@Override
	@SuppressWarnings("unchecked")
	public Set<Class<?>> getFieldTypes() {
		return (Set) Collections.singleton(MonetaryAmount.class);
	}

	@Override
	public Printer<MonetaryAmount> getPrinter(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}

	@Override
	public Parser<MonetaryAmount> getParser(NumberFormat annotation, Class<?> fieldType) {
		return configureFormatterFrom(annotation);
	}


	private Formatter<MonetaryAmount> configureFormatterFrom(NumberFormat annotation) {
		if (StringUtils.hasLength(annotation.pattern())) {
			return new PatternDecoratingFormatter(resolveEmbeddedValue(annotation.pattern()));
		}
		else {
			Style style = annotation.style();
			if (style == Style.NUMBER) {
				return new NumberDecoratingFormatter(new NumberStyleFormatter());
			}
			else if (style == Style.PERCENT) {
				return new NumberDecoratingFormatter(new PercentStyleFormatter());
			}
			else {
				return new NumberDecoratingFormatter(new CurrencyStyleFormatter());
			}
		}
	}


	private static class NumberDecoratingFormatter implements Formatter<MonetaryAmount> {

		private final Formatter<Number> numberFormatter;

		public NumberDecoratingFormatter(Formatter<Number> numberFormatter) {
			this.numberFormatter = numberFormatter;
		}

		@Override
		public String print(MonetaryAmount object, Locale locale) {
			return this.numberFormatter.print(object.getNumber(), locale);
		}

		@Override
		public MonetaryAmount parse(String text, Locale locale) throws ParseException {
			CurrencyUnit currencyUnit = Monetary.getCurrency(locale);
			Number numberValue = this.numberFormatter.parse(text, locale);
			return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
		}
	}


	private static class PatternDecoratingFormatter implements Formatter<MonetaryAmount> {

		private final String pattern;

		public PatternDecoratingFormatter(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public String print(MonetaryAmount object, Locale locale) {
			CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
			formatter.setCurrency(Currency.getInstance(object.getCurrency().getCurrencyCode()));
			formatter.setPattern(this.pattern);
			return formatter.print(object.getNumber(), locale);
		}

		@Override
		public MonetaryAmount parse(String text, Locale locale) throws ParseException {
			CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();
			Currency currency = determineCurrency(text, locale);
			CurrencyUnit currencyUnit = Monetary.getCurrency(currency.getCurrencyCode());
			formatter.setCurrency(currency);
			formatter.setPattern(this.pattern);
			Number numberValue = formatter.parse(text, locale);
			return Monetary.getDefaultAmountFactory().setNumber(numberValue).setCurrency(currencyUnit).create();
		}

		private Currency determineCurrency(String text, Locale locale) {
			try {
				if (text.length() < 3) {
					// 无法包含货币代码 -> 尝试使用区域设置, 并可能在解析时失败.
					return Currency.getInstance(locale);
				}
				else if (this.pattern.startsWith(CURRENCY_CODE_PATTERN)) {
					return Currency.getInstance(text.substring(0, 3));
				}
				else if (this.pattern.endsWith(CURRENCY_CODE_PATTERN)) {
					return Currency.getInstance(text.substring(text.length() - 3));
				}
				else {
					// 没有货币代码的模式...
					return Currency.getInstance(locale);
				}
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Cannot determine currency for number value [" + text + "]", ex);
			}
		}
	}
}
