package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * 运算符'instanceof'检查对象是否属于右侧操作数中指定的类, 与{@code instanceof}在Java中的方式相同.
 */
public class OperatorInstanceof extends Operator {

	private Class<?> type;
	

	public OperatorInstanceof(int pos, SpelNodeImpl... operands) {
		super("instanceof", pos, operands);
	}


	/**
	 * 比较左操作数以查看它是指定为右操作数的类型的实例. 右操作数必须是一个类.
	 * 
	 * @param state 表达式状态
	 * 
	 * @return {@code true} 如果左操作数是 instanceof 右操作数的, 否则{@code false}
	 * @throws EvaluationException 如果评估表达式有问题
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl rightOperand = getRightOperand();
		TypedValue left = getLeftOperand().getValueInternal(state);
		TypedValue right = rightOperand.getValueInternal(state);
		Object leftValue = left.getValue();
		Object rightValue = right.getValue();
		BooleanTypedValue result;
		if (rightValue == null || !(rightValue instanceof Class)) {
			throw new SpelEvaluationException(getRightOperand().getStartPosition(),
					SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND,
					(rightValue == null ? "null" : rightValue.getClass().getName()));
		}
		Class<?> rightClass = (Class<?>) rightValue;
		if (leftValue == null) {
			result = BooleanTypedValue.FALSE;  // null is not an instanceof anything
		}
		else {
			result = BooleanTypedValue.forValue(rightClass.isAssignableFrom(leftValue.getClass()));
		}
		this.type = rightClass;
		if (rightOperand instanceof TypeReference) {
			// 只能生成右操作数是直接类型引用的字节码, 而不是它是间接的 (例如, 当右操作数是变量引用时)
			this.exitTypeDescriptor = "Z";
		}
		return result;
	}

	@Override
	public boolean isCompilable() {
		return (this.exitTypeDescriptor != null && getLeftOperand().isCompilable());
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		getLeftOperand().generateCode(mv, cf);
		CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
		if (this.type.isPrimitive()) {
			// 总是 false - 但是左操作数代码总是被驱动以防它有副作用
			mv.visitInsn(POP);
			mv.visitInsn(ICONST_0); // value of false
		} 
		else {
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(this.type));
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
