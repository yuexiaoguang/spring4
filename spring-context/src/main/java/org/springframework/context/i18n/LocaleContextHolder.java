package org.springframework.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;

/**
 * 简单的holder类, 它将LocaleContext实例与当前线程相关联.
 * 如果{@code inheritable}标志设置为{@code true}, 则当前线程生成的任何子线程都将继承LocaleContext.
 *
 * <p>在必要时用作Spring中当前Locale的中央持有者:
 * 例如, in MessageSourceAccessor.
 * DispatcherServlet在此自动暴露其当前的Locale.
 * 其他应用程序也可以暴露它们, 以使像MessageSourceAccessor这样的类自动使用该Locale.
 */
public abstract class LocaleContextHolder {

	private static final ThreadLocal<LocaleContext> localeContextHolder =
			new NamedThreadLocal<LocaleContext>("LocaleContext");

	private static final ThreadLocal<LocaleContext> inheritableLocaleContextHolder =
			new NamedInheritableThreadLocal<LocaleContext>("LocaleContext");

	// 框架级别的共享默认区域设置
	private static Locale defaultLocale;

	// 框架级别的共享默认时区
	private static TimeZone defaultTimeZone;


	/**
	 * 重置当前线程的LocaleContext.
	 */
	public static void resetLocaleContext() {
		localeContextHolder.remove();
		inheritableLocaleContextHolder.remove();
	}

	/**
	 * 将给定的LocaleContext与当前线程相关联, 而不是将其暴露为子线程可继承.
	 * <p>给定的LocaleContext可以是 {@link TimeZoneAwareLocaleContext}, 包含关联的时区信息的区域设置.
	 * 
	 * @param localeContext 当前的LocaleContext, 或{@code null}来重置线程绑定的上下文
	 */
	public static void setLocaleContext(LocaleContext localeContext) {
		setLocaleContext(localeContext, false);
	}

	/**
	 * 将给定的LocaleContext与当前线程相关联.
	 * <p>给定的LocaleContext可以是 {@link TimeZoneAwareLocaleContext}, 包含关联的时区信息的区域设置.
	 * 
	 * @param localeContext 当前的LocaleContext, 或{@code null}来重置线程绑定的上下文
	 * @param inheritable 是否将LocaleContext暴露为子线程可继承 (使用 {@link InheritableThreadLocal})
	 */
	public static void setLocaleContext(LocaleContext localeContext, boolean inheritable) {
		if (localeContext == null) {
			resetLocaleContext();
		}
		else {
			if (inheritable) {
				inheritableLocaleContextHolder.set(localeContext);
				localeContextHolder.remove();
			}
			else {
				localeContextHolder.set(localeContext);
				inheritableLocaleContextHolder.remove();
			}
		}
	}

	/**
	 * 返回与当前线程关联的LocaleContext.
	 * 
	 * @return 当前的LocaleContext, 或{@code null}
	 */
	public static LocaleContext getLocaleContext() {
		LocaleContext localeContext = localeContextHolder.get();
		if (localeContext == null) {
			localeContext = inheritableLocaleContextHolder.get();
		}
		return localeContext;
	}

	/**
	 * 将给定的Locale与当前线程相关联, 保留可能已经设置的任何TimeZone.
	 * <p>将隐式地为给定的Locale创建LocaleContext, 而不是将其公开为子线程可继承.
	 * 
	 * @param locale 当前的Locale, 或{@code null}来重置线程绑定上下文的区域设置部分
	 */
	public static void setLocale(Locale locale) {
		setLocale(locale, false);
	}

