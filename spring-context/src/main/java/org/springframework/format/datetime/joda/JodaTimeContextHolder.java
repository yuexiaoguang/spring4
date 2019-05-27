package org.springframework.format.datetime.joda;

import java.util.Locale;

import org.joda.time.format.DateTimeFormatter;

import org.springframework.core.NamedThreadLocal;

/**
 * 具有用户特定Joda-Time设置的线程本地{@link JodaTimeContext}的持有者.
 */
public final class JodaTimeContextHolder {

	private static final ThreadLocal<JodaTimeContext> jodaTimeContextHolder =
			new NamedThreadLocal<JodaTimeContext>("JodaTimeContext");


	/**
	 * 重置当前线程的JodaTimeContext.
	 */
	public static void resetJodaTimeContext() {
		jodaTimeContextHolder.remove();
	}

	/**
	 * 将给定的JodaTimeContext与当前线程相关联.
	 * 
	 * @param jodaTimeContext 当前的JodaTimeContext, 或{@code null}来重置线程绑定的上下文
	 */
	public static void setJodaTimeContext(JodaTimeContext jodaTimeContext) {
		if (jodaTimeContext == null) {
			resetJodaTimeContext();
		}
		else {
			jodaTimeContextHolder.set(jodaTimeContext);
		}
	}

	/**
	 * 返回与当前线程关联的JodaTimeContext.
	 * 
	 * @return 当前JodaTimeContext, 或{@code null}
	 */
	public static JodaTimeContext getJodaTimeContext() {
		return jodaTimeContextHolder.get();
	}


	/**
	 * 获取DateTimeFormatter, 其中将特定于用户的设置应用于给定的基本Formatter.
	 * 
	 * @param formatter 建立默认格式规则的基本格式化器 (通常与用户无关)
	 * @param locale 当前用户区域设置 (may be {@code null} if not known)
	 * 
	 * @return 用户特定的DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, Locale locale) {
		DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
		JodaTimeContext context = getJodaTimeContext();
		return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
	}

}
