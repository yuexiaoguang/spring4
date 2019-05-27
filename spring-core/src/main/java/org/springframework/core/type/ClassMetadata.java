package org.springframework.core.type;

/**
 * 定义特定类的抽象元数据的接口, 不需要加载该类.
 */
public interface ClassMetadata {

	/**
	 * 返回底层类的名称.
	 */
	String getClassName();

	/**
	 * 返回底层类是否表示接口.
	 */
	boolean isInterface();

	/**
	 * 返回底层类是否表示注解.
	 */
	boolean isAnnotation();

	/**
	 * 返回基础类是否标记为 abstract.
	 */
	boolean isAbstract();

	/**
	 * 返回底层类是否表示具体类,
	 * i.e. 既不是接口也不是抽象类.
	 */
	boolean isConcrete();

	/**
	 * 返回底层类是否标记为 'final'.
	 */
	boolean isFinal();

	/**
	 * 确定底层类是否是独立的, i.e. 它是一个顶级类还是一个嵌套类 (静态内部类), 它可以独立于一个封闭类构造.
	 */
	boolean isIndependent();

	/**
	 * 返回底层类是否在封闭类中声明 (i.e. 底层类是内部/嵌套类或方法中的本地类).
	 * <p>如果此方法返回{@code false}, 则底层类是顶级类.
	 */
	boolean hasEnclosingClass();

	/**
	 * 返回底层类的封闭类的名称, 或{@code null} 如果底层类是顶级类.
	 */
	String getEnclosingClassName();

	/**
	 * 返回底层类是否具有超类.
	 */
	boolean hasSuperClass();

	/**
	 * 返回底层类的超类的名称, 或{@code null} 如果没有定义超类.
	 */
	String getSuperClassName();

	/**
	 * 返回底层类实现的所有接口的名称, 如果没有则返回空数组.
	 */
	String[] getInterfaceNames();

	/**
	 * 返回声明为此ClassMetadata对象表示的类成员的所有类的名称.
	 * 包括类声明的public, protected, 包级私有, private类, 以及接口, 但不包括继承的类和接口.
	 * 如果不存在成员类或接口, 则返回空数组.
	 */
	String[] getMemberClassNames();

}
