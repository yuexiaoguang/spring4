package org.springframework.expression;

import java.util.List;

/**
 * 在评估上下文中执行的表达式.
 * 在此上下文中, 在表达式评估期间遇到引用.
 *
 * <p>此EvaluationContext接口有一个默认实现:
 * {@link org.springframework.expression.spel.support.StandardEvaluationContext}可以扩展, 而不必手动实现所有内容.
 */
public interface EvaluationContext {

	/**
	 * 返回应该解析非限定属性/方法/等的默认根上下文对象.
	 * 在计算表达式时可以覆盖它.
	 */
	TypedValue getRootObject();

	/**
	 * 返回将依次要求读/写属性的访问者列表.
	 */
	List<PropertyAccessor> getPropertyAccessors();

	/**
	 * 返回一个解析器列表, 将依次询问这些解析器以找到构造函数.
	 */
	List<ConstructorResolver> getConstructorResolvers();

	/**
	 * 返回一个解析器列表, 将依次询问这些解析器以查找方法.
	 */
	List<MethodResolver> getMethodResolvers();

	/**
	 * 返回一个可以按名称查找bean的bean解析器.
	 */
	BeanResolver getBeanResolver();

	/**
	 * 返回可用于查找类型的类型定位器, 可以是短名称或完全限定名称.
	 */
	TypeLocator getTypeLocator();

	/**
	 * 返回一个类型转换器, 可以将值从一种类型转换(或强制)为另一种类型.
	 */
	TypeConverter getTypeConverter();

	/**
	 * 返回一个类型比较器, 用于比较对象对的相等性.
	 */
	TypeComparator getTypeComparator();

	/**
	 * 返回一个运算符overloader, 它可以支持多个标准类型集之间的数学运算.
	 */
	OperatorOverloader getOperatorOverloader();

	/**
	 * 在此评估上下文中将命名变量设置为指定值.
	 * 
	 * @param name 要设置的变量
	 * @param value 要放在变量中的值
	 */
	void setVariable(String name, Object value);

	/**
	 * 在此评估上下文中查找命名变量.
	 * 
	 * @param name 要查找的变量
	 * 
	 * @return 变量的值, 或{@code null}
	 */
	Object lookupVariable(String name);

}
