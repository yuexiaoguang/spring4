package org.springframework.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Helper类, 用于确定集合和映射的元素类型.
 *
 * <p>主要用于框架内的使用, 确定要添加到集合或映射的值的目标类型 (以便在适当时尝试进行类型转换).
 *
 * @deprecated as of 4.3.6, in favor of direct {@link ResolvableType} usage
 */
@Deprecated
public abstract class GenericCollectionTypeResolver {

	/**
	 * 确定给定Collection类的泛型元素类型 (如果它通过泛型超类或泛型接口声明).
	 * 
	 * @param collectionClass 要内省的集合类
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> getCollectionType(Class<? extends Collection> collectionClass) {
		return ResolvableType.forClass(collectionClass).asCollection().resolveGeneric();
	}

	/**
	 * 确定给定Map类的泛型Key类型(如果它通过泛型超类或泛型接口声明).
	 * 
	 * @param mapClass 要内省的Map类
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> getMapKeyType(Class<? extends Map> mapClass) {
		return ResolvableType.forClass(mapClass).asMap().resolveGeneric(0);
	}

	/**
	 * 确定给定Map类的泛型值类型 (如果它通过泛型超类或泛型接口声明).
	 * 
	 * @param mapClass 要内省的Map类
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> getMapValueType(Class<? extends Map> mapClass) {
		return ResolvableType.forClass(mapClass).asMap().resolveGeneric(1);
	}

	/**
	 * 确定给定Collection字段的泛型元素类型.
	 * 
	 * @param collectionField 要内省的集合字段
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getCollectionFieldType(Field collectionField) {
		return ResolvableType.forField(collectionField).asCollection().resolveGeneric();
	}

	/**
	 * 确定给定Collection字段的泛型元素类型.
	 * 
	 * @param collectionField 要内省的集合字段
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getCollectionFieldType(Field collectionField, int nestingLevel) {
		return ResolvableType.forField(collectionField).getNested(nestingLevel).asCollection().resolveGeneric();
	}

	/**
	 * 确定给定Collection字段的泛型元素类型.
	 * 
	 * @param collectionField 要内省的集合字段
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * @param typeIndexesPerLevel 由嵌套级别作为键的Map, 每个值表示该级别的遍历类型索引
	 * 
	 * @return 泛型类型, 或{@code null}
	 * @deprecated as of 4.0, in favor of using {@link ResolvableType} for arbitrary nesting levels
	 */
	@Deprecated
	public static Class<?> getCollectionFieldType(Field collectionField, int nestingLevel, Map<Integer, Integer> typeIndexesPerLevel) {
		return ResolvableType.forField(collectionField).getNested(nestingLevel, typeIndexesPerLevel).asCollection().resolveGeneric();
	}

	/**
	 * 确定给定Map字段的泛型键类型.
	 * 
	 * @param mapField 要内省的Map字段
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapKeyFieldType(Field mapField) {
		return ResolvableType.forField(mapField).asMap().resolveGeneric(0);
	}

	/**
	 * 确定给定Map字段的泛型键类型.
	 * 
	 * @param mapField 要内省的Map字段
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapKeyFieldType(Field mapField, int nestingLevel) {
		return ResolvableType.forField(mapField).getNested(nestingLevel).asMap().resolveGeneric(0);
	}

	/**
	 * 确定给定Map字段的泛型键类型.
	 * 
	 * @param mapField 要内省的Map字段
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * @param typeIndexesPerLevel 由嵌套级别作为键的Map, 每个值表示该级别的遍历类型索引
	 * 
	 * @return 泛型类型, 或{@code null}
	 * @deprecated as of 4.0, in favor of using {@link ResolvableType} for arbitrary nesting levels
	 */
	@Deprecated
	public static Class<?> getMapKeyFieldType(Field mapField, int nestingLevel, Map<Integer, Integer> typeIndexesPerLevel) {
		return ResolvableType.forField(mapField).getNested(nestingLevel, typeIndexesPerLevel).asMap().resolveGeneric(0);
	}

