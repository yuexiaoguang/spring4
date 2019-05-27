package org.springframework.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用于数字转换和解析的其他实用程序方法.
 * <p>主要供框架内部使用; 考虑Apache的Commons Lang提供更全面的数字工具套件.
 */
public abstract class NumberUtils {

	private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

	private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

	/**
	 * 标准数字类型 (全部不可变):
	 * Byte, Short, Integer, Long, BigInteger, Float, Double, BigDecimal.
	 */
	public static final Set<Class<?>> STANDARD_NUMBER_TYPES;

	static {
		Set<Class<?>> numberTypes = new HashSet<Class<?>>(8);
		numberTypes.add(Byte.class);
		numberTypes.add(Short.class);
		numberTypes.add(Integer.class);
		numberTypes.add(Long.class);
		numberTypes.add(BigInteger.class);
		numberTypes.add(Float.class);
		numberTypes.add(Double.class);
		numberTypes.add(BigDecimal.class);
		STANDARD_NUMBER_TYPES = Collections.unmodifiableSet(numberTypes);
	}


	/**
	 * 将给定数字转换为给定目标类的实例.
	 * 
	 * @param number 要转换的数字
	 * @param targetClass 要转换为的目标类
	 * 
	 * @return 转换后的数字
	 * @throws IllegalArgumentException 如果不支持目标类 (i.e. 不是JDK中包含的标准Number子类)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T convertNumberToTargetClass(Number number, Class<T> targetClass)
			throws IllegalArgumentException {

		Assert.notNull(number, "Number must not be null");
		Assert.notNull(targetClass, "Target class must not be null");

		if (targetClass.isInstance(number)) {
			return (T) number;
		}
		else if (Byte.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return (T) Byte.valueOf(number.byteValue());
		}
		else if (Short.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return (T) Short.valueOf(number.shortValue());
		}
		else if (Integer.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return (T) Integer.valueOf(number.intValue());
		}
		else if (Long.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			return (T) Long.valueOf(value);
		}
		else if (BigInteger.class == targetClass) {
			if (number instanceof BigDecimal) {
				// 不要失去精度 - 使用BigDecimal自己的转换
				return (T) ((BigDecimal) number).toBigInteger();
			}
			else {
				// 原始值不是 Big* 数字 - 使用标准long转换
				return (T) BigInteger.valueOf(number.longValue());
			}
		}
		else if (Float.class == targetClass) {
			return (T) Float.valueOf(number.floatValue());
		}
		else if (Double.class == targetClass) {
			return (T) Double.valueOf(number.doubleValue());
		}
		else if (BigDecimal.class == targetClass) {
			// 这里总是使用 BigDecimal(String) 来避免BigDecimal的不可预测性 BigDecimal(double)
			// (see BigDecimal javadoc for details)
			return (T) new BigDecimal(number.toString());
		}
		else {
			throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
					number.getClass().getName() + "] to unsupported target class [" + targetClass.getName() + "]");
		}
	}

	/**
	 * 在将给定数字作为long值返回之前, 检查 {@code BigInteger}/{@code BigDecimal} long 溢出.
	 * 
	 * @param number 要转换的数字
	 * @param targetClass 要转换为的目标类
	 * 
	 * @return long值, 如果可转换, 没有溢出
	 * @throws IllegalArgumentException 溢出
	 */
	private static long checkedLongValue(Number number, Class<? extends Number> targetClass) {
		BigInteger bigInt = null;
		if (number instanceof BigInteger) {
			bigInt = (BigInteger) number;
		}
		else if (number instanceof BigDecimal) {
			bigInt = ((BigDecimal) number).toBigInteger();
		}
		// 有效地类似于JDK 8的 BigInteger.longValueExact()
		if (bigInt != null && (bigInt.compareTo(LONG_MIN) < 0 || bigInt.compareTo(LONG_MAX) > 0)) {
			raiseOverflowException(number, targetClass);
		}
		return number.longValue();
	}

