package org.springframework.beans.support;

import java.beans.PropertyEditor;
import java.lang.reflect.Method;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;

/**
 * 试图通过{@link TypeConverter}转换实际目标方法的给定参数的{@link MethodInvoker}的子类.
 *
 * <p>支持灵活的参数转换, 特别是用于调用特定的重载方法.
 */
public class ArgumentConvertingMethodInvoker extends MethodInvoker {

	private TypeConverter typeConverter;

	private boolean useDefaultConverter = true;


	/**
	 * 设置用于参数类型转换的TypeConverter.
	 * <p>默认是 {@link org.springframework.beans.SimpleTypeConverter}.
	 * 可以使用任何TypeConverter实现覆盖, 通常是预配置的SimpleTypeConverter或BeanWrapperImpl实例.
	 */
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
		this.useDefaultConverter = false;
	}

	/**
	 * 返回用于参数类型转换的TypeConverter.
	 * <p>如果需要直接访问底层PropertyEditors, 可以转换为{@link org.springframework.beans.PropertyEditorRegistry}
	 * (假设当前的TypeConverter实际上实现了PropertyEditorRegistry接口).
	 */
	public TypeConverter getTypeConverter() {
		if (this.typeConverter == null && this.useDefaultConverter) {
			this.typeConverter = getDefaultTypeConverter();
		}
		return this.typeConverter;
	}

	/**
	 * 获取此方法调用程序的默认TypeConverter.
	 * <p>如果未指定显式TypeConverter, 则调用.
	 * 默认实现构建一个 {@link org.springframework.beans.SimpleTypeConverter}.
	 * 可以在子类中重写.
	 */
	protected TypeConverter getDefaultTypeConverter() {
		return new SimpleTypeConverter();
	}

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器.
	 * <p>通常与默认值 {@link org.springframework.beans.SimpleTypeConverter}一起使用;
	 * 将适用于所有实现PropertyEditorRegistry接口的TypeConverter.
	 * 
	 * @param requiredType 属性的类型
	 * @param propertyEditor 要注册的编辑器
	 */
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		TypeConverter converter = getTypeConverter();
		if (!(converter instanceof PropertyEditorRegistry)) {
			throw new IllegalStateException(
					"TypeConverter does not implement PropertyEditorRegistry interface: " + converter);
		}
		((PropertyEditorRegistry) converter).registerCustomEditor(requiredType, propertyEditor);
	}


	/**
	 * 此实现查找具有匹配参数类型的方法.
	 */
	@Override
	protected Method findMatchingMethod() {
		Method matchingMethod = super.findMatchingMethod();
		// Second pass: 寻找可以将参数转换为参数类型的方法.
		if (matchingMethod == null) {
			// 将参数数组解释为单独的方法参数.
			matchingMethod = doFindMatchingMethod(getArguments());
		}
		if (matchingMethod == null) {
			// 将参数数组解释为数组类型的单个方法参数.
			matchingMethod = doFindMatchingMethod(new Object[] {getArguments()});
		}
		return matchingMethod;
	}

	/**
	 * 实际查找一个匹配参数类型的方法, i.e. 其中每个参数值可分配给相应的参数类型.
	 * 
	 * @param arguments 与方法参数匹配的参数值
	 * 
	 * @return 匹配的方法, 或{@code null}
	 */
	protected Method doFindMatchingMethod(Object[] arguments) {
		TypeConverter converter = getTypeConverter();
		if (converter != null) {
			String targetMethod = getTargetMethod();
			Method matchingMethod = null;
			int argCount = arguments.length;
			Method[] candidates = ReflectionUtils.getAllDeclaredMethods(getTargetClass());
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Object[] argumentsToUse = null;
			for (Method candidate : candidates) {
				if (candidate.getName().equals(targetMethod)) {
					// 检查方法是否具有正确数量的参数.
					Class<?>[] paramTypes = candidate.getParameterTypes();
					if (paramTypes.length == argCount) {
						Object[] convertedArguments = new Object[argCount];
						boolean match = true;
						for (int j = 0; j < argCount && match; j++) {
							// 验证提供的参数是否可分配给method参数.
							try {
								convertedArguments[j] = converter.convertIfNecessary(arguments[j], paramTypes[j]);
							}
							catch (TypeMismatchException ex) {
								// Ignore -> simply doesn't match.
								match = false;
							}
						}
						if (match) {
							int typeDiffWeight = getTypeDifferenceWeight(paramTypes, convertedArguments);
							if (typeDiffWeight < minTypeDiffWeight) {
								minTypeDiffWeight = typeDiffWeight;
								matchingMethod = candidate;
								argumentsToUse = convertedArguments;
							}
						}
					}
				}
			}
			if (matchingMethod != null) {
				setArguments(argumentsToUse);
				return matchingMethod;
			}
		}
		return null;
	}

}
