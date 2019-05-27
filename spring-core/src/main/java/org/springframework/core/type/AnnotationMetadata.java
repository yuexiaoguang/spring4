package org.springframework.core.type;

import java.util.Set;

/**
 * 定义对特定类的注解的抽象访问的接口, 不需要加载该类.
 */
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

	/**
	 * 获取底层类上<em>存在</em>的所有注解类型的完全限定类名.
	 * 
	 * @return 注解类型名称
	 */
	Set<String> getAnnotationTypes();

	/**
	 * 获取基础类上给定注解类型上<em>存在</em>的所有元注解类型的完全限定类名.
	 * 
	 * @param annotationName 要查找的元注解类型的完全限定类名
	 * 
	 * @return 元注解类型名称
	 */
	Set<String> getMetaAnnotationTypes(String annotationName);

	/**
	 * 确定给定类型的注解是否在底层类上<em>存在</em>.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return {@code true} 如果存在匹配的注解
	 */
	boolean hasAnnotation(String annotationName);

	/**
	 * 确定底层类是否具有注解, 该注解本身使用给定类型的元注解进行注解.
	 * 
	 * @param metaAnnotationName 要查找的元注解类型的完全限定类名
	 * 
	 * @return {@code true} 如果存在匹配的元注解
	 */
	boolean hasMetaAnnotation(String metaAnnotationName);

	/**
	 * 确定底层类是否具有使用给定注解类型进行注解(或元注解)的方法.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 */
	boolean hasAnnotatedMethods(String annotationName);

	/**
	 * 检索使用给定注解类型注解(或元注解)的所有方法的方法元数据.
	 * <p>对于任何返回的方法, {@link MethodMetadata#isAnnotated}将为给定的注解类型返回{@code true}.
	 * 
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * 
	 * @return 拥有匹配的注解的方法的{@link MethodMetadata}.
	 * 如果没有方法与注解类型匹配, 则返回值将为空集.
	 */
	Set<MethodMetadata> getAnnotatedMethods(String annotationName);

}
