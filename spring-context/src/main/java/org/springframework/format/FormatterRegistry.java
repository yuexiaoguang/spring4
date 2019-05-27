package org.springframework.format;

import java.lang.annotation.Annotation;

import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * 字段格式化逻辑的注册表.
 */
public interface FormatterRegistry extends ConverterRegistry {

	/**
	 * 添加Formatter以格式化特定类型的字段.
	 * 参数化Formatter实例隐含字段类型.
	 * 
	 * @param formatter 要添加的格式化器
	 */
	void addFormatter(Formatter<?> formatter);

	/**
	 * 添加Formatter以格式化给定类型的字段.
	 * <p>在打印时, 如果声明了 Formatter的类型T, 并且 {@code fieldType} 不能分配给T,
	 * 则在委托给 {@code formatter}以打印字段值之前将尝试强制转换到T.
	 * 在解析时, 如果 {@code formatter} 返回的已解析对象不能分配给运行时字段类型,
	 * 则在返回解析的字段值之前将尝试强制转换字段类型.
	 * 
	 * @param fieldType 要格式化的字段类型
	 * @param formatter 要添加的格式化器
	 */
	void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter);

	/**
	 * 添加 Printer/Parser 对以格式化特定类型的字段.
	 * 格式化器将委托给指定的{@code printer}进行打印, 并委托给指定的 {@code parser}进行解析.
	 * <p>在打印时, 如果声明了Printer的类型T, 并且{@code fieldType}不能分配给T, 则在委托给{@code printer}打印字段值之前将尝试强制转换为T.
	 * 在解析时, 如果Parser 返回的对象不能分配给运行时字段类型, 则在返回解析的字段值之前将尝试强制转换为字段类型.
	 * 
	 * @param fieldType 要格式化的字段类型
	 * @param printer 格式化器的打印部分
	 * @param parser 格式化器的解析部分
	 */
	void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser);

	/**
	 * 添加Formatter以格式化使用特定格式注解的字段.
	 * 
	 * @param annotationFormatterFactory 要添加的注解格式化工厂
	 */
	void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory);

}
