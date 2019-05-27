package org.springframework.expression;

/**
 * 类型比较器的实例应该能够比较对象对的相等性.
 * 返回值的规范与{@link java.lang.Comparable}的规范相同.
 */
public interface TypeComparator {

	/**
	 * 如果比较器可以比较这两个对象, 则返回{@code true}.
	 * 
	 * @param firstObject 第一个对象
	 * @param secondObject 第二个对象
	 * 
	 * @return {@code true} 如果比较器可以比较这些对象
	 */
	boolean canCompare(Object firstObject, Object secondObject);

	/**
	 * 比较两个给定的对象.
	 * 
	 * @param firstObject 第一个对象
	 * @param secondObject 第二个对象
	 * 
	 * @return 0 相等, <0 第一个小于第二个, >0 第一个大于第二个
	 * @throws EvaluationException 如果在比较期间出现问题 (或者如果它们首先不具有可比性)
	 */
	int compare(Object firstObject, Object secondObject) throws EvaluationException;

}
