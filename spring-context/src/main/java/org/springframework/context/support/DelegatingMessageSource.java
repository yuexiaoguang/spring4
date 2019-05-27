package org.springframework.context.support;

import java.util.Locale;

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

/**
 * 清空{@link MessageSource}, 将所有调用委托给父级MessageSource.
 * 如果没有父级可用, 它将无法解析任何消息.
 *
 * <p>如果上下文未定义自己的MessageSource, 则由AbstractApplicationContext用作占位符. 不适合直接用于应用程序.
 */
public class DelegatingMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

	private MessageSource parentMessageSource;


	@Override
	public void setParentMessageSource(MessageSource parent) {
		this.parentMessageSource = parent;
	}

	@Override
	public MessageSource getParentMessageSource() {
		return this.parentMessageSource;
	}


	@Override
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		if (this.parentMessageSource != null) {
			return this.parentMessageSource.getMessage(code, args, defaultMessage, locale);
		}
		else {
			return renderDefaultMessage(defaultMessage, args, locale);
		}
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		if (this.parentMessageSource != null) {
			return this.parentMessageSource.getMessage(code, args, locale);
		}
		else {
			throw new NoSuchMessageException(code, locale);
		}
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		if (this.parentMessageSource != null) {
			return this.parentMessageSource.getMessage(resolvable, locale);
		}
		else {
			if (resolvable.getDefaultMessage() != null) {
				return renderDefaultMessage(resolvable.getDefaultMessage(), resolvable.getArguments(), locale);
			}
			String[] codes = resolvable.getCodes();
			String code = (codes != null && codes.length > 0 ? codes[0] : null);
			throw new NoSuchMessageException(code, locale);
		}
	}

}
