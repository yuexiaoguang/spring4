package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Helper类, 用于根据类型变量解析泛型类型.
 *
 * <p>主要用于在框架内使用, 解析方法参数类型, 即使它们被一般声明.
 */
public abstract class GenericTypeResolver {

	/** Cache from Class to TypeVariable Map */
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache =
			new ConcurrentReferenceHashMap<Class<?>, Map<TypeVariable, Type>>();


	/**
	 * 确定给定参数规范的目标类型.
	 * 
	 * @param methodParameter 方法参数规范
	 * 
	 * @return 相应的泛型参数类型
	 * @deprecated as of Spring 4.0, use {@link MethodParameter#getGenericParameterType()}
	 */
	@Deprecated
	public static Type getTargetType(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return methodParameter.getGenericParameterType();
	}

	/**
	 * 确定给定泛型参数类型的目标类型.
	 * 
	 * @param methodParameter 方法参数规范
	 * @param implementationClass 用于解析类型变量的类
	 * 
	 * @return 相应的泛型参数或返回类型
	 */
	public static Class<?> resolveParameterType(MethodParameter methodParameter, Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "Class must not be null");
		methodParameter.setContainingClass(implementationClass);
		ResolvableType.resolveMethodParameter(methodParameter);
		return methodParameter.getParameterType();
	}

	/**
	 * 确定给定方法的泛型返回类型的目标类型, 其中泛型在给定类上声明形式类型变量.
	 * 
	 * @param method 要内省的方法
	 * @param clazz 用于解析类型变量的类
	 * 
	 * @return 相应的泛型参数或返回类型
	 */
	public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(clazz, "Class must not be null");
		return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
	}

	/**
	 * 确定给定<em>泛型方法</em>的泛型返回类型的目标类型, 其中泛型在给定方法本身上声明形式类型变量.
	 * <p>例如, 给定具有以下签名的工厂方法,
	 * 如果使用{@code creatProxy()}和包含{@code MyService.class}的{@code Object []}数组的反射方法,
	 * 调用{@code resolveReturnTypeForGenericMethod()},
	 * {@code resolveReturnTypeForGenericMethod()}将推断目标返回类型为{@code MyService}.
	 * <pre class="code">{@code public static <T> T createProxy(Class<T> clazz)}</pre>
	 * <h4>可能的返回值</h4>
	 * <ul>
	 * <li>目标返回类型, 如果可以推断的话</li>
	 * <li>{@linkplain Method#getReturnType() 标准返回类型},
	 * 如果给定的{@code method}没有声明任何 {@linkplain Method#getTypeParameters() 形式类型变量}</li>
	 * <li>{@linkplain Method#getReturnType() 标准返回类型}, 如果无法推断目标返回类型 (e.g., 由于类型擦除)</li>
	 * <li>{@code null}, 如果给定参数数组的长度短于给定方法的{@linkplain Method#getGenericParameterTypes() 形式参数列表}的长度</li>
	 * </ul>
	 * 
	 * @param method 要内省的方法, never {@code null}
	 * @param args 调用时将提供给方法的参数 (never {@code null})
	 * @param classLoader 用于解析类名的ClassLoader (may be {@code null})
	 * 
	 * @return 已解析的目标返回类型, 标准返回类型, 或{@code null}
	 * @deprecated as of Spring Framework 4.3.8, superseded by {@link ResolvableType} usage
	 */
	@Deprecated
	public static Class<?> resolveReturnTypeForGenericMethod(Method method, Object[] args, ClassLoader classLoader) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(args, "Argument array must not be null");

		TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
		Type genericReturnType = method.getGenericReturnType();
		Type[] methodArgumentTypes = method.getGenericParameterTypes();

		// 没有要检查的声明类型变量, 因此只返回标准返回类型.
		if (declaredTypeVariables.length == 0) {
			return method.getReturnType();
		}

		// 提供的参数列表对于方法的签名来说太短, 因此返回null, 因为这样的方法调用将失败.
		if (args.length < methodArgumentTypes.length) {
			return null;
		}

		// 确保类型变量 (e.g., T) 直接在方法本身上声明 (e.g., 通过<T>), 而不是在封闭的类或接口上.
		boolean locallyDeclaredTypeVariableMatchesReturnType = false;
		for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
			if (currentTypeVariable.equals(genericReturnType)) {
				locallyDeclaredTypeVariableMatchesReturnType = true;
				break;
			}
		}

		if (locallyDeclaredTypeVariableMatchesReturnType) {
			for (int i = 0; i < methodArgumentTypes.length; i++) {
				Type currentMethodArgumentType = methodArgumentTypes[i];
				if (currentMethodArgumentType.equals(genericReturnType)) {
					return args[i].getClass();
				}
				if (currentMethodArgumentType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) currentMethodArgumentType;
					Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
					for (Type typeArg : actualTypeArguments) {
						if (typeArg.equals(genericReturnType)) {
							Object arg = args[i];
							if (arg instanceof Class) {
								return (Class<?>) arg;
							}
							else if (arg instanceof String && classLoader != null) {
								try {
									return classLoader.loadClass((String) arg);
								}
								catch (ClassNotFoundException ex) {
									throw new IllegalStateException(
											"Could not resolve specific class name argument [" + arg + "]", ex);
								}
							}
							else {
								// 如果可能, 请考虑添加逻辑以确定typeArg的类.
								// For now, just fall back...
								return method.getReturnType();
							}
						}
					}
				}
			}
		}

		// Fall back...
		return method.getReturnType();
	}

	/**
	 * 根据给定的目标方法解析给定泛型接口的单一类型参数, 假定该方法返回给定的接口或其实现.
	 * 
	 * @param method 要检查返回类型的目标方法
	 * @param genericIfc 用于解析类型参数的泛型接口或超类
	 * 
	 * @return 方法返回类型的已解析的参数类型; 或{@code null}如果不可解析, 或者单个参数的类型为{@link WildcardType}.
	 */
	public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
		Assert.notNull(method, "Method must not be null");
		ResolvableType resolvableType = ResolvableType.forMethodReturnType(method).as(genericIfc);
		if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	/**
	 * 根据给定的目标类解析给定泛型接口的单一类型参数, 假定该目标类实现泛型接口, 并可能为其类型变量声明具体类型.
	 * 
	 * @param clazz 要检查的目标类
	 * @param genericIfc 用于解析类型参数的泛型接口或超类
	 * 
	 * @return 参数已解析的类型, 或{@code null}
	 */
	public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
		ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
		if (!resolvableType.hasGenerics()) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
		if (resolvableType.getGenerics().length > 1) {
			throw new IllegalArgumentException("Expected 1 type argument on generic interface [" +
					resolvableType + "] but found " + resolvableType.getGenerics().length);
		}
		return resolvableType.getGeneric().resolve();
	}


	/**
	 * 根据给定的目标类解析给定泛型接口的类型参数, 假定该目标类实现泛型接口, 并可能为其类型变量声明具体类型.
	 * 
	 * @param clazz 要检查的目标类
	 * @param genericIfc 用于解析类型参数的泛型接口或超类
	 * 
	 * @return 每个参数已解析的类型, 数组大小与实际类型参数的数量匹配, 或{@code null}
	 */
	public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
		ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
		if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
			return null;
		}
		return type.resolveGenerics(Object.class);
	}

	/**
	 * 根据给定的TypeVariable Map, 解析指定的泛型类型.
	 * <p>由Spring Data使用.
	 * 
	 * @param genericType 要解析的泛型类型
	 * @param map 要解析的TypeVariable Map
	 * 
	 * @return 解析为Class的类型, 否则为{@code Object.class}
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
		return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).resolve(Object.class);
	}

	/**
	 * 为指定的{@link Class}构建{@link TypeVariable#getName TypeVariable names}到{@link Class 具体类}的映射.
	 * 搜索所有的超类型, 包含类型和接口.
	 */
	@SuppressWarnings("rawtypes")
	public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
		Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
		if (typeVariableMap == null) {
			typeVariableMap = new HashMap<TypeVariable, Type>();
			buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
			typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
		}
		return typeVariableMap;
	}

	@SuppressWarnings("rawtypes")
	private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
		if (type != ResolvableType.NONE) {
			if (type.getType() instanceof ParameterizedType) {
				TypeVariable<?>[] variables = type.resolve().getTypeParameters();
				for (int i = 0; i < variables.length; i++) {
					ResolvableType generic = type.getGeneric(i);
					while (generic.getType() instanceof TypeVariable<?>) {
						generic = generic.resolveType();
					}
					if (generic != ResolvableType.NONE) {
						typeVariableMap.put(variables[i], generic.getType());
					}
				}
			}
			buildTypeVariableMap(type.getSuperType(), typeVariableMap);
			for (ResolvableType interfaceType : type.getInterfaces()) {
				buildTypeVariableMap(interfaceType, typeVariableMap);
			}
			if (type.resolve().isMemberClass()) {
				buildTypeVariableMap(ResolvableType.forClass(type.resolve().getEnclosingClass()), typeVariableMap);
			}
		}
	}


	@SuppressWarnings({"serial", "rawtypes"})
	private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {

		private final Map<TypeVariable, Type> typeVariableMap;

		public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
			this.typeVariableMap = typeVariableMap;
		}

		@Override
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			Type type = this.typeVariableMap.get(variable);
			return (type != null ? ResolvableType.forType(type) : null);
		}

		@Override
		public Object getSource() {
			return this.typeVariableMap;
		}
	}
}
