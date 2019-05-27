package org.springframework.beans;

/**
 * 不在特定目标对象上运行的{@link TypeConverter}接口的简单实现.
 * 这是使用完整的BeanWrapperImpl实例来满足任意类型转换需求的替代方法,
 * 同时使用相同的转换算法 (包括委托给{@link java.beans.PropertyEditor}和{@link org.springframework.core.convert.ConversionService}).
 *
 * <p><b>Note:</b>由于它依赖{@link java.beans.PropertyEditor PropertyEditors}, SimpleTypeConverter不是线程安全的.
 * 为每个线程使用单独的实例.
 */
public class SimpleTypeConverter extends TypeConverterSupport {

	public SimpleTypeConverter() {
		this.typeConverterDelegate = new TypeConverterDelegate(this);
		registerDefaultEditors();
	}

}
