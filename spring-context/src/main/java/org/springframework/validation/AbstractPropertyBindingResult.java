package org.springframework.validation;

import java.beans.PropertyEditor;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConvertingPropertyEditorAdapter;
import org.springframework.util.Assert;

/**
 * {@link BindingResult}实现的抽象基类, 使用Spring的{@link org.springframework.beans.PropertyAccessor}机制.
 * 通过委托到相应的PropertyAccessor方法预先实现字段访问.
 */
@SuppressWarnings("serial")
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {

	private transient ConversionService conversionService;


	/**
	 * @param objectName 目标对象的名称
	 */
	protected AbstractPropertyBindingResult(String objectName) {
		super(objectName);
	}


	public void initConversion(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
		if (getTarget() != null) {
			getPropertyAccessor().setConversionService(conversionService);
		}
	}

	/**
	 * 返回底层的PropertyAccessor.
	 */
	@Override
	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return getPropertyAccessor();
	}

	/**
	 * 返回规范的属性名称.
	 */
	@Override
	protected String canonicalFieldName(String field) {
		return PropertyAccessorUtils.canonicalPropertyName(field);
	}

	/**
	 * 从属性类型确定字段类型.
	 */
	@Override
	public Class<?> getFieldType(String field) {
		return getPropertyAccessor().getPropertyType(fixedField(field));
	}

	/**
	 * 从PropertyAccessor获取字段值.
	 */
	@Override
	protected Object getActualFieldValue(String field) {
		return getPropertyAccessor().getPropertyValue(field);
	}

	/**
	 * 根据已注册的PropertyEditors格式化字段值.
	 */
	@Override
	protected Object formatFieldValue(String field, Object value) {
		String fixedField = fixedField(field);
		// Try custom editor...
		PropertyEditor customEditor = getCustomEditor(fixedField);
		if (customEditor != null) {
			customEditor.setValue(value);
			String textValue = customEditor.getAsText();
			// 如果PropertyEditor返回null, 则此值没有适当的文本表示形式: only use it if non-null.
			if (textValue != null) {
				return textValue;
			}
		}
		if (this.conversionService != null) {
			// Try custom converter...
			TypeDescriptor fieldDesc = getPropertyAccessor().getPropertyTypeDescriptor(fixedField);
			TypeDescriptor strDesc = TypeDescriptor.valueOf(String.class);
			if (fieldDesc != null && this.conversionService.canConvert(fieldDesc, strDesc)) {
				return this.conversionService.convert(value, fieldDesc, strDesc);
			}
		}
		return value;
	}

	/**
	 * 检索给定字段的自定义PropertyEditor.
	 * 
	 * @param fixedField 完全限定的字段名称
	 * 
	 * @return 自定义PropertyEditor, 或{@code null}
	 */
	protected PropertyEditor getCustomEditor(String fixedField) {
		Class<?> targetType = getPropertyAccessor().getPropertyType(fixedField);
		PropertyEditor editor = getPropertyAccessor().findCustomEditor(targetType, fixedField);
		if (editor == null) {
			editor = BeanUtils.findEditorByConvention(targetType);
		}
		return editor;
	}

	/**
	 * 如果适用, 此实现为Formatter公开PropertyEditor适配器.
	 */
	@Override
	public PropertyEditor findEditor(String field, Class<?> valueType) {
		Class<?> valueTypeForLookup = valueType;
		if (valueTypeForLookup == null) {
			valueTypeForLookup = getFieldType(field);
		}
		PropertyEditor editor = super.findEditor(field, valueTypeForLookup);
		if (editor == null && this.conversionService != null) {
			TypeDescriptor td = null;
			if (field != null) {
				TypeDescriptor ptd = getPropertyAccessor().getPropertyTypeDescriptor(fixedField(field));
				if (valueType == null || valueType.isAssignableFrom(ptd.getType())) {
					td = ptd;
				}
			}
			if (td == null) {
				td = TypeDescriptor.valueOf(valueTypeForLookup);
			}
			if (this.conversionService.canConvert(TypeDescriptor.valueOf(String.class), td)) {
				editor = new ConvertingPropertyEditorAdapter(this.conversionService, td);
			}
		}
		return editor;
	}


	/**
	 * 根据具体的访问策略, 提供PropertyAccessor.
	 * <p>请注意, 默认情况下, BindingResult使用的PropertyAccessor应始终将其"extractOldValueForEditor"标志设置为"true",
	 * 因为这通常可能对作为数据绑定目标的模型对象没有副作用.
	 */
	public abstract ConfigurablePropertyAccessor getPropertyAccessor();

}
