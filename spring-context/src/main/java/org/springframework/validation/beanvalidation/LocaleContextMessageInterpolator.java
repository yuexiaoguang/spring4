package org.springframework.validation.beanvalidation;

import java.util.Locale;
import javax.validation.MessageInterpolator;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;

/**
 * 委托给目标{@link MessageInterpolator}实现, 但强制Spring托管的区域设置.
 * 通常用于包装验证提供器的默认插补器.
 */
public class LocaleContextMessageInterpolator implements MessageInterpolator {

	private final MessageInterpolator targetInterpolator;


	/**
	 * @param targetInterpolator 要包装的目标MessageInterpolator
	 */
	public LocaleContextMessageInterpolator(MessageInterpolator targetInterpolator) {
		Assert.notNull(targetInterpolator, "Target MessageInterpolator must not be null");
		this.targetInterpolator = targetInterpolator;
	}


	@Override
	public String interpolate(String message, Context context) {
		return this.targetInterpolator.interpolate(message, context, LocaleContextHolder.getLocale());
	}

	@Override
	public String interpolate(String message, Context context, Locale locale) {
		return this.targetInterpolator.interpolate(message, context, locale);
	}

}
