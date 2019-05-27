package org.springframework.core.type;

/**
 * 定义对特定类的注解的抽象访问的接口, 不需要加载该类.
 */
public interface MethodMetadata extends AnnotatedTypeMetadata {

	/**
	 * 返回方法的名称.
	 */
	String getMethodName();

	/**
	 * 返回声明此方法的类的完全限定名称.
	 */
	String getDeclaringClassName();

	/**
	 * 返回此方法声明的返回类型的完全限定名称.
	 */
	String getReturnTypeName();

	/**
	 * 返回底层方法是否是有效的抽象:
	 * i.e. 在类上标记为抽象, 或在接口中声明为常规非默认方法.
	 */
	boolean isAbstract();

	/**
	 * 返回底层方法是否声明为 'static'.
	 */
	boolean isStatic();

	/**
	 * 返回底层方法是否声明为 'final'.
	 */
	boolean isFinal();

	/**
	 * 返回底层方法是否可以覆盖,
	 * i.e. 未标记为 static, final 或 private.
	 */
	boolean isOverridable();

}
