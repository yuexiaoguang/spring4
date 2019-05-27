package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 包含用于支持autowire的bean工厂的实现的各种方法.
 */
abstract class AutowireUtils {

	/**
	 * 对给定的构造函数进行排序, 首选public构造函数和具有最大参数数量的“贪婪”构造函数.
	 * 结果首先包含public构造函数, 参数数量递减, 然后是非public构造函数, 同样参数数量递减.
	 * 
	 * @param constructors 要排序的构造函数数组
	 */
	public static void sortConstructors(Constructor<?>[] constructors) {
		Arrays.sort(constructors, new Comparator<Constructor<?>>() {
			@Override
			public int compare(Constructor<?> c1, Constructor<?> c2) {
				boolean p1 = Modifier.isPublic(c1.getModifiers());
				boolean p2 = Modifier.isPublic(c2.getModifiers());
				if (p1 != p2) {
					return (p1 ? -1 : 1);
				}
				int c1pl = c1.getParameterTypes().length;
				int c2pl = c2.getParameterTypes().length;
				return (c1pl < c2pl ? 1 : (c1pl > c2pl ? -1 : 0));
			}
		});
	}

	/**
	 * 对给定的工厂方法进行排序, 首选public方法和最多参数的“贪婪”方法.
	 * 结果将首先包含public方法, 参数数量递减, 然后是非递减方法, 同样参数数量递减.
	 * 
	 * @param factoryMethods 要排序的工厂方法数组
	 */
	public static void sortFactoryMethods(Method[] factoryMethods) {
		Arrays.sort(factoryMethods, new Comparator<Method>() {
			@Override
			public int compare(Method fm1, Method fm2) {
				boolean p1 = Modifier.isPublic(fm1.getModifiers());
				boolean p2 = Modifier.isPublic(fm2.getModifiers());
				if (p1 != p2) {
					return (p1 ? -1 : 1);
				}
				int c1pl = fm1.getParameterTypes().length;
				int c2pl = fm2.getParameterTypes().length;
				return (c1pl < c2pl ? 1 : (c1pl > c2pl ? -1 : 0));
			}
		});
	}

