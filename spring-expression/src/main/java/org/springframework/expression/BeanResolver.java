package org.springframework.expression;

/**
 * 可以在评估上下文中注册bean解析器, 并启动bean引用:
 * {@code @myBeanName}和{@code &myBeanName}表达式.
 * <tt>&</tt> 变体语法允许在相关时访问工厂bean.
 */
public interface BeanResolver {

	/**
	 * 按给定名称查找bean并为其返回相应的实例.
	 * 要尝试访问工厂bean, 名称需要<tt>&</tt>前缀.
	 * 
	 * @param context 当前评估上下文
	 * @param beanName 要查找的bean的名称
	 * 
	 * @return 表示bean的对象
	 * @throws AccessException 如果解析bean有意外问题
	 */
	Object resolve(EvaluationContext context, String beanName) throws AccessException;

}
