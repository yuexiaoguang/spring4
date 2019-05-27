package org.springframework.scheduling.support;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.springframework.util.StringUtils;

/**
 * <a href="http://www.manpagez.com/man/5/crontab/">Crontab pattern</a>日期序列生成器,
 * 允许客户端指定序列匹配的模式.
 *
 * <p>该模式是六个单独的空格分隔字段的列表:
 * 表示秒, 分, 时, 日, 月, 周.
 * 月份和工作日名称可以作为英文名称的前三个字母.
 *
 * <p>Example patterns:
 * <ul>
 * <li>"0 0 * * * *" = 每天每小时的最高点.</li>
 * <li>"*&#47;10 * * * * *" = 每十秒.</li>
 * <li>"0 0 8-10 * * *" = 每天的8, 9 和 10点.</li>
 * <li>"0 0 6,19 * * *" = 每天6:00 AM 和 7:00 PM.</li>
 * <li>"0 0/30 8-10 * * *" = 每天8:00, 8:30, 9:00, 9:30, 10:00 和 10:30.</li>
 * <li>"0 0 9-17 * * MON-FRI" = 在工作日的9至5时</li>
 * <li>"0 0 0 25 12 ?" = 每个圣诞节的午夜</li>
 * </ul>
 */
public class CronSequenceGenerator {

	private final String expression;

	private final TimeZone timeZone;

	private final BitSet months = new BitSet(12);

	private final BitSet daysOfMonth = new BitSet(31);

	private final BitSet daysOfWeek = new BitSet(7);

	private final BitSet hours = new BitSet(24);

	private final BitSet minutes = new BitSet(60);

	private final BitSet seconds = new BitSet(60);


	/**
	 * 使用默认的{@link TimeZone}.
	 * 
	 * @param expression 以空格分隔的时间字段列表
	 * 
	 * @throws IllegalArgumentException 如果模式无法解析
	 */
	public CronSequenceGenerator(String expression) {
		this(expression, TimeZone.getDefault());
	}

	/**
	 * 使用指定的{@link TimeZone}.
	 * 
	 * @param expression 以空格分隔的时间字段列表
	 * @param timeZone 用于生成触发时间的TimeZone
	 * 
	 * @throws IllegalArgumentException 如果模式无法解析
	 */
	public CronSequenceGenerator(String expression, TimeZone timeZone) {
		this.expression = expression;
		this.timeZone = timeZone;
		parse(expression);
	}

	private CronSequenceGenerator(String expression, String[] fields) {
		this.expression = expression;
		this.timeZone = null;
		doParse(fields);
	}


	/**
	 * 返回为此序列生成器构建的cron模式.
	 */
	String getExpression() {
		return this.expression;
	}


	/**
	 * 在与Cron模式匹配的序列中以及在提供的值之后的下一个{@link Date}.
	 * 返回值将具有整数秒, 并且将在输入值之后.
	 * 
	 * @param date 种子值
	 * 
	 * @return 与模式匹配的下一个值
	 */
	public Date next(Date date) {
		/*
		The plan:

		1 从整秒开始 (必要时四舍五入)

		2 如果秒匹配继续, 否则找到下一个匹配:
		2.1 如果下一个匹配是在下一分钟, 那么前滚

		3 如果分钟匹配继续, 否则找到下一个匹配
		3.1 如果下一个匹配是在下一个小时, 那么前滚
		3.2 重置秒数并转到2

		4 如果小时匹配继续, 否则找到下一个匹配
		4.1 如果下一个匹配是在第二天, 那么前滚
		4.2 重置分钟和秒, 然后转到2
		*/

		Calendar calendar = new GregorianCalendar();
		calendar.setTimeZone(this.timeZone);
		calendar.setTime(date);

		// 首先, 只需重置毫秒, 并尝试从那里进行计算...
		calendar.set(Calendar.MILLISECOND, 0);
		long originalTimestamp = calendar.getTimeInMillis();
		doNext(calendar, calendar.get(Calendar.YEAR));

		if (calendar.getTimeInMillis() == originalTimestamp) {
			// 到达了原来的时间戳 - 前滚到下一秒, 然后再试一次...
			calendar.add(Calendar.SECOND, 1);
			doNext(calendar, calendar.get(Calendar.YEAR));
		}

		return calendar.getTime();
	}

