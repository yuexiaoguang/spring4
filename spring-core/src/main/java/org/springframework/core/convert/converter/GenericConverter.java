package org.springframework.core.convert.converter;

import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;

/**
 * 用于在两种或更多种类型之间转换的泛型转换器接口.
 *
 * <p>这是Converter SPI接口中最灵活的, 也是最复杂的.
 * 它的灵活性在于GenericConverter可能支持在多个源/目标类型对之间进行转换 (see {@link #getConvertibleTypes()}.
 * 此外, GenericConverter实现在类型转换过程中可以访问源/目标{@link TypeDescriptor field context}.
 * 这允许解析源和目标字段元数据, 例如注解和泛型信息, 这些元数据可用于影响转换逻辑.
 *
 * <p>当更简单的{@link Converter}或{@link ConverterFactory}接口足够时, 通常不应使用此接口.
 *
 * <p>实现可以另外实现{@link ConditionalConverter}.
 */
public interface GenericConverter {

	/**
	 * 返回此转换器可以在其间转换的源和目标类型.
	 * <p>每个条目都是可转换的源到目标类型对.
	 * <p>对于{@link ConditionalConverter 条件转换器}, 此方法可能返回{@code null}, 以指示应考虑所有源到目标对.
	 */
	Set<ConvertiblePair> getConvertibleTypes();

	/**
	 * 将源对象转换为{@code TypeDescriptor}描述的targetType.
	 * 
	 * @param source 要转换的源对象 (may be {@code null})
	 * @param sourceType 要转换的源字段的类型描述符
	 * @param targetType 要转换的目标字段的类型描述符
	 * 
	 * @return 已转换的对象
	 */
	Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);


	/**
	 * 源到目标类对的保存器.
	 */
	final class ConvertiblePair {

		private final Class<?> sourceType;

		private final Class<?> targetType;

		/**
		 * 创建新的源到目标对.
		 * 
		 * @param sourceType 源类型
		 * @param targetType 目标类型
		 */
		public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
			Assert.notNull(sourceType, "Source type must not be null");
			Assert.notNull(targetType, "Target type must not be null");
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		public Class<?> getSourceType() {
			return this.sourceType;
		}

		public Class<?> getTargetType() {
			return this.targetType;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || other.getClass() != ConvertiblePair.class) {
				return false;
			}
			ConvertiblePair otherPair = (ConvertiblePair) other;
			return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
		}

		@Override
		public int hashCode() {
			return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
		}

		@Override
		public String toString() {
			return (this.sourceType.getName() + " -> " + this.targetType.getName());
		}
	}
}
