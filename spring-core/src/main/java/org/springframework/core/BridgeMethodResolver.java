package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 用于解析合成的{@link Method#isBridge bridge Methods}到被桥接的{@link Method}.
 *
 * <p>给定合成的{@link Method#isBridge bridge Method}, 返回被桥接的{@link Method}.
 * 当扩展其方法具有参数化参数的参数化类型时, 编译器可以创建桥接方法.
 * 在运行时调用期间, 可以通过反射调用和/或使用桥{@link Method}.
 * 当试图在{@link Method Methods}上定位注解时, 最好根据需要检查桥接{@link Method Methods}, 并查找桥接的{@link Method}.
 *
 * <p>See <a href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.12.4.5">
 * The Java Language Specification</a> for more details on the use of bridge methods.
 */
public abstract class BridgeMethodResolver {

	/**
	 * 查找提供的{@link Method bridge Method}的原始方法.
	 * <p>调用此方法传入非桥接{@link Method}实例是安全的.
	 * 在这种情况下, 提供的{@link Method}实例将直接返回给调用者.
	 * 在调用此方法之前, 调用者<strong>不</strong>需要检查桥接.
	 * 
	 * @param bridgeMethod 要内省的方法
	 * 
	 * @return 原始方法 (如果找不到更具体的方法, 可以使用桥接的方法或传入的方法)
	 */
	public static Method findBridgedMethod(Method bridgeMethod) {
		if (bridgeMethod == null || !bridgeMethod.isBridge()) {
			return bridgeMethod;
		}

		// 使用匹配的名称和参数大小收集所有方法.
		List<Method> candidateMethods = new ArrayList<Method>();
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(bridgeMethod.getDeclaringClass());
		for (Method candidateMethod : methods) {
			if (isBridgedCandidateFor(candidateMethod, bridgeMethod)) {
				candidateMethods.add(candidateMethod);
			}
		}

		// 现在执行简单的快速检查.
		if (candidateMethods.size() == 1) {
			return candidateMethods.get(0);
		}

		// 搜索候选匹配.
		Method bridgedMethod = searchCandidates(candidateMethods, bridgeMethod);
		if (bridgedMethod != null) {
			// 发现了桥接的方法...
			return bridgedMethod;
		}
		else {
			// 传入了一种桥接方法, 但找不到桥接的方法.
			return bridgeMethod;
		}
	}

	/**
	 * 如果提供的'{@code candidateMethod}'可以被认为是{@link Method}的验证候选者, 则返回{@code true},
	 * 该{@link Method}由提供的{@link Method bridge Method} {@link Method#isBridge() bridged}.
	 * 此方法执行廉价检查, 可以快速过滤一组可能的匹配.
	 */
	private static boolean isBridgedCandidateFor(Method candidateMethod, Method bridgeMethod) {
		return (!candidateMethod.isBridge() && !candidateMethod.equals(bridgeMethod) &&
				candidateMethod.getName().equals(bridgeMethod.getName()) &&
				candidateMethod.getParameterTypes().length == bridgeMethod.getParameterTypes().length);
	}

	/**
	 * 在给定的候选者中搜索桥接的方法.
	 * 
	 * @param candidateMethods 候选方法
	 * @param bridgeMethod 桥接方法
	 * 
	 * @return 桥接的方法, 或{@code null}
	 */
	private static Method searchCandidates(List<Method> candidateMethods, Method bridgeMethod) {
		if (candidateMethods.isEmpty()) {
			return null;
		}
		Method previousMethod = null;
		boolean sameSig = true;
		for (Method candidateMethod : candidateMethods) {
			if (isBridgeMethodFor(bridgeMethod, candidateMethod, bridgeMethod.getDeclaringClass())) {
				return candidateMethod;
			}
			else if (previousMethod != null) {
				sameSig = sameSig &&
						Arrays.equals(candidateMethod.getGenericParameterTypes(), previousMethod.getGenericParameterTypes());
			}
			previousMethod = candidateMethod;
		}
		return (sameSig ? candidateMethods.get(0) : null);
	}

	/**
	 * 确定桥接{@link Method}是否是提供的候选 {@link Method}的桥梁.
	 */
	static boolean isBridgeMethodFor(Method bridgeMethod, Method candidateMethod, Class<?> declaringClass) {
		if (isResolvedTypeMatch(candidateMethod, bridgeMethod, declaringClass)) {
			return true;
		}
		Method method = findGenericDeclaration(bridgeMethod);
		return (method != null && isResolvedTypeMatch(method, candidateMethod, declaringClass));
	}

	/**
	 * 在针对declaringType解析所有类型后, 如果提供的{@link Method#getGenericParameterTypes() generic Method}
	 * 和具体{@link Method}的{@link Type}签名相等, 则返回{@code true}
	 * 否则返回{@code false}.
	 */
	private static boolean isResolvedTypeMatch(Method genericMethod, Method candidateMethod, Class<?> declaringClass) {
		Type[] genericParameters = genericMethod.getGenericParameterTypes();
		Class<?>[] candidateParameters = candidateMethod.getParameterTypes();
		if (genericParameters.length != candidateParameters.length) {
			return false;
		}
		for (int i = 0; i < candidateParameters.length; i++) {
			ResolvableType genericParameter = ResolvableType.forMethodParameter(genericMethod, i, declaringClass);
			Class<?> candidateParameter = candidateParameters[i];
			if (candidateParameter.isArray()) {
				// 数组类型: 比较组件类型.
				if (!candidateParameter.getComponentType().equals(genericParameter.getComponentType().resolve(Object.class))) {
					return false;
				}
			}
			// 非数组类型: 比较类型本身.
			if (!candidateParameter.equals(genericParameter.resolve(Object.class))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 搜索泛型{@link Method}声明, 其擦除的签名与提供的桥接方法的签名匹配.
	 * 
	 * @throws IllegalStateException 如果找不到泛型声明
	 */
	private static Method findGenericDeclaration(Method bridgeMethod) {
		// 搜索具有与bridge相同签名的方法的父类型.
		Class<?> superclass = bridgeMethod.getDeclaringClass().getSuperclass();
		while (superclass != null && Object.class != superclass) {
			Method method = searchForMatch(superclass, bridgeMethod);
			if (method != null && !method.isBridge()) {
				return method;
			}
			superclass = superclass.getSuperclass();
		}

		Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bridgeMethod.getDeclaringClass());
		return searchInterfaces(interfaces, bridgeMethod);
	}

	private static Method searchInterfaces(Class<?>[] interfaces, Method bridgeMethod) {
		for (Class<?> ifc : interfaces) {
			Method method = searchForMatch(ifc, bridgeMethod);
			if (method != null && !method.isBridge()) {
				return method;
			}
			else {
				method = searchInterfaces(ifc.getInterfaces(), bridgeMethod);
				if (method != null) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * 如果提供的{@link Class}具有声明的{@link Method}, 其签名与提供的{@link Method}的签名匹配, 则返回此匹配的{@link Method},
	 * 否则返回{@code null}.
	 */
	private static Method searchForMatch(Class<?> type, Method bridgeMethod) {
		try {
			return type.getDeclaredMethod(bridgeMethod.getName(), bridgeMethod.getParameterTypes());
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 比较桥接方法的签名和它桥接的方法.
	 * 如果参数和返回类型相同, 那么它是Java 6中引入的'visibility'桥接方法,
	 * 用于修复http://bugs.sun.com/view_bug.do?bug_id=6342411.
	 * See also http://stas-blogspot.blogspot.com/2010/03/java-bridge-methods-explained.html
	 * 
	 * @return 签名是否与描述匹配
	 */
	public static boolean isVisibilityBridgeMethodPair(Method bridgeMethod, Method bridgedMethod) {
		if (bridgeMethod == bridgedMethod) {
			return true;
		}
		return (Arrays.equals(bridgeMethod.getParameterTypes(), bridgedMethod.getParameterTypes()) &&
				bridgeMethod.getReturnType().equals(bridgedMethod.getReturnType()));
	}

}