	private void doNext(Calendar calendar, int dot) {
		List<Integer> resets = new ArrayList<Integer>();

		int second = calendar.get(Calendar.SECOND);
		List<Integer> emptyList = Collections.emptyList();
		int updateSecond = findNext(this.seconds, second, calendar, Calendar.SECOND, Calendar.MINUTE, emptyList);
		if (second == updateSecond) {
			resets.add(Calendar.SECOND);
		}

		int minute = calendar.get(Calendar.MINUTE);
		int updateMinute = findNext(this.minutes, minute, calendar, Calendar.MINUTE, Calendar.HOUR_OF_DAY, resets);
		if (minute == updateMinute) {
			resets.add(Calendar.MINUTE);
		}
		else {
			doNext(calendar, dot);
		}

		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int updateHour = findNext(this.hours, hour, calendar, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_WEEK, resets);
		if (hour == updateHour) {
			resets.add(Calendar.HOUR_OF_DAY);
		}
		else {
			doNext(calendar, dot);
		}

		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
		int updateDayOfMonth = findNextDay(calendar, this.daysOfMonth, dayOfMonth, daysOfWeek, dayOfWeek, resets);
		if (dayOfMonth == updateDayOfMonth) {
			resets.add(Calendar.DAY_OF_MONTH);
		}
		else {
			doNext(calendar, dot);
		}

		int month = calendar.get(Calendar.MONTH);
		int updateMonth = findNext(this.months, month, calendar, Calendar.MONTH, Calendar.YEAR, resets);
		if (month != updateMonth) {
			if (calendar.get(Calendar.YEAR) - dot > 4) {
				throw new IllegalArgumentException("Invalid cron expression \"" + this.expression +
						"\" led to runaway search for next trigger");
			}
			doNext(calendar, dot);
		}

	}

	private int findNextDay(Calendar calendar, BitSet daysOfMonth, int dayOfMonth, BitSet daysOfWeek, int dayOfWeek,
			List<Integer> resets) {

		int count = 0;
		int max = 366;
		// java.util.Calendar中的DAY_OF_WEEK从 1 (Sunday)开始,
		// 但是在cron模式中, 它们从0开始, 所以我们在这里减去1
		while ((!daysOfMonth.get(dayOfMonth) || !daysOfWeek.get(dayOfWeek - 1)) && count++ < max) {
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
			dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			reset(calendar, resets);
		}
		if (count >= max) {
			throw new IllegalArgumentException("Overflow in day for expression \"" + this.expression + "\"");
		}
		return dayOfMonth;
	}

	/**
	 * 在提供的值之后搜索为下一个设置位提供的位, 然后重置日历.
	 * 
	 * @param bits 表示该字段的允许值的{@link BitSet}
	 * @param value 该字段的当前值
	 * @param calendar 当移动位时, 日历会递增
	 * @param field 要在日历中递增的字段 (@see {@link Calendar} 用于定义有效字段的静态常量)
	 * @param lowerOrders 应重置的Calendar字段ID (i.e. 重要性低于感兴趣的字段)
	 * 
	 * @return 序列中下一个日历字段的值
	 */
	private int findNext(BitSet bits, int value, Calendar calendar, int field, int nextField, List<Integer> lowerOrders) {
		int nextValue = bits.nextSetBit(value);
		// roll over if needed
		if (nextValue == -1) {
			calendar.add(nextField, 1);
			reset(calendar, Collections.singletonList(field));
			nextValue = bits.nextSetBit(0);
		}
		if (nextValue != value) {
			calendar.set(field, nextValue);
			reset(calendar, lowerOrders);
		}
		return nextValue;
	}

	/**
	 * 重置所有提供的字段为零.
	 */
	private void reset(Calendar calendar, List<Integer> fields) {
		for (int field : fields) {
			calendar.set(field, field == Calendar.DAY_OF_MONTH ? 1 : 0);
		}
	}


	// Parsing logic invoked by the constructor

	/**
	 * 解析给定的模式表达式.
	 */
	private void parse(String expression) throws IllegalArgumentException {
		String[] fields = StringUtils.tokenizeToStringArray(expression, " ");
		if (!areValidCronFields(fields)) {
			throw new IllegalArgumentException(String.format(
					"Cron expression must consist of 6 fields (found %d in \"%s\")", fields.length, expression));
		}
		doParse(fields);
	}

	private void doParse(String[] fields) {
		setNumberHits(this.seconds, fields[0], 0, 60);
		setNumberHits(this.minutes, fields[1], 0, 60);
		setNumberHits(this.hours, fields[2], 0, 24);
		setDaysOfMonth(this.daysOfMonth, fields[3]);
		setMonths(this.months, fields[4]);
		setDays(this.daysOfWeek, replaceOrdinals(fields[5], "SUN,MON,TUE,WED,THU,FRI,SAT"), 8);

		if (this.daysOfWeek.get(7)) {
			// Sunday can be represented as 0 or 7
			this.daysOfWeek.set(0);
			this.daysOfWeek.clear(7);
		}
	}

	/**
	 * 将逗号分隔列表中的值 (不区分大小写)替换为列表中的索引.
	 * 
	 * @return 一个新的String, 替换列表中的值
	 */
	private String replaceOrdinals(String value, String commaSeparatedList) {
		String[] list = StringUtils.commaDelimitedListToStringArray(commaSeparatedList);
		for (int i = 0; i < list.length; i++) {
			String item = list[i].toUpperCase();
			value = StringUtils.replace(value.toUpperCase(), item, "" + i);
		}
		return value;
	}

