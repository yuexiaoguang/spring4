package org.springframework.web.servlet.tags;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * 用于在此页面范围内查找主题消息的自定义标记.
 * 使用ApplicationContext的ThemeSource查找消息, 因此应支持国际化.
 *
 * <p>在此标记实例, 页面级别或web.xml级别上考虑HTML转义设置.
 *
 * <p>如果未设置或无法解析"code", 则"text"将用作默认消息.
 *
 * <p>可以通过{@link #setArguments(Object) arguments}属性或使用嵌套的{@code <spring:argument>}标记指定消息参数.
 */
@SuppressWarnings("serial")
public class ThemeTag extends MessageTag {

	/**
	 * 使用主题MessageSource进行主题消息解析.
	 */
	@Override
	protected MessageSource getMessageSource() {
		return getRequestContext().getTheme().getMessageSource();
	}

	/**
	 * 返回指示当前主题的异常消息.
	 */
	@Override
	protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
		return "Theme '" + getRequestContext().getTheme().getName() + "': " + ex.getMessage();
	}

}
