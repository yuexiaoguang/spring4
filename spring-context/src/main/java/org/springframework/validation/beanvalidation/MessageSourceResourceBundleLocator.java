package org.springframework.validation.beanvalidation;

import java.util.Locale;
import java.util.ResourceBundle;

import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.util.Assert;

/**
 * Hibernate Validator 4.3/5.x的{@link ResourceBundleLocator}接口的实现,
 * 将Spring {@link MessageSource}暴露为本地化的{@link MessageSourceResourceBundle}.
 */
public class MessageSourceResourceBundleLocator implements ResourceBundleLocator {

	private final MessageSource messageSource;

	/**
	 * @param messageSource 要包装的Spring MessageSource
	 */
	public MessageSourceResourceBundleLocator(MessageSource messageSource) {
		Assert.notNull(messageSource, "MessageSource must not be null");
		this.messageSource = messageSource;
	}

	@Override
	public ResourceBundle getResourceBundle(Locale locale) {
		return new MessageSourceResourceBundle(this.messageSource, locale);
	}

}
