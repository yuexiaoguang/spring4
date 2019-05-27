package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.util.TimeZone;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 创建JSR-310 {@link java.time.format.DateTimeFormatter}的工厂.
 *
 * <p>将使用定义的 {@link #setPattern pattern}, {@link #setIso ISO}, 和<code>xxxStyle</code>方法 (按此顺序考虑)创建格式化器.
 */
@UsesJava8
public class DateTimeFormatterFactory {

	private String pattern;

	private ISO iso;

	private FormatStyle dateStyle;

	private FormatStyle timeStyle;

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
	 * 设置用于日期类型的样式.
	 */
	public void setDateStyle(FormatStyle dateStyle) {
		this.dateStyle = dateStyle;
	}

	/**
	 * 设置用于时间类型的样式.
	 */
	public void setTimeStyle(FormatStyle timeStyle) {
		this.timeStyle = timeStyle;
	}

	/**
	 * 设置用于日期和时间类型的样式.
	 */
	public void setDateTimeStyle(FormatStyle dateTimeStyle) {
		this.dateStyle = dateTimeStyle;
		this.timeStyle = dateTimeStyle;
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
	 * <p>此方法模仿Joda-Time支持的样式.
	 * 请注意, JSR-310原生支持 {@link java.time.format.FormatStyle},
	 * 用于{@link #setDateStyle}, {@link #setTimeStyle} 和 {@link #setDateTimeStyle}.
	 * 
	 * @param style {"S", "M", "L", "F", "-"}中的两个字符
	 */
	public void setStylePattern(String style) {
		Assert.isTrue(style != null && style.length() == 2, "Style pattern must consist of two characters");
		this.dateStyle = convertStyleCharacter(style.charAt(0));
		this.timeStyle = convertStyleCharacter(style.charAt(1));
	}

	private FormatStyle convertStyleCharacter(char c) {
		switch (c) {
			case 'S': return FormatStyle.SHORT;
			case 'M': return FormatStyle.MEDIUM;
			case 'L': return FormatStyle.LONG;
			case 'F': return FormatStyle.FULL;
			case '-': return null;
			default: throw new IllegalArgumentException("Invalid style character '" + c + "'");
		}
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
	 * <p>如果未定义特定模式或样式, 将使用{@link FormatStyle#MEDIUM 中日期时间格式}.
	 * 
	 * @return 新的日期时间格式化器
	 */
	public DateTimeFormatter createDateTimeFormatter() {
		return createDateTimeFormatter(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

	/**
	 * 使用此工厂创建一个新的{@code DateTimeFormatter}.
	 * <p>如果未定义特定模式或样式, 则将使用提供的 {@code fallbackFormatter}.
	 * 
	 * @param fallbackFormatter 在没有设置特定工厂属性时, 使用的后备格式化器 (can be {@code null}).
	 * 
	 * @return 新的日期时间格式化器
	 */
	public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
		DateTimeFormatter dateTimeFormatter = null;
		if (StringUtils.hasLength(this.pattern)) {
			// 使用严格的解析, 来与Joda-Time和标准的DateFormat行为保持一致:
			// 否则, 2月29日的非闰年不会被拒绝.
			// 但是, 使用严格的解析, 需要将年份数字指定为 'u'...
			String patternToUse = this.pattern.replace("yy", "uu");
			dateTimeFormatter = DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
		}
		else if (this.iso != null && this.iso != ISO.NONE) {
			switch (this.iso) {
				case DATE:
					dateTimeFormatter = DateTimeFormatter.ISO_DATE;
					break;
				case TIME:
					dateTimeFormatter = DateTimeFormatter.ISO_TIME;
					break;
				case DATE_TIME:
					dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
					break;
				case NONE:
					/* no-op */
					break;
				default:
					throw new IllegalStateException("Unsupported ISO format: " + this.iso);
			}
		}
		else if (this.dateStyle != null && this.timeStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(this.dateStyle, this.timeStyle);
		}
		else if (this.dateStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(this.dateStyle);
		}
		else if (this.timeStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(this.timeStyle);
		}

		if (dateTimeFormatter != null && this.timeZone != null) {
			dateTimeFormatter = dateTimeFormatter.withZone(this.timeZone.toZoneId());
		}
		return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
	}

}
