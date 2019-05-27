package org.springframework.format.number.money;

import java.util.Locale;
import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.springframework.format.Formatter;

/**
 * JSR-354 {@link javax.money.CurrencyUnit}值的格式化器, 来自货币代码字符串.
 */
public class CurrencyUnitFormatter implements Formatter<CurrencyUnit> {

	@Override
	public String print(CurrencyUnit object, Locale locale) {
		return object.getCurrencyCode();
	}

	@Override
	public CurrencyUnit parse(String text, Locale locale) {
		return Monetary.getCurrency(text);
	}

}
