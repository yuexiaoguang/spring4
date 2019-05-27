package org.springframework.core.convert.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.UsesJava8;

/**
 * 将{@link Stream}转换为集合或数组, 并在必要时转换元素类型.
 */
@UsesJava8
class StreamConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STREAM_TYPE = TypeDescriptor.valueOf(Stream.class);

	private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = createConvertibleTypes();

	private final ConversionService conversionService;


	public StreamConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return CONVERTIBLE_TYPES;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return matchesFromStream(sourceType.getElementTypeDescriptor(), targetType);
		}
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return matchesToStream(targetType.getElementTypeDescriptor(), sourceType);
		}
		return false;
	}

	/**
	 * 验证流中保存的元素的{@link Collection}是否可以转换为指定的{@code targetType}.
	 * 
	 * @param elementType 流元素的类型
	 * @param targetType 要转换为的类型
	 */
	public boolean matchesFromStream(TypeDescriptor elementType, TypeDescriptor targetType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(collectionOfElement, targetType);
	}

	/**
	 * 验证指定的{@code sourceType}是否可以转换为流元素类型的{@link Collection}.
	 * 
	 * @param elementType 流元素的类型
	 * @param sourceType 要转换的类型
	 */
	public boolean matchesToStream(TypeDescriptor elementType, TypeDescriptor sourceType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(sourceType, collectionOfElement);
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return convertFromStream((Stream<?>) source, sourceType, targetType);
		}
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return convertToStream(source, sourceType, targetType);
		}
		// Should not happen
		throw new IllegalStateException("Unexpected source/target types");
	}

	private Object convertFromStream(Stream<?> source, TypeDescriptor streamType, TypeDescriptor targetType) {
		List<Object> content = source.collect(Collectors.<Object>toList());
		TypeDescriptor listType = TypeDescriptor.collection(List.class, streamType.getElementTypeDescriptor());
		return this.conversionService.convert(content, listType, targetType);
	}

	private Object convertToStream(Object source, TypeDescriptor sourceType, TypeDescriptor streamType) {
		TypeDescriptor targetCollection = TypeDescriptor.collection(List.class, streamType.getElementTypeDescriptor());
		List<?> target = (List<?>) this.conversionService.convert(source, sourceType, targetCollection);
		return target.stream();
	}


	private static Set<ConvertiblePair> createConvertibleTypes() {
		Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
		convertiblePairs.add(new ConvertiblePair(Stream.class, Collection.class));
		convertiblePairs.add(new ConvertiblePair(Stream.class, Object[].class));
		convertiblePairs.add(new ConvertiblePair(Collection.class, Stream.class));
		convertiblePairs.add(new ConvertiblePair(Object[].class, Stream.class));
		return convertiblePairs;
	}

}
