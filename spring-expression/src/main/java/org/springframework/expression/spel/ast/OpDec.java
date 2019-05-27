package org.springframework.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;

/**
 * 递减运算符.
 * 可以用于前缀或后缀形式.
 * 如果请求的操作数不支持递减, 这将抛出适当的异常.
 */
public class OpDec extends Operator {

	private final boolean postfix;  // false 表示前缀


	public OpDec(int pos, boolean postfix, SpelNodeImpl... operands) {
		super("--", pos, operands);
		this.postfix = postfix;
		Assert.notEmpty(operands, "Operands must not be empty");
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl operand = getLeftOperand();

		// 操作数将被读取然后分配给, 我们不想评估它两次.
		ValueRef lvalue = operand.getValueRef(state);

		TypedValue operandTypedValue = lvalue.getValue();  //operand.getValueInternal(state);
		Object operandValue = operandTypedValue.getValue();
		TypedValue returnValue = operandTypedValue;
		TypedValue newValue = null;

		if (operandValue instanceof Number) {
			Number op1 = (Number) operandValue;
			if (op1 instanceof BigDecimal) {
				newValue = new TypedValue(((BigDecimal) op1).subtract(BigDecimal.ONE), operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Double) {
				newValue = new TypedValue(op1.doubleValue() - 1.0d, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Float) {
				newValue = new TypedValue(op1.floatValue() - 1.0f, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof BigInteger) {
				newValue = new TypedValue(((BigInteger) op1).subtract(BigInteger.ONE), operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Long) {
				newValue = new TypedValue(op1.longValue() - 1L, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Integer) {
				newValue = new TypedValue(op1.intValue() - 1, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Short) {
				newValue = new TypedValue(op1.shortValue() - (short) 1, operandTypedValue.getTypeDescriptor());
			}
			else if (op1 instanceof Byte) {
				newValue = new TypedValue(op1.byteValue() - (byte) 1, operandTypedValue.getTypeDescriptor());
			}
			else {
				// 未知Number子类型 -> 最佳猜测是双重减少
				newValue = new TypedValue(op1.doubleValue() - 1.0d, operandTypedValue.getTypeDescriptor());
			}
		}

		if (newValue == null) {
			try {
				newValue = state.operate(Operation.SUBTRACT, returnValue.getValue(), 1);
			}
			catch (SpelEvaluationException ex) {
				if (ex.getMessageCode() == SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES) {
					// 这意味着操作数不可递减
					throw new SpelEvaluationException(operand.getStartPosition(),
							SpelMessage.OPERAND_NOT_DECREMENTABLE, operand.toStringAST());
				}
				else {
					throw ex;
				}
			}
		}

		// set the new value
		try {
			lvalue.setValue(newValue.getValue());
		}
		catch (SpelEvaluationException see) {
			// 如果无法设置该值, 则操作数不可写 (e.g. 1-- )
			if (see.getMessageCode() == SpelMessage.SETVALUE_NOT_SUPPORTED) {
				throw new SpelEvaluationException(operand.getStartPosition(),
						SpelMessage.OPERAND_NOT_DECREMENTABLE);
			}
			else {
				throw see;
			}
		}

		if (!this.postfix) {
			// 返回值是新值, 而不是原始值
			returnValue = newValue;
		}

		return returnValue;
	}

	@Override
	public String toStringAST() {
		return getLeftOperand().toStringAST() + "--";
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		return null;
	}

}
