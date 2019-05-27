package org.springframework.beans;

import org.springframework.core.convert.ConversionService;

/**
 * 封装PropertyAccessor的配置方法的接口.
 * 也扩展了PropertyEditorRegistry接口, 它定义了PropertyEditor管理的方法.
 *
 * <p>作为{@link BeanWrapper}的基础接口.
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

	/**
	 * 指定用于转换属性值的Spring 3.0 ConversionService, 作为JavaBeans PropertyEditors的替代品.
	 */
	void setConversionService(ConversionService conversionService);

	/**
	 * 返回关联的ConversionService.
	 */
	ConversionService getConversionService();

	/**
	 * 设置在将属性编辑器应用于属性的新值时是否提取旧属性值.
	 */
	void setExtractOldValueForEditor(boolean extractOldValueForEditor);

	/**
	 * 返回在将属性编辑器应用于属性的新值时是否提取旧属性值.
	 */
	boolean isExtractOldValueForEditor();

	/**
	 * 设置此实例是否应尝试“自动增长”包含{@code null}值的嵌套路径.
	 * <p>如果为{@code true}, {@code null}路径位置将填充默认对象值并遍历, 而不是导致{@link NullValueInNestedPathException}.
	 * <p>在普通的PropertyAccessor实例上, 默认值为{@code false}.
	 */
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * 返回是否已激活嵌套路径的“自动增长”.
	 */
	boolean isAutoGrowNestedPaths();

}
