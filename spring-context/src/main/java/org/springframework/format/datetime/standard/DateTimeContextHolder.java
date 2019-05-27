package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.UsesJava8;

/**
 * 线程本地用户 {@link DateTimeContext}的持有者.
 */
@UsesJava8
public final class DateTimeContextHolder {

	private static final ThreadLocal<DateTimeContext> dateTimeContextHolder =
			new NamedThreadLocal<DateTimeContext>("DateTimeContext");


	/**
	 * 重置当前线程的DateTimeContext.
	 */
	public static void resetDateTimeContext() {
		dateTimeContextHolder.remove();
	}

	/**
	 * 将给定的DateTimeContext与当前线程相关联.
	 * 
	 * @param dateTimeContext 当前DateTimeContext, 或{@code null}重置线程绑定上下文
	 */
	public static void setDateTimeContext(DateTimeContext dateTimeContext) {
		if (dateTimeContext == null) {
			resetDateTimeContext();
		}
		else {
			dateTimeContextHolder.set(dateTimeContext);
		}
	}

	/**
	 * 返回与当前线程关联的DateTimeContext, if any.
	 * 
	 * @return 当前DateTimeContext, 或{@code null}
	 */
	public static DateTimeContext getDateTimeContext() {
		return dateTimeContextHolder.get();
	}


	/**
	 * 获取DateTimeFormatter, 将其中特定于用户的设置应用于给定的基本Formatter.
	 * 
	 * @param formatter 建立默认格式规则的基本格式化器 (通常与用户无关)
	 * @param locale 当前用户区域设置 (may be {@code null} if not known)
	 * 
	 * @return 用户特定的DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, Locale locale) {
		DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
		DateTimeContext context = getDateTimeContext();
		return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
	}

}
