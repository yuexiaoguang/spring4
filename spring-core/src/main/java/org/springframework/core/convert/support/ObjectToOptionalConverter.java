package org.springframework.core.convert.support;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.UsesJava8;

/**
 * 如果需要, 使用{@code ConversionService}将Object转换为{@code java.util.Optional<T>},
 * 以便在已知时将源Object转换为泛型类型Optional.
 */
@UsesJava8
final class ObjectToOptionalConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public ObjectToOptionalConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		Set<ConvertiblePair> convertibleTypes = new LinkedHashSet<ConvertiblePair>(4);
		convertibleTypes.add(new ConvertiblePair(Collection.class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object[].class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object.class, Optional.class));
		return convertibleTypes;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.getResolvableType() != null) {
			return this.conversionService.canConvert(sourceType, new GenericTypeDescriptor(targetType));
		}
		else {
			return true;
		}
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return Optional.empty();
		}
		else if (source instanceof Optional) {
			return source;
		}
		else if (targetType.getResolvableType() != null) {
			Object target = this.conversionService.convert(source, sourceType, new GenericTypeDescriptor(targetType));
			if (target == null || (target.getClass().isArray() && Array.getLength(target) == 0) ||
						(target instanceof Collection && ((Collection) target).isEmpty())) {
				return Optional.empty();
			}
			return Optional.of(target);
		}
		else {
			return Optional.of(source);
		}
	}


	@SuppressWarnings("serial")
	private static class GenericTypeDescriptor extends TypeDescriptor {

		public GenericTypeDescriptor(TypeDescriptor typeDescriptor) {
			super(typeDescriptor.getResolvableType().getGeneric(), null, typeDescriptor.getAnnotations());
		}
	}

}
