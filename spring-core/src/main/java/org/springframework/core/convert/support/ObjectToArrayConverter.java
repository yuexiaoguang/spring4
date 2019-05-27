package org.springframework.core.convert.support;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * 将Object转换为包含Object的单元素数组.
 * 如有必要, 将Object转换为目标数组的组件类型.
 */
final class ObjectToArrayConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public ObjectToArrayConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Object target = Array.newInstance(targetType.getElementTypeDescriptor().getType(), 1);
		Object targetElement = this.conversionService.convert(source, sourceType, targetType.getElementTypeDescriptor());
		Array.set(target, 0, targetElement);
		return target;
	}

}
