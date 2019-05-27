package org.springframework.core.convert.support;

import java.beans.PropertyEditorSupport;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

/**
 * 适用于任何给定{@link org.springframework.core.convert.ConversionService}
 * 和特定目标类型的{@link java.beans.PropertyEditor}的适配器.
 */
public class ConvertingPropertyEditorAdapter extends PropertyEditorSupport {

	private final ConversionService conversionService;

	private final TypeDescriptor targetDescriptor;

	private final boolean canConvertToString;


	/**
	 * @param conversionService 要委托给的ConversionService
	 * @param targetDescriptor 要转换为的目标类型
	 */
	public ConvertingPropertyEditorAdapter(ConversionService conversionService, TypeDescriptor targetDescriptor) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		Assert.notNull(targetDescriptor, "TypeDescriptor must not be null");
		this.conversionService = conversionService;
		this.targetDescriptor = targetDescriptor;
		this.canConvertToString = conversionService.canConvert(this.targetDescriptor, TypeDescriptor.valueOf(String.class));
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(this.conversionService.convert(text, TypeDescriptor.valueOf(String.class), this.targetDescriptor));
	}

	@Override
	public String getAsText() {
		if (this.canConvertToString) {
			return (String) this.conversionService.convert(getValue(), this.targetDescriptor, TypeDescriptor.valueOf(String.class));
		}
		else {
			return null;
		}
	}

}
