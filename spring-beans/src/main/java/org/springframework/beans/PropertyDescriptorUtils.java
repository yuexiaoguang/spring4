package org.springframework.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;

import org.springframework.util.ObjectUtils;

/**
 * Spring的内部{@link PropertyDescriptor}实现的常见委托方法.
 */
class PropertyDescriptorUtils {

	/**
	 * See {@link java.beans.FeatureDescriptor}.
	 */
	public static void copyNonMethodProperties(PropertyDescriptor source, PropertyDescriptor target)
			throws IntrospectionException {

		target.setExpert(source.isExpert());
		target.setHidden(source.isHidden());
		target.setPreferred(source.isPreferred());
		target.setName(source.getName());
		target.setShortDescription(source.getShortDescription());
		target.setDisplayName(source.getDisplayName());

		// Copy all attributes (emulating behavior of private FeatureDescriptor#addTable)
		Enumeration<String> keys = source.attributeNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			target.setValue(key, source.getValue(key));
		}

		// See java.beans.PropertyDescriptor#PropertyDescriptor(PropertyDescriptor)
		target.setPropertyEditorClass(source.getPropertyEditorClass());
		target.setBound(source.isBound());
		target.setConstrained(source.isConstrained());
	}

	/**
	 * See {@link java.beans.PropertyDescriptor#findPropertyType}.
	 */
	public static Class<?> findPropertyType(Method readMethod, Method writeMethod) throws IntrospectionException {
		Class<?> propertyType = null;

		if (readMethod != null) {
			Class<?>[] params = readMethod.getParameterTypes();
			if (params.length != 0) {
				throw new IntrospectionException("Bad read method arg count: " + readMethod);
			}
			propertyType = readMethod.getReturnType();
			if (propertyType == Void.TYPE) {
				throw new IntrospectionException("Read method returns void: " + readMethod);
			}
		}

		if (writeMethod != null) {
			Class<?>[] params = writeMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException("Bad write method arg count: " + writeMethod);
			}
			if (propertyType != null) {
				if (propertyType.isAssignableFrom(params[0])) {
					// Write方法的属性类型可能更具体
					propertyType = params[0];
				}
				else if (params[0].isAssignableFrom(propertyType)) {
					// 继续读取方法的属性类型
				}
				else {
					throw new IntrospectionException(
							"Type mismatch between read and write methods: " + readMethod + " - " + writeMethod);
				}
			}
			else {
				propertyType = params[0];
			}
		}

		return propertyType;
	}

	/**
	 * See {@link java.beans.IndexedPropertyDescriptor#findIndexedPropertyType}.
	 */
	public static Class<?> findIndexedPropertyType(String name, Class<?> propertyType,
			Method indexedReadMethod, Method indexedWriteMethod) throws IntrospectionException {

		Class<?> indexedPropertyType = null;

		if (indexedReadMethod != null) {
			Class<?>[] params = indexedReadMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException("Bad indexed read method arg count: " + indexedReadMethod);
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException("Non int index to indexed read method: " + indexedReadMethod);
			}
			indexedPropertyType = indexedReadMethod.getReturnType();
			if (indexedPropertyType == Void.TYPE) {
				throw new IntrospectionException("Indexed read method returns void: " + indexedReadMethod);
			}
		}

		if (indexedWriteMethod != null) {
			Class<?>[] params = indexedWriteMethod.getParameterTypes();
			if (params.length != 2) {
				throw new IntrospectionException("Bad indexed write method arg count: " + indexedWriteMethod);
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException("Non int index to indexed write method: " + indexedWriteMethod);
			}
			if (indexedPropertyType != null) {
				if (indexedPropertyType.isAssignableFrom(params[1])) {
					// Write method's property type potentially more specific
					indexedPropertyType = params[1];
				}
				else if (params[1].isAssignableFrom(indexedPropertyType)) {
					// Proceed with read method's property type
				}
				else {
					throw new IntrospectionException("Type mismatch between indexed read and write methods: " +
							indexedReadMethod + " - " + indexedWriteMethod);
				}
			}
			else {
				indexedPropertyType = params[1];
			}
		}

		if (propertyType != null && (!propertyType.isArray() ||
				propertyType.getComponentType() != indexedPropertyType)) {
			throw new IntrospectionException("Type mismatch between indexed and non-indexed methods: " +
					indexedReadMethod + " - " + indexedWriteMethod);
		}

		return indexedPropertyType;
	}

	public static boolean equals(PropertyDescriptor pd, PropertyDescriptor otherPd) {
		return (ObjectUtils.nullSafeEquals(pd.getReadMethod(), otherPd.getReadMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getWriteMethod(), otherPd.getWriteMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getPropertyType(), otherPd.getPropertyType()) &&
				ObjectUtils.nullSafeEquals(pd.getPropertyEditorClass(), otherPd.getPropertyEditorClass()) &&
				pd.isBound() == otherPd.isBound() && pd.isConstrained() == otherPd.isConstrained());
	}
}