	/**
	 * 确定是否从依赖性检查中排除给定的bean属性.
	 * <p>此实现不包括CGLIB定义的属性.
	 * 
	 * @param pd bean属性的PropertyDescriptor
	 * 
	 * @return 是否排除bean属性
	 */
	public static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		Method wm = pd.getWriteMethod();
		if (wm == null) {
			return false;
		}
		if (!wm.getDeclaringClass().getName().contains("$$")) {
			// Not a CGLIB method so it's OK.
			return false;
		}
		// 它是由CGLIB声明的, 但是如果它实际上是由超类声明的话, 我们可能仍然想要自动装配它.
		Class<?> superclass = wm.getDeclaringClass().getSuperclass();
		return !ClassUtils.hasMethod(superclass, wm.getName(), wm.getParameterTypes());
	}

	/**
	 * 返回是否在任何给定接口中定义给定bean属性的setter方法.
	 * 
	 * @param pd bean属性的PropertyDescriptor
	 * @param interfaces 接口集 (Class objects)
	 * 
	 * @return setter方法是否由接口定义
	 */
	public static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set<Class<?>> interfaces) {
		Method setter = pd.getWriteMethod();
		if (setter != null) {
			Class<?> targetClass = setter.getDeclaringClass();
			for (Class<?> ifc : interfaces) {
				if (ifc.isAssignableFrom(targetClass) &&
						ClassUtils.hasMethod(ifc, setter.getName(), setter.getParameterTypes())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 根据给定的必需类型解析给定的自动装配值, e.g. 一个{@link ObjectFactory}值到它的实际对象结果.
	 * 
	 * @param autowiringValue 要解析的值
	 * @param requiredType 要分配结果的类型
	 * 
	 * @return 解析后的值
	 */
	public static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
		if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
			ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
			if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
				autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(),
						new Class<?>[] {requiredType}, new ObjectFactoryDelegatingInvocationHandler(factory));
			}
			else {
				return factory.getObject();
			}
		}
		return autowiringValue;
	}

	/**
	 * 确定给定泛型工厂方法的泛型返回类型的目标类型, 在给定方法本身上声明的形式类型变量.
	 * <p>例如, 具有以下签名的给定工厂方法,
	 * 如果使用{@code creatProxy()}的反射方法和包含{@code MyService.class}的{@code Object[]}数组调用{@code resolveReturnTypeForFactoryMethod()},
	 * {@code resolveReturnTypeForFactoryMethod()} 将推断目标返回类型为{@code MyService}.
	 * <pre class="code">{@code public static <T> T createProxy(Class<T> clazz)}</pre>
	 * <h4>可能的返回值</h4>
	 * <ul>
	 * <li>目标返回类型, 如果可以推断的话</li>
	 * <li>{@linkplain Method#getReturnType() 标准返回类型}, 如果给定的{@code method} 没有声明任何{@linkplain Method#getTypeParameters() 正式类型变量}</li>
	 * <li>{@linkplain Method#getReturnType() 标准返回类型}, 如果无法推断目标返回类型 (e.g., 由于类型擦除)</li>
	 * <li>{@code null}, 如果给定参数数组的长度短于给定方法的 {@linkplain Method#getGenericParameterTypes() 形式参数列表} 的长度</li>
	 * </ul>
	 * 
	 * @param method 要反射的方法 (never {@code null})
	 * @param args 调用时将提供给方法的参数 (never {@code null})
	 * @param classLoader 要解析类名的ClassLoader (never {@code null})
	 * 
	 * @return 已解析的目标返回类型或标准方法返回类型
	 * @since 3.2.5
	 */
	public static Class<?> resolveReturnTypeForFactoryMethod(Method method, Object[] args, ClassLoader classLoader) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(args, "Argument array must not be null");
		Assert.notNull(classLoader, "ClassLoader must not be null");

		TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
		Type genericReturnType = method.getGenericReturnType();
		Type[] methodParameterTypes = method.getGenericParameterTypes();
		Assert.isTrue(args.length == methodParameterTypes.length, "Argument array does not match parameter count");

		// 确保类型变量 (e.g., T) 直接在方法本身上声明 (e.g., via <T>), 而不是在封闭的类或接口上.
		boolean locallyDeclaredTypeVariableMatchesReturnType = false;
		for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
			if (currentTypeVariable.equals(genericReturnType)) {
				locallyDeclaredTypeVariableMatchesReturnType = true;
				break;
			}
		}

		if (locallyDeclaredTypeVariableMatchesReturnType) {
			for (int i = 0; i < methodParameterTypes.length; i++) {
				Type methodParameterType = methodParameterTypes[i];
				Object arg = args[i];
				if (methodParameterType.equals(genericReturnType)) {
					if (arg instanceof TypedStringValue) {
						TypedStringValue typedValue = ((TypedStringValue) arg);
						if (typedValue.hasTargetType()) {
							return typedValue.getTargetType();
						}
						try {
							return typedValue.resolveTargetType(classLoader);
						}
						catch (ClassNotFoundException ex) {
							throw new IllegalStateException("Failed to resolve value type [" +
									typedValue.getTargetTypeName() + "] for factory method argument", ex);
						}
					}
					// Only consider argument type if it is a simple value...
					if (arg != null && !(arg instanceof BeanMetadataElement)) {
						return arg.getClass();
					}
					return method.getReturnType();
				}
				else if (methodParameterType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
					Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
					for (Type typeArg : actualTypeArguments) {
						if (typeArg.equals(genericReturnType)) {
							if (arg instanceof Class) {
								return (Class<?>) arg;
							}
							else {
								String className = null;
								if (arg instanceof String) {
									className = (String) arg;
								}
								else if (arg instanceof TypedStringValue) {
									TypedStringValue typedValue = ((TypedStringValue) arg);
									String targetTypeName = typedValue.getTargetTypeName();
									if (targetTypeName == null || Class.class.getName().equals(targetTypeName)) {
										className = typedValue.getValue();
									}
								}
								if (className != null) {
									try {
										return ClassUtils.forName(className, classLoader);
									}
									catch (ClassNotFoundException ex) {
										throw new IllegalStateException("Could not resolve class name [" + arg +
												"] for factory method argument", ex);
									}
								}
								// 考虑添加逻辑以确定typeArg的类.
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
	 * 反射的InvocationHandler, 用于延迟访问当前目标对象.
	 */
	@SuppressWarnings("serial")
	private static class ObjectFactoryDelegatingInvocationHandler implements InvocationHandler, Serializable {

		private final ObjectFactory<?> objectFactory;

		public ObjectFactoryDelegatingInvocationHandler(ObjectFactory<?> objectFactory) {
			this.objectFactory = objectFactory;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (methodName.equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (methodName.equals("hashCode")) {
				// Use hashCode of proxy.
				return System.identityHashCode(proxy);
			}
			else if (methodName.equals("toString")) {
				return this.objectFactory.toString();
			}
			try {
				return method.invoke(this.objectFactory.getObject(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
