package org.springframework.core.convert.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * 使用约定将源对象转换为{@code targetType}的泛型转换器,
 * 通过委托给源对象上的方法或{@code targetType}上的静态工厂方法或构造函数.
 *
 * <h3>转换算法</h3>
 * <ol>
 * <li>如果存在这样的方法, 则在返回类型等于{@code targetType}的源对象上调用非静态{@code to[targetType.simpleName]()}方法.
 * 例如, {@code org.example.Bar Foo#toBar()}是遵循此约定的方法.
 * <li>否则如果存在这样的方法, 调用{@code targetType}上的<em>static</em> {@code valueOf(sourceType)},
 *  或Java 8风格的<em>static</em> {@code of(sourceType)}或 {@code from(sourceType)}方法.
 * <li>否则调用{@code targetType}上接受单个{@code sourceType}参数的构造函数, 如果存在这样的构造函数.
 * <li>否则抛出{@link ConversionFailedException}.
 * </ol>
 *
 * <p><strong>Warning</strong>: 此转换器<em>不</em>支持{@link Object#toString()}方法,
 * 以便从{@code sourceType}转换为{@code java.lang.String}.
 * 对于{@code toString()}支持, 使用{@link FallbackObjectToStringConverter}.
 */
final class ObjectToObjectConverter implements ConditionalGenericConverter {

	// 缓存, 在给定Class上解析的最新to方法
	private static final Map<Class<?>, Member> conversionMemberCache =
			new ConcurrentReferenceHashMap<Class<?>, Member>(32);


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (sourceType.getType() != targetType.getType() &&
				hasConversionMethodOrConstructor(targetType.getType(), sourceType.getType()));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		Class<?> sourceClass = sourceType.getType();
		Class<?> targetClass = targetType.getType();
		Member member = getValidatedMember(targetClass, sourceClass);

		try {
			if (member instanceof Method) {
				Method method = (Method) member;
				ReflectionUtils.makeAccessible(method);
				if (!Modifier.isStatic(method.getModifiers())) {
					return method.invoke(source);
				}
				else {
					return method.invoke(null, source);
				}
			}
			else if (member instanceof Constructor) {
				Constructor<?> ctor = (Constructor<?>) member;
				ReflectionUtils.makeAccessible(ctor);
				return ctor.newInstance(source);
			}
		}
		catch (InvocationTargetException ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
		}
		catch (Throwable ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}

		// 如果sourceClass为Number, 且targetClass为Integer, 则以下消息应扩展为:
		// java.lang.Number上没有toInteger()方法,
		// 并且java.lang.Integer上不存在静态valueOf/of/from(java.lang.Number)方法或Integer(java.lang.Number)构造函数.
		throw new IllegalStateException(String.format("No to%3$s() method exists on %1$s, " +
				"and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.",
				sourceClass.getName(), targetClass.getName(), targetClass.getSimpleName()));
	}



	static boolean hasConversionMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return (getValidatedMember(targetClass, sourceClass) != null);
	}

	private static Member getValidatedMember(Class<?> targetClass, Class<?> sourceClass) {
		Member member = conversionMemberCache.get(targetClass);
		if (isApplicable(member, sourceClass)) {
			return member;
		}

		member = determineToMethod(targetClass, sourceClass);
		if (member == null) {
			member = determineFactoryMethod(targetClass, sourceClass);
			if (member == null) {
				member = determineFactoryConstructor(targetClass, sourceClass);
				if (member == null) {
					return null;
				}
			}
		}

		conversionMemberCache.put(targetClass, member);
		return member;
	}

	private static boolean isApplicable(Member member, Class<?> sourceClass) {
		if (member instanceof Method) {
			Method method = (Method) member;
			return (!Modifier.isStatic(method.getModifiers()) ?
					ClassUtils.isAssignable(method.getDeclaringClass(), sourceClass) :
					method.getParameterTypes()[0] == sourceClass);
		}
		else if (member instanceof Constructor) {
			Constructor<?> ctor = (Constructor<?>) member;
			return (ctor.getParameterTypes()[0] == sourceClass);
		}
		else {
			return false;
		}
	}

	private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
		if (String.class == targetClass || String.class == sourceClass) {
			// 不要在String本身上接受 toString()方法或任何方法
			return null;
		}

		Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());
		return (method != null && !Modifier.isStatic(method.getModifiers()) &&
				ClassUtils.isAssignable(targetClass, method.getReturnType()) ? method : null);
	}

	private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
		if (String.class == targetClass) {
			// 不接受 String.valueOf(Object)方法
			return null;
		}

		Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
		if (method == null) {
			method = ClassUtils.getStaticMethod(targetClass, "of", sourceClass);
			if (method == null) {
				method = ClassUtils.getStaticMethod(targetClass, "from", sourceClass);
			}
		}
		return method;
	}

	private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return ClassUtils.getConstructorIfAvailable(targetClass, sourceClass);
	}

}
