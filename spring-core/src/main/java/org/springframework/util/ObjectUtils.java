package org.springframework.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * <p>主要供框架内部使用.
 *
 * <p>Thanks to Alex Ruiz for contributing several enhancements to this class!
 */
public abstract class ObjectUtils {

	private static final int INITIAL_HASH = 7;
	private static final int MULTIPLIER = 31;

	private static final String EMPTY_STRING = "";
	private static final String NULL_STRING = "null";
	private static final String ARRAY_START = "{";
	private static final String ARRAY_END = "}";
	private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
	private static final String ARRAY_ELEMENT_SEPARATOR = ", ";


	/**
	 * 返回给定的throwable是否为受检的异常:
	 * 也就是说, 既不是RuntimeException也不是Error.
	 * 
	 * @param ex 要检查的throwable
	 * 
	 * @return 是否为受检的异常
	 */
	public static boolean isCheckedException(Throwable ex) {
		return !(ex instanceof RuntimeException || ex instanceof Error);
	}

	/**
	 * 检查给定的异常是否与throws子句中声明的指定异常类型兼容.
	 * 
	 * @param ex 要检查的异常
	 * @param declaredExceptions throws子句中声明的异常类型
	 * 
	 * @return 给定的异常是否兼容
	 */
	public static boolean isCompatibleWithThrowsClause(Throwable ex, Class<?>... declaredExceptions) {
		if (!isCheckedException(ex)) {
			return true;
		}
		if (declaredExceptions != null) {
			for (Class<?> declaredException : declaredExceptions) {
				if (declaredException.isInstance(ex)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 确定给定对象是否为数组:
	 * Object数组或基本类型数组.
	 * 
	 * @param obj 要检查的对象
	 */
	public static boolean isArray(Object obj) {
		return (obj != null && obj.getClass().isArray());
	}

	/**
	 * 确定给定数组是否为空:
	 * i.e. {@code null}或零.
	 * 
	 * @param array 要检查的数组
	 */
	public static boolean isEmpty(Object[] array) {
		return (array == null || array.length == 0);
	}

	/**
	 * 确定给定对象是否为空.
	 * <p>此方法支持以下对象类型.
	 * <ul>
	 * <li>{@code Array}: 如果长度为零则认为是空的</li>
	 * <li>{@link CharSequence}: 如果长度为零则认为是空的</li>
	 * <li>{@link Collection}: 委托给{@link Collection#isEmpty()}</li>
	 * <li>{@link Map}: 委托给{@link Map#isEmpty()}</li>
	 * </ul>
	 * <p>如果给定对象为非null且不是上述支持的类型之一, 则此方法返回{@code false}.
	 * 
	 * @param obj 要检查的对象
	 * 
	 * @return {@code true}如果对象是{@code null}或<em>空</em>
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object obj) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof CharSequence) {
			return ((CharSequence) obj).length() == 0;
		}
		if (obj.getClass().isArray()) {
			return Array.getLength(obj) == 0;
		}
		if (obj instanceof Collection) {
			return ((Collection) obj).isEmpty();
		}
		if (obj instanceof Map) {
			return ((Map) obj).isEmpty();
		}

		// else
		return false;
	}

	/**
	 * 检查给定数组是否包含给定元素.
	 * 
	 * @param array 要检查的数组 (可能是{@code null}, 在这种情况下, 返回值将始终为{@code false})
	 * @param element 要检查的元素
	 * 
	 * @return 是否在给定数组中找到了该元素
	 */
	public static boolean containsElement(Object[] array, Object element) {
		if (array == null) {
			return false;
		}
		for (Object arrayEle : array) {
			if (nullSafeEquals(arrayEle, element)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的枚举常量数组是否包含具有给定名称的常量, 在确定匹配时忽略大小写.
	 * 
	 * @param enumValues 要检查的枚举值, 通常是对 MyEnum.values()的调用的结果
	 * @param constant 要查找的常量名称 (不能为null或空字符串)
	 * 
	 * @return 是否已在给定数组中找到常量
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
		return containsConstant(enumValues, constant, false);
	}

	/**
	 * 检查给定的枚举常量数组是否包含具有给定名称的常量.
	 * 
	 * @param enumValues 要检查的枚举值, 通常是对 MyEnum.values()的调用的结果
	 * @param constant 要查找的常量名称 (不能为null或空字符串)
	 * @param caseSensitive 是否忽略大小写
	 * 
	 * @return 是否已在给定数组中找到常量
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
		for (Enum<?> candidate : enumValues) {
			if (caseSensitive ?
					candidate.toString().equals(constant) :
					candidate.toString().equalsIgnoreCase(constant)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 不区分大小写的替代{@link Enum#valueOf(Class, String)}.
	 * 
	 * @param <E> 具体的Enum类型
	 * @param enumValues 有问题的所有Enum常量的数组, 通常是每个 Enum.values()
	 * @param constant 获取枚举值的常量
	 * 
	 * @throws IllegalArgumentException 如果在给定的枚举值数组中找不到给定的常量.
	 * 使用{@link #containsConstant(Enum[], String)} 以避免此异常.
	 */
	public static <E extends Enum<?>> E caseInsensitiveValueOf(E[] enumValues, String constant) {
		for (E candidate : enumValues) {
			if (candidate.toString().equalsIgnoreCase(constant)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException(
				String.format("constant [%s] does not exist in enum type %s",
						constant, enumValues.getClass().getComponentType().getName()));
	}

	/**
	 * 将给定对象附加到给定数组, 返回一个由输入数组内容和给定对象组成的新数组.
	 * 
	 * @param array 要附加到的数组 (can be {@code null})
	 * @param obj 要附加的对象
	 * 
	 * @return 新数组 (相同组件类型; never {@code null})
	 */
	public static <A, O extends A> A[] addObjectToArray(A[] array, O obj) {
		Class<?> compType = Object.class;
		if (array != null) {
			compType = array.getClass().getComponentType();
		}
		else if (obj != null) {
			compType = obj.getClass();
		}
		int newArrLength = (array != null ? array.length + 1 : 1);
		@SuppressWarnings("unchecked")
		A[] newArr = (A[]) Array.newInstance(compType, newArrLength);
		if (array != null) {
			System.arraycopy(array, 0, newArr, 0, array.length);
		}
		newArr[newArr.length - 1] = obj;
		return newArr;
	}

	/**
	 * 将给定数组 (可以是基本类型数组) 转换为对象数组 (如果需要原始类型包装器对象).
	 * <p>{@code null}源值将转换为空的Object数组.
	 * 
	 * @param source (可以是基本类型)数组
	 * 
	 * @return 相应的对象数组 (never {@code null})
	 * @throws IllegalArgumentException 如果参数不是数组
	 */
	public static Object[] toObjectArray(Object source) {
		if (source instanceof Object[]) {
			return (Object[]) source;
		}
		if (source == null) {
			return new Object[0];
		}
		if (!source.getClass().isArray()) {
			throw new IllegalArgumentException("Source is not an array: " + source);
		}
		int length = Array.getLength(source);
		if (length == 0) {
			return new Object[0];
		}
		Class<?> wrapperType = Array.get(source, 0).getClass();
		Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
		for (int i = 0; i < length; i++) {
			newArray[i] = Array.get(source, i);
		}
		return newArray;
	}


	//---------------------------------------------------------------------
	// Convenience methods for content-based equality/hash-code handling
	//---------------------------------------------------------------------

	/**
	 * 确定给定的对象是否相等, 如果都是{@code null}, 则返回{@code true}; 如果只有一个是{@code null}, 则返回{@code false}.
	 * <p>使用{@code Arrays.equals}比较数组, 根据数组元素而不是数组引用执行相等性检查.
	 * 
	 * @param o1 第一个要比较的对象
	 * @param o2 第二个要比较的对象
	 * 
	 * @return 给定的对象是否相等
	 */
	public static boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			return arrayEquals(o1, o2);
		}
		return false;
	}

	/**
	 * 将给定的数组与{@code Arrays.equals}进行比较, 根据数组元素而不是数组引用执行相等性检查.
	 * 
	 * @param o1 第一个要比较的对象
	 * @param o2 第二个要比较的对象
	 * 
	 * @return 给定的对象是否相等
	 */
	private static boolean arrayEquals(Object o1, Object o2) {
		if (o1 instanceof Object[] && o2 instanceof Object[]) {
			return Arrays.equals((Object[]) o1, (Object[]) o2);
		}
		if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		}
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		}
		if (o1 instanceof char[] && o2 instanceof char[]) {
			return Arrays.equals((char[]) o1, (char[]) o2);
		}
		if (o1 instanceof double[] && o2 instanceof double[]) {
			return Arrays.equals((double[]) o1, (double[]) o2);
		}
		if (o1 instanceof float[] && o2 instanceof float[]) {
			return Arrays.equals((float[]) o1, (float[]) o2);
		}
		if (o1 instanceof int[] && o2 instanceof int[]) {
			return Arrays.equals((int[]) o1, (int[]) o2);
		}
		if (o1 instanceof long[] && o2 instanceof long[]) {
			return Arrays.equals((long[]) o1, (long[]) o2);
		}
		if (o1 instanceof short[] && o2 instanceof short[]) {
			return Arrays.equals((short[]) o1, (short[]) o2);
		}
		return false;
	}

	/**
	 * 返回给定对象的哈希码; 通常是{@code Object#hashCode()}}的值.
	 * 如果对象是数组, 则此方法将委托给此类中的数组的任何{@code nullSafeHashCode}方法.
	 * 如果对象是{@code null}, 则此方法返回0.
	 */
	public static int nullSafeHashCode(Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj.getClass().isArray()) {
			if (obj instanceof Object[]) {
				return nullSafeHashCode((Object[]) obj);
			}
			if (obj instanceof boolean[]) {
				return nullSafeHashCode((boolean[]) obj);
			}
			if (obj instanceof byte[]) {
				return nullSafeHashCode((byte[]) obj);
			}
			if (obj instanceof char[]) {
				return nullSafeHashCode((char[]) obj);
			}
			if (obj instanceof double[]) {
				return nullSafeHashCode((double[]) obj);
			}
			if (obj instanceof float[]) {
				return nullSafeHashCode((float[]) obj);
			}
			if (obj instanceof int[]) {
				return nullSafeHashCode((int[]) obj);
			}
			if (obj instanceof long[]) {
				return nullSafeHashCode((long[]) obj);
			}
			if (obj instanceof short[]) {
				return nullSafeHashCode((short[]) obj);
			}
		}
		return obj.hashCode();
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array}是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(Object[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (Object element : array) {
			hash = MULTIPLIER * hash + nullSafeHashCode(element);
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(boolean[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (boolean element : array) {
			hash = MULTIPLIER * hash + hashCode(element);
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(byte[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (byte element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(char[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (char element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(double[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (double element : array) {
			hash = MULTIPLIER * hash + hashCode(element);
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(float[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (float element : array) {
			hash = MULTIPLIER * hash + hashCode(element);
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(int[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (int element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(long[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (long element : array) {
			hash = MULTIPLIER * hash + hashCode(element);
		}
		return hash;
	}

	/**
	 * 根据指定数组的内容返回哈希码.
	 * 如果{@code array} 是{@code null}, 则此方法返回 0.
	 */
	public static int nullSafeHashCode(short[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (short element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 返回与{@link Boolean#hashCode()}}相同的值.
	 */
	public static int hashCode(boolean bool) {
		return (bool ? 1231 : 1237);
	}

	/**
	 * 返回与{@link Double#hashCode()}}相同的值.
	 */
	public static int hashCode(double dbl) {
		return hashCode(Double.doubleToLongBits(dbl));
	}

	/**
	 * 返回与{@link Float#hashCode()}}相同的值.
	 */
	public static int hashCode(float flt) {
		return Float.floatToIntBits(flt);
	}

	/**
	 * 返回与{@link Long#hashCode()}}相同的值.
	 */
	public static int hashCode(long lng) {
		return (int) (lng ^ (lng >>> 32));
	}


	//---------------------------------------------------------------------
	// Convenience methods for toString output
	//---------------------------------------------------------------------

	/**
	 * 返回对象的整体标识的String表示形式.
	 * 
	 * @param obj 对象 (may be {@code null})
	 * 
	 * @return 对象的标识为String表示形式, 如果对象为{@code null}则为空字符串
	 */
	public static String identityToString(Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return obj.getClass().getName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * 返回对象的标识哈希码的十六进制字符串形式.
	 * 
	 * @param obj 对象
	 * 
	 * @return 对象的标识哈希码
	 */
	public static String getIdentityHexString(Object obj) {
		return Integer.toHexString(System.identityHashCode(obj));
	}

	/**
	 * 如果{@code obj}不是{@code null}, 则返回基于内容的字符串表示形式; 否则返回一个空字符串.
	 * <p>与{@link #nullSafeToString(Object)}的不同之处在于它为{@code null}值返回一个空字符串而不是 "null".
	 * 
	 * @param obj 要构建显示字符串的对象
	 * 
	 * @return {@code obj}的显示字符串表示
	 */
	public static String getDisplayString(Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return nullSafeToString(obj);
	}

	/**
	 * 确定给定对象的类名.
	 * <p>如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param obj 要内省的对象 (may be {@code null})
	 * 
	 * @return 相应的类名
	 */
	public static String nullSafeClassName(Object obj) {
		return (obj != null ? obj.getClass().getName() : NULL_STRING);
	}

	/**
	 * 返回指定Object的String表示形式.
	 * <p>在数组的情况下构建内容的String表示.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param obj 为其构建String表示的对象
	 * 
	 * @return {@code obj}的字符串表示
	 */
	public static String nullSafeToString(Object obj) {
		if (obj == null) {
			return NULL_STRING;
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		if (obj instanceof Object[]) {
			return nullSafeToString((Object[]) obj);
		}
		if (obj instanceof boolean[]) {
			return nullSafeToString((boolean[]) obj);
		}
		if (obj instanceof byte[]) {
			return nullSafeToString((byte[]) obj);
		}
		if (obj instanceof char[]) {
			return nullSafeToString((char[]) obj);
		}
		if (obj instanceof double[]) {
			return nullSafeToString((double[]) obj);
		}
		if (obj instanceof float[]) {
			return nullSafeToString((float[]) obj);
		}
		if (obj instanceof int[]) {
			return nullSafeToString((int[]) obj);
		}
		if (obj instanceof long[]) {
			return nullSafeToString((long[]) obj);
		}
		if (obj instanceof short[]) {
			return nullSafeToString((short[]) obj);
		}
		String str = obj.toString();
		return (str != null ? str : EMPTY_STRING);
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(Object[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(String.valueOf(array[i]));
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(boolean[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}

			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(byte[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(char[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append("'").append(array[i]).append("'");
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(double[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}

			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(float[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}

			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(int[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(long[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}

	/**
	 * 返回指定数组内容的String表示形式.
	 * <p>由数组元素的列表组成的String表示, 用大括号括起来 ({@code "{}"}).
	 * 相邻元素由字符 {@code ", "} (逗号后跟空格)分隔.
	 * 如果{@code obj}是{@code null}, 则返回{@code "null"}.
	 * 
	 * @param array 为其构建String表示的数组
	 * 
	 * @return {@code array}的String表示
	 */
	public static String nullSafeToString(short[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				sb.append(ARRAY_START);
			}
			else {
				sb.append(ARRAY_ELEMENT_SEPARATOR);
			}
			sb.append(array[i]);
		}
		sb.append(ARRAY_END);
		return sb.toString();
	}
}
