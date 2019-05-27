package org.springframework.core.convert.support;

import java.util.Currency;

import org.springframework.core.convert.converter.Converter;

/**
 * 将字符串转换为{@link Currency}.
 */
class StringToCurrencyConverter implements Converter<String, Currency> {

	@Override
	public Currency convert(String source) {
		return Currency.getInstance(source);
	}

}
