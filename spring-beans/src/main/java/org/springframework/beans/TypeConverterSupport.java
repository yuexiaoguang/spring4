package org.springframework.beans;

import java.lang.reflect.Field;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;

/**
 * {@link TypeConverter}接口的基本实现, 使用包级私有委托.
 * 主要作为{@link BeanWrapperImpl}的基类.
 */
public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {

	TypeConverterDelegate typeConverterDelegate;


	@Override
	public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
		return doConvert(value, requiredType, null, null);
	}

	@Override
	public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
			throws TypeMismatchException {

		return doConvert(value, requiredType, methodParam, null);
	}

	@Override
	public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field)
			throws TypeMismatchException {

		return doConvert(value, requiredType, null, field);
	}

	private <T> T doConvert(Object value, Class<T> requiredType, MethodParameter methodParam, Field field)
			throws TypeMismatchException {
		try {
			if (field != null) {
				return this.typeConverterDelegate.convertIfNecessary(value, requiredType, field);
			}
			else {
				return this.typeConverterDelegate.convertIfNecessary(value, requiredType, methodParam);
			}
		}
		catch (ConverterNotFoundException ex) {
			throw new ConversionNotSupportedException(value, requiredType, ex);
		}
		catch (ConversionException ex) {
			throw new TypeMismatchException(value, requiredType, ex);
		}
		catch (IllegalStateException ex) {
			throw new ConversionNotSupportedException(value, requiredType, ex);
		}
		catch (IllegalArgumentException ex) {
			throw new TypeMismatchException(value, requiredType, ex);
		}
	}

}
