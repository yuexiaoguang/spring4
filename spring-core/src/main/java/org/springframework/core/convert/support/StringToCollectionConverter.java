package org.springframework.core.convert.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.StringUtils;

/**
 * 将逗号分隔的String转换为Collection.
 * 如果声明了目标集合元素类型, 则只有在{@code String.class}可以转换为它时才匹配.
 */
final class StringToCollectionConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public StringToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (targetType.getElementTypeDescriptor() == null ||
				this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor()));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		String string = (String) source;

		String[] fields = StringUtils.commaDelimitedListToStringArray(string);
		TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), fields.length);

		if (elementDesc == null) {
			for (String field : fields) {
				target.add(field.trim());
			}
		}
		else {
			for (String field : fields) {
				Object targetElement = this.conversionService.convert(field.trim(), sourceType, elementDesc);
				target.add(targetElement);
			}
		}
		return target;
	}

}
