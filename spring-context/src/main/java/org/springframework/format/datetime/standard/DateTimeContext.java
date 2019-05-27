package org.springframework.format.datetime.standard;

import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.UsesJava8;

/**
 * 包含用户特定的<code>java.time</code> (JSR-310)设置的上下文, 例如用户的 Chronology (日历系统)和时区.
 * {@code null}属性值表示用户尚未指定设置.
 */
@UsesJava8
public class DateTimeContext {

	private Chronology chronology;

	private ZoneId timeZone;


	/**
	 * 设置用户的年表 (日历系统).
	 */
	public void setChronology(Chronology chronology) {
		this.chronology = chronology;
	}

	/**
	 * 返回用户的年表 (日历系统).
	 */
	public Chronology getChronology() {
		return this.chronology;
	}

	/**
	 * 设置用户的时区.
	 * <p>或者, 在{@link LocaleContextHolder}上设置{@link TimeZoneAwareLocaleContext}.
	 * 如果此处未提供任何设置, 则此上下文类将回退到检查区域设置上下文.
	 */
	public void setTimeZone(ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 返回用户的时区.
	 */
	public ZoneId getTimeZone() {
		return this.timeZone;
	}


	/**
	 * 获取DateTimeFormatter, 并将此上下文的设置应用于基础{@code formatter}.
	 * 
	 * @param formatter 建立默认格式规则的基本格式化器, 通常与上下文无关
	 * 
	 * @return 上下文DateTimeFormatter
	 */
	public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
		if (this.chronology != null) {
			formatter = formatter.withChronology(this.chronology);
		}
		if (this.timeZone != null) {
			formatter = formatter.withZone(this.timeZone);
		}
		else {
			LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
				if (timeZone != null) {
					formatter = formatter.withZone(timeZone.toZoneId());
				}
			}
		}
		return formatter;
	}

}
