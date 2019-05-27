package org.springframework.context.i18n;

import java.util.TimeZone;

/**
 * {@link LocaleContext}的扩展, 添加当前时区的感知.
 *
 * <p>将LocaleContext的此变体设置为{@link LocaleContextHolder}, 意味着已配置了一些TimeZone-aware的基础结构,
 * 即使它目前可能无法生成非null TimeZone.
 */
public interface TimeZoneAwareLocaleContext extends LocaleContext {

	/**
	 * 返回当前TimeZone, 可根据实施策略动态修复或确定.
	 * 
	 * @return 当前的TimeZone; 如果没有关联特定的TimeZone, 则为{@code null}
	 */
	TimeZone getTimeZone();

}
