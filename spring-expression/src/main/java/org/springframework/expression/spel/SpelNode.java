package org.springframework.expression.spel;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;

/**
 * 表示解析表达式的Ast中的节点.
 */
public interface SpelNode {

	/**
	 * 在提供的表达式状态的上下文中评估表达式节点并返回值.
	 * 
	 * @param expressionState 当前表达式状态 (包括上下文)
	 * 
	 * @return 此节点根据指定的状态进行评估的值
	 */
	Object getValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 在提供的表达式状态的上下文中评估表达式节点, 并返回类型值.
	 * 
	 * @param expressionState 当前表达式状态 (包括上下文)
	 * 
	 * @return 根据指定的状态评估此节点的类型值
	 */
	TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 确定此表达式节点是否支持 setValue() 调用.
	 * 
	 * @param expressionState 当前表达式状态 (包括上下文)
	 * 
	 * @return true 如果表达式节点将允许 setValue()
	 * @throws EvaluationException 如果出现问题
	 */
	boolean isWritable(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 将表达式计算到节点, 然后在该节点上设置新值.
	 * 例如, 如果表达式求值为属性引用, 则该属性将设置为新值.
	 * 
	 * @param expressionState 当前表达式状态 (包括上下文)
	 * @param newValue 新值
	 * 
	 * @throws EvaluationException 如果在评估表达式或设置新值时出现任何问题
	 */
	void setValue(ExpressionState expressionState, Object newValue) throws EvaluationException;

	/**
	 * @return 此AST节点的字符串形式
	 */
	String toStringAST();

	/**
	 * @return 此节点下的子节点数
	 */
	int getChildCount();

	/**
	 * 返回SpelNode而不是Antlr Tree节点的Helper方法.
	 * 
	 * @return 子节点转换为SpelNode
	 */
	SpelNode getChild(int index);

	/**
	 * 确定传入的对象的类, 除非它已经是类对象.
	 * 
	 * @param obj 调用者想要Class的对象
	 * 
	 * @return 对象的Class, 如果它还不是Class对象, 或{@code null} 如果对象是{@code null}
	 */
	Class<?> getObjectClass(Object obj);

	/**
	 * @return 表达式字符串中此Ast节点的起始位置
	 */
	int getStartPosition();

	/**
	 * @return 此Ast节点在表达式字符串中的结束位置
	 */
	int getEndPosition();

}