	/**
	 * 确定给定Map字段的泛型值类型.
	 * 
	 * @param mapField 要内省的Map字段
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapValueFieldType(Field mapField) {
		return ResolvableType.forField(mapField).asMap().resolveGeneric(1);
	}

	/**
	 * 确定给定Map字段的泛型值类型.
	 * 
	 * @param mapField 要内省的Map字段
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapValueFieldType(Field mapField, int nestingLevel) {
		return ResolvableType.forField(mapField).getNested(nestingLevel).asMap().resolveGeneric(1);
	}

	/**
	 * 确定给定Map字段的泛型值类型.
	 * 
	 * @param mapField 要内省的Map字段
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * @param typeIndexesPerLevel 由嵌套级别作为键的Map, 每个值表示该级别的遍历类型索引
	 * 
	 * @return 泛型类型, 或{@code null}
	 * @deprecated as of 4.0, in favor of using {@link ResolvableType} for arbitrary nesting levels
	 */
	@Deprecated
	public static Class<?> getMapValueFieldType(Field mapField, int nestingLevel, Map<Integer, Integer> typeIndexesPerLevel) {
		return ResolvableType.forField(mapField).getNested(nestingLevel, typeIndexesPerLevel).asMap().resolveGeneric(1);
	}

	/**
	 * 确定给定Collection参数的泛型元素类型.
	 * 
	 * @param methodParam 方法参数规范
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getCollectionParameterType(MethodParameter methodParam) {
		return ResolvableType.forMethodParameter(methodParam).asCollection().resolveGeneric();
	}

	/**
	 * 确定给定Map参数的泛型键类型.
	 * 
	 * @param methodParam 方法参数规范
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapKeyParameterType(MethodParameter methodParam) {
		return ResolvableType.forMethodParameter(methodParam).asMap().resolveGeneric(0);
	}

	/**
	 * 确定给定Map参数的泛型值类型.
	 * 
	 * @param methodParam 方法参数规范
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapValueParameterType(MethodParameter methodParam) {
		return ResolvableType.forMethodParameter(methodParam).asMap().resolveGeneric(1);
	}

	/**
	 * 确定给定Collection返回类型的泛型元素类型.
	 * 
	 * @param method 要检查返回类型的方法
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getCollectionReturnType(Method method) {
		return ResolvableType.forMethodReturnType(method).asCollection().resolveGeneric();
	}

	/**
	 * 确定给定Collection返回类型的泛型元素类型.
	 * <p>如果指定的嵌套级别高于1, 则将分析嵌套的Collection/Map的元素类型.
	 * 
	 * @param method 要检查返回类型的方法
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getCollectionReturnType(Method method, int nestingLevel) {
		return ResolvableType.forMethodReturnType(method).getNested(nestingLevel).asCollection().resolveGeneric();
	}

	/**
	 * 确定给定Map返回类型的泛型键类型.
	 * 
	 * @param method 要检查返回类型的方法
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapKeyReturnType(Method method) {
		return ResolvableType.forMethodReturnType(method).asMap().resolveGeneric(0);
	}

	/**
	 * 确定给定Map返回类型的泛型键类型.
	 * 
	 * @param method 要检查返回类型的方法
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapKeyReturnType(Method method, int nestingLevel) {
		return ResolvableType.forMethodReturnType(method).getNested(nestingLevel).asMap().resolveGeneric(0);
	}

	/**
	 * 确定给定Map返回类型的泛型值类型.
	 * 
	 * @param method 要检查返回类型的方法
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapValueReturnType(Method method) {
		return ResolvableType.forMethodReturnType(method).asMap().resolveGeneric(1);
	}

	/**
	 * 确定给定Map返回类型的泛型值类型.
	 * 
	 * @param method 要检查返回类型的方法
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套的List的元素)
	 * 
	 * @return 泛型类型, 或{@code null}
	 */
	public static Class<?> getMapValueReturnType(Method method, int nestingLevel) {
		return ResolvableType.forMethodReturnType(method).getNested(nestingLevel).asMap().resolveGeneric(1);
	}
}
