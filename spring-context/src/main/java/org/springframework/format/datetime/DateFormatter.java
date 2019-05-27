package org.springframework.format.datetime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.format.Formatter;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.util.StringUtils;

/**
 * {@link java.util.Date}类型的格式化器.
 * 允许配置明确的日期模式和区域设置.
 */
public class DateFormatter implements Formatter<Date> {

	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	private static final Map<ISO, String> ISO_PATTERNS;

	static {
		Map<ISO, String> formats = new EnumMap<ISO, String>(ISO.class);
		formats.put(ISO.DATE, "yyyy-MM-dd");
		formats.put(ISO.TIME, "HH:mm:ss.SSSZ");
		formats.put(ISO.DATE_TIME, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		ISO_PATTERNS = Collections.unmodifiableMap(formats);
	}


	private String pattern;

	private int style = DateFormat.DEFAULT;

	private String stylePattern;

	private ISO iso;

	private TimeZone timeZone;

	private boolean lenient = false;


	public DateFormatter() {
	}

	public DateFormatter(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * 设置用于格式化日期值的模式.
	 * <p>如果未指定, 将使用DateFormat的默认样式.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 设置用于此日期的ISO格式.
	 * 
	 * @param iso {@link ISO}格式
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * 设置用于格式化日期值的样式.
	 * <p>如果未指定, 将使用DateFormat的默认样式.
	 */
	public void setStyle(int style) {
		this.style = style;
	}

	/**
	 * 设置要用于格式化日期值的两个字符.
	 * 第一个字符用于日期样式, 第二个字符用于时间样式.
	 * 支持的字符是
	 * <ul>
	 * <li>'S' = Small</li>
	 * <li>'M' = Medium</li>
	 * <li>'L' = Long</li>
	 * <li>'F' = Full</li>
	 * <li>'-' = Omitted</li>
	 * <ul>
	 * 此方法模仿Joda-Time支持的样式.
	 * 
	 * @param stylePattern {"S", "M", "L", "F", "-"}中的两个字符
	 */
	public void setStylePattern(String stylePattern) {
		this.stylePattern = stylePattern;
	}

	/**
	 * 设置标准化日期值的TimeZone.
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 指定解析是否宽松. 默认是 false.
	 * <p>使用宽松解析, 解析器可能允许与格式不完全匹配的输入.
	 * 对于严格的解析, 输入必须与格式完全匹配.
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	@Override
	public String print(Date date, Locale locale) {
		return getDateFormat(locale).format(date);
	}

	@Override
	public Date parse(String text, Locale locale) throws ParseException {
		return getDateFormat(locale).parse(text);
	}


	protected DateFormat getDateFormat(Locale locale) {
		DateFormat dateFormat = createDateFormat(locale);
		if (this.timeZone != null) {
			dateFormat.setTimeZone(this.timeZone);
		}
		dateFormat.setLenient(this.lenient);
		return dateFormat;
	}

	private DateFormat createDateFormat(Locale locale) {
		if (StringUtils.hasLength(this.pattern)) {
			return new SimpleDateFormat(this.pattern, locale);
		}
		if (this.iso != null && this.iso != ISO.NONE) {
			String pattern = ISO_PATTERNS.get(this.iso);
			if (pattern == null) {
				throw new IllegalStateException("Unsupported ISO format " + this.iso);
			}
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			format.setTimeZone(UTC);
			return format;
		}
		if (StringUtils.hasLength(this.stylePattern)) {
			int dateStyle = getStylePatternForChar(0);
			int timeStyle = getStylePatternForChar(1);
			if (dateStyle != -1 && timeStyle != -1) {
				return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
			}
			if (dateStyle != -1) {
				return DateFormat.getDateInstance(dateStyle, locale);
			}
			if (timeStyle != -1) {
				return DateFormat.getTimeInstance(timeStyle, locale);
			}
			throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");

		}
		return DateFormat.getDateInstance(this.style, locale);
	}

	private int getStylePatternForChar(int index) {
		if (this.stylePattern != null && this.stylePattern.length() > index) {
			switch (this.stylePattern.charAt(index)) {
				case 'S': return DateFormat.SHORT;
				case 'M': return DateFormat.MEDIUM;
				case 'L': return DateFormat.LONG;
				case 'F': return DateFormat.FULL;
				case '-': return -1;
			}
		}
		throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");
	}

}
