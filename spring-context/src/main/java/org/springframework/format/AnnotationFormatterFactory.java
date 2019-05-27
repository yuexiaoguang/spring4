package org.springframework.format;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 一个工厂, 创建格式化器以格式化使用特定{@link Annotation}注解的字段的值.
 *
 * <p>例如, {@code DateTimeFormatAnnotationFormatterFactory} 可能会创建一个格式化器,
 * 格式化{@code Date}值, 并在使用{@code @DateTimeFormat}注解的字段上设置.
 *
 * @param <A> 应该触发格式化的注解类型
 */
public interface AnnotationFormatterFactory<A extends Annotation> {

	/**
	 * 可以使用 &lt;A&gt; 注解的字段类型.
	 */
	Set<Class<?>> getFieldTypes();

	/**
	 * 获取Printer, 打印使用{@code annotation}注解的 {@code fieldType}字段的值.
	 * <p>如果Printer接受的类型T不能分配给 {@code fieldType}, 则在调用Printer之前将尝试从 {@code fieldType} 到T的强制执行.
	 * 
	 * @param annotation 注解实例
	 * @param fieldType 带注解的字段类型
	 * 
	 * @return the printer
	 */
	Printer<?> getPrinter(A annotation, Class<?> fieldType);

	/**
	 * 获取Parser, 解析使用{@code annotation}注解的 {@code fieldType}字段提交的值.
	 * <p>如果解析器返回的对象不能分配给 {@code fieldType}, 则在设置字段之前将尝试强制转换为 {@code fieldType}.
	 * 
	 * @param annotation 注解实例
	 * @param fieldType 带注解的字段类型
	 * 
	 * @return the parser
	 */
	Parser<?> getParser(A annotation, Class<?> fieldType);

}
