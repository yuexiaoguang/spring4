package org.springframework.expression.spel.support;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;

/**
 * {@link TypeConverter}接口的默认实现, 委托给核心Spring {@link ConversionService}.
 */
public class StandardTypeConverter implements TypeConverter {

	private final ConversionService conversionService;


	/**
	 * 使用默认的ConversionService.
	 */
	public StandardTypeConverter() {
		this.conversionService = DefaultConversionService.getSharedInstance();
	}

	/**
	 * @param conversionService 要委托给的ConversionService
	 */
	public StandardTypeConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}


	@Override
	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.canConvert(sourceType, targetType);
	}

	@Override
	public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) {
		try {
			return this.conversionService.convert(value, sourceType, targetType);
		}
		catch (ConversionException ex) {
			throw new SpelEvaluationException(ex, SpelMessage.TYPE_CONVERSION_ERROR,
					(sourceType != null ? sourceType.toString() : (value != null ? value.getClass().getName() : "null")),
					targetType.toString());
		}
	}

}
