package org.springframework.core.convert.support;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * 将Collection转换为数组.
 *
 * <p>首先, 创建一个请求的targetType的新数组, 其长度等于源Collection的大小.
 * 然后将每个集合元素添加到数组.
 * 如有必要, 将执行从集合的参数化类型到数组的组件类型的元素转换.
 */
final class CollectionToArrayConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public CollectionToArrayConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, Object[].class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(),
				targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Collection<?> sourceCollection = (Collection<?>) source;
		Object array = Array.newInstance(targetType.getElementTypeDescriptor().getType(), sourceCollection.size());
		int i = 0;
		for (Object sourceElement : sourceCollection) {
			Object targetElement = this.conversionService.convert( sourceElement,
					sourceType.elementTypeDescriptor(sourceElement), targetType.getElementTypeDescriptor());
			Array.set(array, i++, targetElement);
		}
		return array;
	}

}