	/**
	 * 为给定的数字和目标类引发<em>溢出</em>异常.
	 * 
	 * @param number 要转换的数字
	 * @param targetClass 要转换为的目标类
	 * 
	 * @throws IllegalArgumentException 溢出
	 */
	private static void raiseOverflowException(Number number, Class<?> targetClass) {
		throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
				number.getClass().getName() + "] to target class [" + targetClass.getName() + "]: overflow");
	}

	/**
	 * 使用相应的{@code decode} / {@code valueOf}方法将给定的{@code text}解析为给定目标类的{@link Number}实例.
	 * <p>在尝试解析数字之前修剪输入的{@code String}.
	 * <p>支持十六进制格式的数字 ("0x", "0X", 或"#"开头).
	 * 
	 * @param text 要转换的文本
	 * @param targetClass 要解析为的文本
	 * 
	 * @return 已解析的数字
	 * @throws IllegalArgumentException 如果不支持目标类 (i.e. 不是JDK中包含的标准Number子类)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T parseNumber(String text, Class<T> targetClass) {
		Assert.notNull(text, "Text must not be null");
		Assert.notNull(targetClass, "Target class must not be null");
		String trimmed = StringUtils.trimAllWhitespace(text);

		if (Byte.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Byte.decode(trimmed) : Byte.valueOf(trimmed));
		}
		else if (Short.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Short.decode(trimmed) : Short.valueOf(trimmed));
		}
		else if (Integer.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer.valueOf(trimmed));
		}
		else if (Long.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Long.decode(trimmed) : Long.valueOf(trimmed));
		}
		else if (BigInteger.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? decodeBigInteger(trimmed) : new BigInteger(trimmed));
		}
		else if (Float.class == targetClass) {
			return (T) Float.valueOf(trimmed);
		}
		else if (Double.class == targetClass) {
			return (T) Double.valueOf(trimmed);
		}
		else if (BigDecimal.class == targetClass || Number.class == targetClass) {
			return (T) new BigDecimal(trimmed);
		}
		else {
			throw new IllegalArgumentException(
					"Cannot convert String [" + text + "] to target class [" + targetClass.getName() + "]");
		}
	}

	/**
	 * 使用提供的{@link NumberFormat}将给定的{@code text}解析为给定目标类的{@link Number}实例.
	 * <p>在尝试解析数字之前修剪输入的{@code String}.
	 * 
	 * @param text 要转换的文本
	 * @param targetClass 要解析为的文本
	 * @param numberFormat 用于解析的{@code NumberFormat} (如果是{@code null}, 此方法可以回退到{@link #parseNumber(String, Class)})
	 * 
	 * @return 已解析的数字
	 * @throws IllegalArgumentException 如果不支持目标类 (i.e. 不是JDK中包含的标准Number子类)
	 */
	public static <T extends Number> T parseNumber(String text, Class<T> targetClass, NumberFormat numberFormat) {
		if (numberFormat != null) {
			Assert.notNull(text, "Text must not be null");
			Assert.notNull(targetClass, "Target class must not be null");
			DecimalFormat decimalFormat = null;
			boolean resetBigDecimal = false;
			if (numberFormat instanceof DecimalFormat) {
				decimalFormat = (DecimalFormat) numberFormat;
				if (BigDecimal.class == targetClass && !decimalFormat.isParseBigDecimal()) {
					decimalFormat.setParseBigDecimal(true);
					resetBigDecimal = true;
				}
			}
			try {
				Number number = numberFormat.parse(StringUtils.trimAllWhitespace(text));
				return convertNumberToTargetClass(number, targetClass);
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Could not parse number: " + ex.getMessage());
			}
			finally {
				if (resetBigDecimal) {
					decimalFormat.setParseBigDecimal(false);
				}
			}
		}
		else {
			return parseNumber(text, targetClass);
		}
	}

	/**
	 * 确定给定的{@code value}字符串是否表示十六进制数,
	 * i.e. 需要传递到{@code Integer.decode}, 而不是{@code Integer.valueOf}, 等.
	 */
	private static boolean isHexNumber(String value) {
		int index = (value.startsWith("-") ? 1 : 0);
		return (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index));
	}

	/**
	 * 从提供的{@link String}值解码{@link java.math.BigInteger}.
	 * <p>支持十进制, 十六进制和八进制表示法.
	 */
	private static BigInteger decodeBigInteger(String value) {
		int radix = 10;
		int index = 0;
		boolean negative = false;

		// Handle minus sign, if present.
		if (value.startsWith("-")) {
			negative = true;
			index++;
		}

		// Handle radix specifier, if present.
		if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
			index += 2;
			radix = 16;
		}
		else if (value.startsWith("#", index)) {
			index++;
			radix = 16;
		}
		else if (value.startsWith("0", index) && value.length() > 1 + index) {
			index++;
			radix = 8;
		}

		BigInteger result = new BigInteger(value.substring(index), radix);
		return (negative ? result.negate() : result);
	}
}
