package org.springframework.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * 用于获取{@link java.lang.reflect.Type}的包装的{@link Serializable}变体.
 *
 * <p>{@link #forField(Field) Fields}或{@link #forMethodParameter(MethodParameter) MethodParameters}可用作可序列化类型的根源.
 * 或者, {@link #forGenericSuperclass(Class) 超类}, {@link #forGenericInterfaces(Class) 接口}
 * 或{@link #forTypeParameters(Class) 类型参数} 或常规{@link Class}也可用作源.
 *
 * <p>返回的类型将是{@link Class}或
 * {@link GenericArrayType}, {@link ParameterizedType}, {@link TypeVariable}, {@link WildcardType}的可序列化代理.
 * 除{@link Class} (其是 final 的)之外, 对返回{@link Type}的方法的调用(例如{@link GenericArrayType#getGenericComponentType()})将自动包装.
 */
abstract class SerializableTypeWrapper {

	private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {
			GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class};

	static final ConcurrentReferenceHashMap<Type, Type> cache = new ConcurrentReferenceHashMap<Type, Type>(256);


	/**
	 * 返回{@link Field#getGenericType()}的{@link Serializable}变体.
	 */
	public static Type forField(Field field) {
		return forTypeProvider(new FieldTypeProvider(field));
	}

	/**
	 * 返回{@link MethodParameter#getGenericParameterType()}的{@link Serializable}变体.
	 */
	public static Type forMethodParameter(MethodParameter methodParameter) {
		return forTypeProvider(new MethodParameterTypeProvider(methodParameter));
	}

	/**
	 * 返回{@link Class#getGenericSuperclass()}的{@link Serializable}变体.
	 */
	@SuppressWarnings("serial")
	public static Type forGenericSuperclass(final Class<?> type) {
		return forTypeProvider(new SimpleTypeProvider() {
			@Override
			public Type getType() {
				return type.getGenericSuperclass();
			}
		});
	}

	/**
	 * 返回{@link Class#getGenericInterfaces()}的{@link Serializable}变体.
	 */
	@SuppressWarnings("serial")
	public static Type[] forGenericInterfaces(final Class<?> type) {
		Type[] result = new Type[type.getGenericInterfaces().length];
		for (int i = 0; i < result.length; i++) {
			final int index = i;
			result[i] = forTypeProvider(new SimpleTypeProvider() {
				@Override
				public Type getType() {
					return type.getGenericInterfaces()[index];
				}
			});
		}
		return result;
	}

	/**
	 * 返回{@link Class#getTypeParameters()}的{@link Serializable}变体.
	 */
	@SuppressWarnings("serial")
	public static Type[] forTypeParameters(final Class<?> type) {
		Type[] result = new Type[type.getTypeParameters().length];
		for (int i = 0; i < result.length; i++) {
			final int index = i;
			result[i] = forTypeProvider(new SimpleTypeProvider() {
				@Override
				public Type getType() {
					return type.getTypeParameters()[index];
				}
			});
		}
		return result;
	}

	/**
	 * 展开给定类型, 有效地返回原始的非可序列化类型.
	 * 
	 * @param type 要解包装的类型
	 * 
	 * @return 原始的非可序列化类型
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Type> T unwrap(T type) {
		Type unwrapped = type;
		while (unwrapped instanceof SerializableTypeProxy) {
			unwrapped = ((SerializableTypeProxy) type).getTypeProvider().getType();
		}
		return (T) unwrapped;
	}

	/**
	 * 返回{@link TypeProvider}支持的{@link Serializable} {@link Type}.
	 */
	static Type forTypeProvider(TypeProvider provider) {
		Type providedType = provider.getType();
		if (providedType == null || providedType instanceof Serializable) {
			// 无需序列化的类型包装 (e.g. for java.lang.Class)
			return providedType;
		}

		// 获取给定提供程序的可序列化类型代理...
		Type cached = cache.get(providedType);
		if (cached != null) {
			return cached;
		}
		for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
			if (type.isInstance(providedType)) {
				ClassLoader classLoader = provider.getClass().getClassLoader();
				Class<?>[] interfaces = new Class<?>[] {type, SerializableTypeProxy.class, Serializable.class};
				InvocationHandler handler = new TypeProxyInvocationHandler(provider);
				cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
				cache.put(providedType, cached);
				return cached;
			}
		}
		throw new IllegalArgumentException("Unsupported Type class: " + providedType.getClass().getName());
	}


	/**
	 * 由类型代理实现的附加接口.
	 */
	interface SerializableTypeProxy {

		/**
		 * 返回底层类型提供器.
		 */
		TypeProvider getTypeProvider();
	}


	/**
	 * 可以访问{@link Type}的{@link Serializable}接口.
	 */
	interface TypeProvider extends Serializable {

		/**
		 * 返回(可能非{@link Serializable}) {@link Type}.
		 */
		Type getType();

		/**
		 * 返回类型的源或{@code null}.
		 */
		Object getSource();
	}


	/**
	 * 使用{@code null}源的{@link TypeProvider}的基实现.
	 */
	@SuppressWarnings("serial")
	private static abstract class SimpleTypeProvider implements TypeProvider {

		@Override
		public Object getSource() {
			return null;
		}
	}


	/**
	 * 代理的{@link Type}使用的{@link Serializable} {@link InvocationHandler}.
	 * 提供序列化支持并增强返回{@code Type}或{@code Type[]}的任何方法.
	 */
	@SuppressWarnings("serial")
	private static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {

		private final TypeProvider provider;

		public TypeProxyInvocationHandler(TypeProvider provider) {
			this.provider = provider;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("equals")) {
				Object other = args[0];
				// Unwrap proxies for speed
				if (other instanceof Type) {
					other = unwrap((Type) other);
				}
				return this.provider.getType().equals(other);
			}
			else if (method.getName().equals("hashCode")) {
				return this.provider.getType().hashCode();
			}
			else if (method.getName().equals("getTypeProvider")) {
				return this.provider;
			}

			if (Type.class == method.getReturnType() && args == null) {
				return forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, -1));
			}
			else if (Type[].class == method.getReturnType() && args == null) {
				Type[] result = new Type[((Type[]) method.invoke(this.provider.getType(), args)).length];
				for (int i = 0; i < result.length; i++) {
					result[i] = forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, i));
				}
				return result;
			}

			try {
				return method.invoke(this.provider.getType(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * 从{@link Field}获取的{@link Type}的{@link TypeProvider}.
	 */
	@SuppressWarnings("serial")
	static class FieldTypeProvider implements TypeProvider {

		private final String fieldName;

		private final Class<?> declaringClass;

		private transient Field field;

		public FieldTypeProvider(Field field) {
			this.fieldName = field.getName();
			this.declaringClass = field.getDeclaringClass();
			this.field = field;
		}

		@Override
		public Type getType() {
			return this.field.getGenericType();
		}

		@Override
		public Object getSource() {
			return this.field;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * 从{@link MethodParameter}获取的{@link Type}的{@link TypeProvider}.
	 */
	@SuppressWarnings("serial")
	static class MethodParameterTypeProvider implements TypeProvider {

		private final String methodName;

		private final Class<?>[] parameterTypes;

		private final Class<?> declaringClass;

		private final int parameterIndex;

		private transient MethodParameter methodParameter;

		public MethodParameterTypeProvider(MethodParameter methodParameter) {
			if (methodParameter.getMethod() != null) {
				this.methodName = methodParameter.getMethod().getName();
				this.parameterTypes = methodParameter.getMethod().getParameterTypes();
			}
			else {
				this.methodName = null;
				this.parameterTypes = methodParameter.getConstructor().getParameterTypes();
			}
			this.declaringClass = methodParameter.getDeclaringClass();
			this.parameterIndex = methodParameter.getParameterIndex();
			this.methodParameter = methodParameter;
		}


		@Override
		public Type getType() {
			return this.methodParameter.getGenericParameterType();
		}

		@Override
		public Object getSource() {
			return this.methodParameter;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * 通过调用no-arg方法获得的{@link Type}的{@link TypeProvider}.
	 */
	@SuppressWarnings("serial")
	static class MethodInvokeTypeProvider implements TypeProvider {

		private final TypeProvider provider;

		private final String methodName;

		private final Class<?> declaringClass;

		private final int index;

		private transient Method method;

		private transient volatile Object result;

		public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
			this.provider = provider;
			this.methodName = method.getName();
			this.declaringClass = method.getDeclaringClass();
			this.index = index;
			this.method = method;
		}

		@Override
		public Type getType() {
			Object result = this.result;
			if (result == null) {
				// 在提供的类型上延迟调用目标方法
				result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
				// 缓存结果以进一步调用getType()
				this.result = result;
			}
			return (result instanceof Type[] ? ((Type[]) result)[this.index] : (Type) result);
		}

		@Override
		public Object getSource() {
			return null;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			this.method = ReflectionUtils.findMethod(this.declaringClass, this.methodName);
			if (this.method.getReturnType() != Type.class && this.method.getReturnType() != Type[].class) {
				throw new IllegalStateException(
						"Invalid return type on deserialized method - needs to be Type or Type[]: " + this.method);
			}
		}
	}

}
