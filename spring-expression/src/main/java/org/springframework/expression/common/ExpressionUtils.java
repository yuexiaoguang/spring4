package org.springframework.expression.common;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.util.ClassUtils;

/**
 * 可由任何表达式语言提供程序使用的常用工具方法.
 */
public abstract class ExpressionUtils {

	/**
	 * 确定指定上下文中是否有可用的类型转换器, 并尝试使用它, 将提供的值转换为指定的类型.
	 * 如果无法进行转换, 则会引发异常.
	 * 
	 * @param context 可以定义类型转换器的评估上下文
	 * @param typedValue 要转换的值和描述它的类型描述符
	 * @param targetType 尝试转换为的类型
	 * 
	 * @return 转换后的值
	 * @throws EvaluationException 如果在转换过程中出现问题, 或者不支持将值转换为指定类型
	 */
	@SuppressWarnings("unchecked")
	public static <T> T convertTypedValue(EvaluationContext context, TypedValue typedValue, Class<T> targetType) {
		Object value = typedValue.getValue();
		if (targetType == null) {
			return (T) value;
		}
		if (context != null) {
			return (T) context.getTypeConverter().convertValue(
					value, typedValue.getTypeDescriptor(), TypeDescriptor.valueOf(targetType));
		}
		if (ClassUtils.isAssignableValue(targetType, value)) {
			return (T) value;
		}
		throw new EvaluationException("Cannot convert value '" + value + "' to type '" + targetType.getName() + "'");
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为int.
	 */
	public static int toInt(TypeConverter typeConverter, TypedValue typedValue) {
		return (Integer) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Integer.class));
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为boolean值.
	 */
	public static boolean toBoolean(TypeConverter typeConverter, TypedValue typedValue) {
		return (Boolean) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Boolean.class));
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为double.
	 */
	public static double toDouble(TypeConverter typeConverter, TypedValue typedValue) {
		return (Double) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Double.class));
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为long.
	 */
	public static long toLong(TypeConverter typeConverter, TypedValue typedValue) {
		return (Long) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Long.class));
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为char.
	 */
	public static char toChar(TypeConverter typeConverter, TypedValue typedValue) {
		return (Character) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Character.class));
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为short.
	 */
	public static short toShort(TypeConverter typeConverter, TypedValue typedValue) {
		return (Short) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Short.class));
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为float.
	 */
	public static float toFloat(TypeConverter typeConverter, TypedValue typedValue) {
		return (Float) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Float.class));
	}

	/**
	 * 尝试使用提供的类型转换器将类型值转换为byte.
	 */
	public static byte toByte(TypeConverter typeConverter, TypedValue typedValue) {
		return (Byte) typeConverter.convertValue(typedValue.getValue(), typedValue.getTypeDescriptor(),
				TypeDescriptor.valueOf(Byte.class));
	}

}
