package org.springframework.expression;

/**
 * 默认情况下, 数学运算符{@link Operation}支持简单类型, 如数字.
 * 通过提供OperatorOverloader的实现, 表达式语言的用户可以在其他类型上支持这些操作.
 */
public interface OperatorOverloader {

	/**
	 * 如果运算符重载符支持两个操作数之间的指定操作, 则返回true.
	 * 
	 * @param operation 要执行的操作
	 * @param leftOperand 左操作数
	 * @param rightOperand 右操作数
	 * 
	 * @return true 如果OperatorOverloader支持两个操作数之间的指定操作
	 * @throws EvaluationException 如果执行操作时有问题
	 */
	boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand)
			throws EvaluationException;

	/**
	 * 执行两个操作数之间指定的操作, 返回结果.
	 * 有关支持的操作, 请参阅{@link Operation}.
	 * 
	 * @param operation 要执行的操作
	 * @param leftOperand 左操作数
	 * @param rightOperand 右操作数
	 * 
	 * @return 对两个操作数执行操作的结果
	 * @throws EvaluationException 如果执行操作时有问题
	 */
	Object operate(Operation operation, Object leftOperand, Object rightOperand)
			throws EvaluationException;

}
