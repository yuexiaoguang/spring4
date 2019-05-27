package org.springframework.expression.spel.ast;

import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * 表示between运算符.
 * between的左操作数必须是单个值, 右操作数必须是列表  - 如果左操作数介于 (使用已注册的比较器) 列表中的两个元素之间, 则此运算符返回true.
 * 包含之间的定义遵循SQL BETWEEN定义.
 */
public class OperatorBetween extends Operator {

	public OperatorBetween(int pos, SpelNodeImpl... operands) {
		super("between", pos, operands);
	}


	/**
	 * 根据值是否在表达的范围内返回boolean值.
	 * 第一个操作数是任何值, 而第二个操作数是两个值的列表 - 这两个值是第一个操作数允许的范围 (包括).
	 * 
	 * @param state 表达式状态
	 * 
	 * @return true 如果左操作数在指定的范围内, 否则false
	 * @throws EvaluationException 如果评估表达式有问题
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (!(right instanceof List) || ((List<?>) right).size() != 2) {
			throw new SpelEvaluationException(getRightOperand().getStartPosition(),
					SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST);
		}

		List<?> list = (List<?>) right;
		Object low = list.get(0);
		Object high = list.get(1);
		TypeComparator comp = state.getTypeComparator();
		try {
			return BooleanTypedValue.forValue(comp.compare(left, low) >= 0 && comp.compare(left, high) <= 0);
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(getStartPosition());
			throw ex;
		}
	}

}