	private void setDaysOfMonth(BitSet bits, String field) {
		int max = 31;
		// Days of month start with 1 (in Cron and Calendar) so add one
		setDays(bits, field, max + 1);
		// ... and remove it from the front
		bits.clear(0);
	}

	private void setDays(BitSet bits, String field, int max) {
		if (field.contains("?")) {
			field = "*";
		}
		setNumberHits(bits, field, 0, max);
	}

	private void setMonths(BitSet bits, String value) {
		int max = 12;
		value = replaceOrdinals(value, "FOO,JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC");
		BitSet months = new BitSet(13);
		// Months start with 1 in Cron and 0 in Calendar, so push the values first into a longer bit set
		setNumberHits(months, value, 1, max + 1);
		// ... and then rotate it to the front of the months
		for (int i = 1; i <= max; i++) {
			if (months.get(i)) {
				bits.set(i - 1);
			}
		}
	}

	private void setNumberHits(BitSet bits, String value, int min, int max) {
		String[] fields = StringUtils.delimitedListToStringArray(value, ",");
		for (String field : fields) {
			if (!field.contains("/")) {
				// 不是增量器, 所以它必须是一个范围 (可能为空)
				int[] range = getRange(field, min, max);
				bits.set(range[0], range[1] + 1);
			}
			else {
				String[] split = StringUtils.delimitedListToStringArray(field, "/");
				if (split.length > 2) {
					throw new IllegalArgumentException("Incrementer has more than two fields: '" +
							field + "' in expression \"" + this.expression + "\"");
				}
				int[] range = getRange(split[0], min, max);
				if (!split[0].contains("-")) {
					range[1] = max - 1;
				}
				int delta = Integer.parseInt(split[1]);
				if (delta <= 0) {
					throw new IllegalArgumentException("Incrementer delta must be 1 or higher: '" +
							field + "' in expression \"" + this.expression + "\"");
				}
				for (int i = range[0]; i <= range[1]; i += delta) {
					bits.set(i);
				}
			}
		}
	}

	private int[] getRange(String field, int min, int max) {
		int[] result = new int[2];
		if (field.contains("*")) {
			result[0] = min;
			result[1] = max - 1;
			return result;
		}
		if (!field.contains("-")) {
			result[0] = result[1] = Integer.valueOf(field);
		}
		else {
			String[] split = StringUtils.delimitedListToStringArray(field, "-");
			if (split.length > 2) {
				throw new IllegalArgumentException("Range has more than two fields: '" +
						field + "' in expression \"" + this.expression + "\"");
			}
			result[0] = Integer.valueOf(split[0]);
			result[1] = Integer.valueOf(split[1]);
		}
		if (result[0] >= max || result[1] >= max) {
			throw new IllegalArgumentException("Range exceeds maximum (" + max + "): '" +
					field + "' in expression \"" + this.expression + "\"");
		}
		if (result[0] < min || result[1] < min) {
			throw new IllegalArgumentException("Range less than minimum (" + min + "): '" +
					field + "' in expression \"" + this.expression + "\"");
		}
		if (result[0] > result[1]) {
			throw new IllegalArgumentException("Invalid inverted range: '" + field +
					"' in expression \"" + this.expression + "\"");
		}
		return result;
	}


	/**
	 * 确定指定的表达式是否表示有效的cron模式.
	 * 
	 * @param expression 要评估的表达式
	 * 
	 * @return {@code true} 如果给定的表达式是有效的cron表达式
	 */
	public static boolean isValidExpression(String expression) {
		if (expression == null) {
			return false;
		}
		String[] fields = StringUtils.tokenizeToStringArray(expression, " ");
		if (!areValidCronFields(fields)) {
			return false;
		}
		try {
			new CronSequenceGenerator(expression, fields);
			return true;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static boolean areValidCronFields(String[] fields) {
		return (fields != null && fields.length == 6);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CronSequenceGenerator)) {
			return false;
		}
		CronSequenceGenerator otherCron = (CronSequenceGenerator) other;
		return (this.months.equals(otherCron.months) && this.daysOfMonth.equals(otherCron.daysOfMonth) &&
				this.daysOfWeek.equals(otherCron.daysOfWeek) && this.hours.equals(otherCron.hours) &&
				this.minutes.equals(otherCron.minutes) && this.seconds.equals(otherCron.seconds));
	}

	@Override
	public int hashCode() {
		return (17 * this.months.hashCode() + 29 * this.daysOfMonth.hashCode() + 37 * this.daysOfWeek.hashCode() +
				41 * this.hours.hashCode() + 53 * this.minutes.hashCode() + 61 * this.seconds.hashCode());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + this.expression;
	}

}
