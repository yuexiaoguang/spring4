package org.springframework.format.datetime.joda;

import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.util.StringUtils;

/**
 * 创建Joda-Time的工厂{@link DateTimeFormatter}.
 *
 * <p>将使用定义的{@link #setPattern pattern}, {@link #setIso ISO}, 和 {@link #setStyle style}方法(按此顺序考虑)创建格式化器.
 */
public class DateTimeFormatterFactory {

	private String pattern;

	private ISO iso;

	private String style;

	private TimeZone timeZone;


	public DateTimeFormatterFactory() {
	}

	/**
	 * @param pattern 用于格式化日期值的模式
	 */
	public DateTimeFormatterFactory(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * 设置用于格式化日期值的模式.
	 * 
	 * @param pattern 格式化模式
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 设置用于格式化日期值的ISO格式.
	 * 
	 * @param iso ISO格式
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * 在Joda-Time样式中设置用于格式化日期值的两个字符.
	 * <p>第一个字符用于日期样式; 第二个是时间样式. 支持的字符是:
	 * <ul>
	 * <li>'S' = Small</li>
	 * <li>'M' = Medium</li>
	 * <li>'L' = Long</li>
	 * <li>'F' = Full</li>
	 * <li>'-' = Omitted</li>
	 * </ul>
	 * 
	 * @param style {"S", "M", "L", "F", "-"}中的两个字符
	 */
	public void setStyle(String style) {
		this.style = style;
	}

	/**
	 * 设置标准化日期值的{@code TimeZone}.
	 * 
	 * @param timeZone 时区
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}


	/**
	 * 使用此工厂创建一个新的{@code DateTimeFormatter}.
	 * <p>如果未定义特定模式或样式, 将使用{@link DateTimeFormat#mediumDateTime() 中日期时间格式}.
	 * 
	 * @return 新的日期时间格式化器
	 */
	public DateTimeFormatter createDateTimeFormatter() {
		return createDateTimeFormatter(DateTimeFormat.mediumDateTime());
	}

	/**
	 * 使用此工厂创建一个新的{@code DateTimeFormatter}.
	 * <p>如果未定义特定模式或样式, 将使用 {@code fallbackFormatter}.
	 * 
	 * @param fallbackFormatter 在没有设置特定工厂属性时, 使用的后备格式化器 (can be {@code null}).
	 * 
	 * @return 新的日期时间格式化器
	 */
	public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
		DateTimeFormatter dateTimeFormatter = null;
		if (StringUtils.hasLength(this.pattern)) {
			dateTimeFormatter = DateTimeFormat.forPattern(this.pattern);
		}
		else if (this.iso != null && this.iso != ISO.NONE) {
			switch (this.iso) {
				case DATE:
					dateTimeFormatter = ISODateTimeFormat.date();
					break;
				case TIME:
					dateTimeFormatter = ISODateTimeFormat.time();
					break;
				case DATE_TIME:
					dateTimeFormatter = ISODateTimeFormat.dateTime();
					break;
				case NONE:
					/* no-op */
					break;
				default:
					throw new IllegalStateException("Unsupported ISO format: " + this.iso);
			}
		}
		else if (StringUtils.hasLength(this.style)) {
			dateTimeFormatter = DateTimeFormat.forStyle(this.style);
		}

		if (dateTimeFormatter != null && this.timeZone != null) {
			dateTimeFormatter = dateTimeFormatter.withZone(DateTimeZone.forTimeZone(this.timeZone));
		}
		return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
	}

}