	/**
	 * 将给定的Locale与当前线程相关联, 保留可能已经设置的任何TimeZone.
	 * <p>将隐式地为给定的Locale创建LocaleContext.
	 * 
	 * @param locale 当前的Locale, 或{@code null}来重置线程绑定上下文的区域设置部分
	 * @param inheritable 是否将LocaleContext暴露为子线程可继承 (使用 {@link InheritableThreadLocal})
	 */
	public static void setLocale(Locale locale, boolean inheritable) {
		LocaleContext localeContext = getLocaleContext();
		TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext ?
				((TimeZoneAwareLocaleContext) localeContext).getTimeZone() : null);
		if (timeZone != null) {
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		}
		else if (locale != null) {
			localeContext = new SimpleLocaleContext(locale);
		}
		else {
			localeContext = null;
		}
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * 在框架级别设置共享的默认区域设置, 以替代JVM范围的默认区域设置.
	 * <p><b>NOTE:</b> 这对于设置与JVM范围的默认区域设置不同的应用程序级默认区域设置非常有用.
	 * 但是, 这要求每个此类应用程序都针对本地部署的Spring Framework jar进行操作.
	 * 在这种情况下, 不要在服务器级别将Spring部署为共享库!
	 * 
	 * @param locale 默认区域设置 (或{@code null}, 让查找回退到{@link Locale#getDefault()})
	 */
	public static void setDefaultLocale(Locale locale) {
		LocaleContextHolder.defaultLocale = locale;
	}

	/**
	 * 返回与当前线程关联的Locale, 否则返回系统默认Locale.
	 * 这实际上是{@link java.util.Locale#getDefault()}的替代, 能够选择性地尊重用户级别的Locale设置.
	 * <p>Note: 此方法可以在框架级别或JVM范围的系统级别回退共享的默认区域设置.
	 * 如果您想检查原始LocaleContext内容 (可能通过{@code null}指示没有特定的区域设置),
	 * 使用@link #getLocaleContext()} 并调用{@link LocaleContext#getLocale()}.
	 * 
	 * @return 当前Locale; 或系统默认的Locale, 如果没有特定的Locale与当前线程关联
	 */
	public static Locale getLocale() {
		LocaleContext localeContext = getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				return locale;
			}
		}
		return (defaultLocale != null ? defaultLocale : Locale.getDefault());
	}

	/**
	 * 将给定的TimeZone与当前线程相关联, 保留可能已经设置的任何区域设置.
	 * <p>将隐式的为给定的Locale创建LocaleContext, 而不是将其公开为子线程可继承.
	 * 
	 * @param timeZone 当前的TimeZone, 或{@code null}重置线程绑定上下文的时区部分
	 */
	public static void setTimeZone(TimeZone timeZone) {
		setTimeZone(timeZone, false);
	}

	/**
	 * 将给定的TimeZone与当前线程相关联, 保留可能已经设置的任何区域设置.
	 * <p>将隐式的为给定的Locale创建LocaleContext.
	 * 
	 * @param timeZone 当前的TimeZone, 或{@code null}重置线程绑定上下文的时区部分
	 * @param inheritable 是否将LocaleContext暴露为子线程可继承 (使用{@link InheritableThreadLocal})
	 */
	public static void setTimeZone(TimeZone timeZone, boolean inheritable) {
		LocaleContext localeContext = getLocaleContext();
		Locale locale = (localeContext != null ? localeContext.getLocale() : null);
		if (timeZone != null) {
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		}
		else if (locale != null) {
			localeContext = new SimpleLocaleContext(locale);
		}
		else {
			localeContext = null;
		}
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * 在框架级别设置共享的默认时区, 作为JVM范围默认时区的替代.
	 * <p><b>NOTE:</b> 这对于设置与JVM范围的默认时区不同的应用程序级默认时区非常有用.
	 * 但是, 这要求每个此类应用程序都针对本地部署的Spring Framework jar进行操作.
	 * 在这种情况下, 不要在服务器级别将Spring部署为共享库!
	 * 
	 * @param timeZone 默认时区 (或{@code null}, 让查找回退到 {@link TimeZone#getDefault()})
	 */
	public static void setDefaultTimeZone(TimeZone timeZone) {
		defaultTimeZone = timeZone;
	}

	/**
	 * 返回与当前线程关联的TimeZone, 否则返回系统默认的TimeZone.
	 * 这实际上是 {@link java.util.TimeZone#getDefault()}的替代, 能够选择性地尊重用户级TimeZone设置.
	 * <p>Note: 此方法可以在框架级别或JVM范围的系统级别回退到共享的默认TimeZone.
	 * 如果您想检查原始LocaleContext内容 (可能通过{@code null}指示没有特定时区),
	 * 使用{@link #getLocaleContext()},
	 * 并在向下转换为{@link TimeZoneAwareLocaleContext}后调用{@link TimeZoneAwareLocaleContext#getTimeZone()}.
	 * 
	 * @return 当前的TimeZone; 或系统默认的TimeZone, 如果没有特定的TimeZone与当前线程关联
	 */
	public static TimeZone getTimeZone() {
		LocaleContext localeContext = getLocaleContext();
		if (localeContext instanceof TimeZoneAwareLocaleContext) {
			TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			if (timeZone != null) {
				return timeZone;
			}
		}
		return (defaultTimeZone != null ? defaultTimeZone : TimeZone.getDefault());
	}
}
