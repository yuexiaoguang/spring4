package org.springframework.context.i18n;

import java.util.Locale;

/**
 * 用于确定当前Locale的策略接口.
 *
 * <p>可以通过LocaleContextHolder类与线程相关联的LocaleContext实例.
 */
public interface LocaleContext {

	/**
	 * 返回当前的Locale, 可以根据实现策略动态修复或确定.
	 * 
	 * @return 当前的Locale; 如果没有特定的Locale关联, 则为{@code null}
	 */
	Locale getLocale();

}
