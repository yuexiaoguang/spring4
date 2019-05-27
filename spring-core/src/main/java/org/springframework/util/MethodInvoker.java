package org.springframework.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Helper类, 允许指定以声明方式调用的方法, 无论是静态还是非静态.
 *
 * <p>Usage: 指定"targetClass"/"targetMethod"或"targetObject"/"targetMethod", 可选的指定参数, 准备调用器.
 * 之后, 可以多次调用该方法, 获取调用结果.
 */
public class MethodInvoker {

	private Class<?> targetClass;

	private Object targetObject;

	private String targetMethod;

	private String staticMethod;

	private Object[] arguments;

	/** 将调用的方法 */
	private Method methodObject;


	/**
	 * 设置要在其上调用目标方法的目标类.
	 * 仅在目标方法是静态时才需要; 否则, 无论如何都需要指定目标对象.
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 返回要调用目标方法的目标类.
	 */
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * 设置要调用目标方法的目标对象.
	 * 仅在目标方法不是静态时才需要; 否则, 目标类就足够了.
	 */
	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
		if (targetObject != null) {
			this.targetClass = targetObject.getClass();
		}
	}

	/**
	 * 返回要调用目标方法的目标对象.
	 */
	public Object getTargetObject() {
		return this.targetObject;
	}

	/**
	 * 设置要调用的方法的名称.
	 * 引用静态方法或非静态方法, 具体取决于要设置的目标对象.
	 */
	public void setTargetMethod(String targetMethod) {
		this.targetMethod = targetMethod;
	}

	/**
	 * 返回要调用的方法的名称.
	 */
	public String getTargetMethod() {
		return this.targetMethod;
	}

	/**
	 * 设置要调用的完全限定的静态方法名称, e.g. "example.MyExampleClass.myExampleMethod".
	 * 指定targetClass和targetMethod的便捷替代方法.
	 */
	public void setStaticMethod(String staticMethod) {
		this.staticMethod = staticMethod;
	}

	/**
	 * 设置方法调用的参数.
	 * 如果未设置此属性, 或者Object数组的长度为0, 则假定不带参数的方法.
	 */
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}

	/**
	 * 返回方法调用的参数.
	 */
	public Object[] getArguments() {
		return (this.arguments != null ? this.arguments : new Object[0]);
	}


	/**
	 * 准备指定的方法.
	 * 之后可以多次调用该方法.
	 */
	public void prepare() throws ClassNotFoundException, NoSuchMethodException {
		if (this.staticMethod != null) {
			int lastDotIndex = this.staticMethod.lastIndexOf('.');
			if (lastDotIndex == -1 || lastDotIndex == this.staticMethod.length()) {
				throw new IllegalArgumentException(
						"staticMethod must be a fully qualified class plus method name: " +
						"e.g. 'example.MyExampleClass.myExampleMethod'");
			}
			String className = this.staticMethod.substring(0, lastDotIndex);
			String methodName = this.staticMethod.substring(lastDotIndex + 1);
			this.targetClass = resolveClassName(className);
			this.targetMethod = methodName;
		}

		Class<?> targetClass = getTargetClass();
		String targetMethod = getTargetMethod();
		Assert.notNull(targetClass, "Either 'targetClass' or 'targetObject' is required");
		Assert.notNull(targetMethod, "Property 'targetMethod' is required");

		Object[] arguments = getArguments();
		Class<?>[] argTypes = new Class<?>[arguments.length];
		for (int i = 0; i < arguments.length; ++i) {
			argTypes[i] = (arguments[i] != null ? arguments[i].getClass() : Object.class);
		}

		// 首先尝试获取确切的方法.
		try {
			this.methodObject = targetClass.getMethod(targetMethod, argTypes);
		}
		catch (NoSuchMethodException ex) {
			// 如果无法获得任何匹配, 重新抛出异常.
			this.methodObject = findMatchingMethod();
			if (this.methodObject == null) {
				throw ex;
			}
		}
	}

	/**
	 * 将给定的类名解析为Class.
	 * <p>默认实现使用{@code ClassUtils.forName}, 使用线程上下文类加载器.
	 * 
	 * @param className 要解析的类名
	 * 
	 * @return 解析后的Class
	 * @throws ClassNotFoundException 如果类名无效
	 */
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 查找具有指定参数的指定名称的匹配方法.
	 * 
	 * @return 匹配的方法, 或{@code null}
	 */
	protected Method findMatchingMethod() {
		String targetMethod = getTargetMethod();
		Object[] arguments = getArguments();
		int argCount = arguments.length;

		Method[] candidates = ReflectionUtils.getAllDeclaredMethods(getTargetClass());
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Method matchingMethod = null;

		for (Method candidate : candidates) {
			if (candidate.getName().equals(targetMethod)) {
				Class<?>[] paramTypes = candidate.getParameterTypes();
				if (paramTypes.length == argCount) {
					int typeDiffWeight = getTypeDifferenceWeight(paramTypes, arguments);
					if (typeDiffWeight < minTypeDiffWeight) {
						minTypeDiffWeight = typeDiffWeight;
						matchingMethod = candidate;
					}
				}
			}
		}

		return matchingMethod;
	}

	/**
	 * 返回将被调用的已准备好的Method对象.
	 * <p>例如, 可以用于确定返回类型.
	 * 
	 * @return 已准备好的Method对象 (never {@code null})
	 * @throws IllegalStateException 如果调用器尚未准备好
	 */
	public Method getPreparedMethod() throws IllegalStateException {
		if (this.methodObject == null) {
			throw new IllegalStateException("prepare() must be called prior to invoke() on MethodInvoker");
		}
		return this.methodObject;
	}

	/**
	 * 返回是否已经准备好这个调用器, i.e. 它是否允许访问{@link #getPreparedMethod()}.
	 */
	public boolean isPrepared() {
		return (this.methodObject != null);
	}

	/**
	 * 调用指定的方法.
	 * <p>调用器需要事先准备好.
	 * 
	 * @return 方法调用返回的对象 (可能为 null), 或{@code null} 如果方法具有void返回类型
	 * @throws InvocationTargetException 如果目标方法引发异常
	 * @throws IllegalAccessException 如果无法访问目标方法
	 */
	public Object invoke() throws InvocationTargetException, IllegalAccessException {
		// 在静态情况下, 目标只是{@code null}.
		Object targetObject = getTargetObject();
		Method preparedMethod = getPreparedMethod();
		if (targetObject == null && !Modifier.isStatic(preparedMethod.getModifiers())) {
			throw new IllegalArgumentException("Target method must not be non-static without a target");
		}
		ReflectionUtils.makeAccessible(preparedMethod);
		return preparedMethod.invoke(targetObject, getArguments());
	}


	/**
	 * 判断候选方法的声明参数类型与应该使用此方法调用的特定参数列表之间的匹配的算法.
	 * <p>确定表示类型和参数之间的类层次结构差异的权重.
	 * 直接匹配, i.e. 类型 Integer -> 类Integer的arg, 不会增加结果 - 所有直接匹配意味着权重0.
	 * 类型Object和类Integer的arg之间的匹配会使权重增加2, 因为层次结构中的超类2步 (i.e. Object) 是仍然匹配所需类型Object的最后一个.
	 * 类型Number 和类Integer会相应地将权重增加1, 因为层次结构中的超类1步 (i.e. Number) 仍然匹配所需的类型Number.
	 * 因此, 对于类型为Integer的arg, 构造函数 (Integer) 将优先于构造函数 (Number), 而构造函数 (Number)又优先于构造函数(Object).
	 * 所有参数权重都会累积.
	 * <p>Note: 这是MethodInvoker本身使用的算法, 也是Spring的bean容器中用于构造函数和工厂方法选择的算法
	 * (如果是宽松的构造函数解析, 即常规bean定义的默认值).
	 * 
	 * @param paramTypes 要匹配的参数类型
	 * @param args 要匹配的参数
	 * 
	 * @return 所有参数的累加权重
	 */
	public static int getTypeDifferenceWeight(Class<?>[] paramTypes, Object[] args) {
		int result = 0;
		for (int i = 0; i < paramTypes.length; i++) {
			if (!ClassUtils.isAssignableValue(paramTypes[i], args[i])) {
				return Integer.MAX_VALUE;
			}
			if (args[i] != null) {
				Class<?> paramType = paramTypes[i];
				Class<?> superClass = args[i].getClass().getSuperclass();
				while (superClass != null) {
					if (paramType.equals(superClass)) {
						result = result + 2;
						superClass = null;
					}
					else if (ClassUtils.isAssignable(paramType, superClass)) {
						result = result + 2;
						superClass = superClass.getSuperclass();
					}
					else {
						superClass = null;
					}
				}
				if (paramType.isInterface()) {
					result = result + 1;
				}
			}
		}
		return result;
	}
}
