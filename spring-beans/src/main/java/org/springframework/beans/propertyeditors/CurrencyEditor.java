package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Currency;

/**
 * {@code java.util.Currency}的编辑器, 将货币代码转换为货币对象.
 * 将货币代码公开为Currency对象的文本表示.
 */
public class CurrencyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(Currency.getInstance(text));
	}

	@Override
	public String getAsText() {
		Currency value = (Currency) getValue();
		return (value != null ? value.getCurrencyCode() : "");
	}

}
