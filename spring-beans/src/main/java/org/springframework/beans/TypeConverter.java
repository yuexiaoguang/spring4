package org.springframework.beans;

import java.lang.reflect.Field;

import org.springframework.core.MethodParameter;

/**
 * 定义类型转换方法的接口. 通常（但不一定）与{@link PropertyEditorRegistry}接口一起实现.
 *
 * <p><b>Note:</b> 由于TypeConverter实现通常基于非线程安全的{@link java.beans.PropertyEditor PropertyEditors},
 * TypeConverters本身也不被认为是线程安全的.
 */
public interface TypeConverter {

	/**
	 * 将值转换为所需类型 (if necessary from a String).
	 * <p>从String到任何类型的转换通常使用PropertyEditor类的{@code setAsText}方法, 或ConversionService中的Spring转换器.
	 * 
	 * @param value 要转换的值
	 * @param requiredType 所需的类型(or {@code null}, 例如在集合元素的情况下)
	 * 
	 * @return 新值, 可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 */
	<T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException;

	/**
	 * 将值转换为所需类型 (if necessary from a String).
	 * <p>从String到任何类型的转换通常使用PropertyEditor类的{@code setAsText}方法, 或ConversionService中的Spring转换器.
	 * 
	 * @param value 要转换的值
	 * @param requiredType 所需的类型(or {@code null}, 例如在集合元素的情况下)
	 * @param methodParam 作为转换目标的方法参数 (用于分析泛型类型; may be {@code null})
	 * 
	 * @return 新值, 可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 */
	<T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
			throws TypeMismatchException;

	/**
	 * 将值转换为所需类型 (if necessary from a String).
	 * <p>从String到任何类型的转换通常使用PropertyEditor类的{@code setAsText}方法, 或ConversionService中的Spring转换器.
	 * 
	 * @param value 要转换的值
	 * @param requiredType 所需的类型(or {@code null}, 例如在集合元素的情况下)
	 * @param field 作为转换目标的反射字段 (用于分析泛型类型; may be {@code null})
	 * 
	 * @return 新值, 可能是类型转换的结果
	 * @throws TypeMismatchException 如果类型转换失败
	 */
	<T> T convertIfNecessary(Object value, Class<T> requiredType, Field field)
			throws TypeMismatchException;

}
