package org.springframework.core.type;

import java.util.Map;

import org.springframework.util.MultiValueMap;

/**
 * 定义对特定类型 ({@link AnnotationMetadata class} 或 {@link MethodMetadata method})的注解的访问,
 * 不一定需要类加载.
 */
public interface AnnotatedTypeMetadata {

	/**
	 * 确定底层元素是否具有定义的给定类型的注解或元注解.
	 * <p>如果此方法返回{@code true}, 则{@link #getAnnotationAttributes}将返回非null Map.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 是否定义了匹配的注解
	 */
	boolean isAnnotated(String annotationName);

	/**
	 * 检索给定类型的注解的属性 (i.e. 如果在底层元素上定义, 作为直接注解或元注解), 还要考虑组合注解的属性覆盖.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 属性的Map, 属性名称作为key (e.g. "value"), 定义的属性值作为Map值.
	 * 如果未定义匹配的注解, 则此返回值将为{@code null}.
	 */
	Map<String, Object> getAnnotationAttributes(String annotationName);

	/**
	 * 检索给定类型的注解的属性 (i.e. 如果在底层元素上定义, 作为直接注解或元注解), 还要考虑组合注解的属性覆盖.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将类引用转换为String类名, 以便在返回的Map中将值作为值转换, 而不是可能必须首先加载的Class引用
	 * 
	 * @return 属性的Map, 属性名称作为key (e.g. "value"), 定义的属性值作为Map值.
	 * 如果未定义匹配的注解, 则此返回值将为{@code null}.
	 */
	Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString);

	/**
	 * 检索给定类型的所有注解的所有属性 (i.e. 如果在底层元素上定义, 作为直接注解或元注解).
	 * 请注意, 此变体<i>不</i>考虑属性覆盖.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 属性的MultiMap, 属性名称作为key (e.g. "value"), 定义的属性值列表作为Map值.
	 * 如果未定义匹配的注解, 则此返回值将为{@code null}.
	 */
	MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName);

	/**
	 * 检索给定类型的所有注解的所有属性 (i.e. 如果在底层元素上定义, 作为直接注解或元注解).
	 * 请注意, 此变体<i>不</i>考虑属性覆盖.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @param classValuesAsString  是否将类引用转换为String类名
	 * 
	 * @return 属性的MultiMap, 属性名称作为key (e.g. "value"), 定义的属性值列表作为Map值.
	 * 如果未定义匹配的注解, 则此返回值将为{@code null}.
	 */
	MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString);

}
