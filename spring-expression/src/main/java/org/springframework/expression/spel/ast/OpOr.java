package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * 表示boolean OR运算.
 */
public class OpOr extends Operator {

	public OpOr(int pos, SpelNodeImpl... operands) {
		super("or", pos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		if (getBooleanValue(state, getLeftOperand())) {
			// 无需评估正确的操作数
			return BooleanTypedValue.TRUE;
		}
		return BooleanTypedValue.forValue(getBooleanValue(state, getRightOperand()));
	}

	private boolean getBooleanValue(ExpressionState state, SpelNodeImpl operand) {
		try {
			Boolean value = operand.getValue(state, Boolean.class);
			assertValueNotNull(value);
			return value;
		}
		catch (SpelEvaluationException ee) {
			ee.setPosition(operand.getStartPosition());
			throw ee;
		}
	}

	private void assertValueNotNull(Boolean value) {
		if (value == null) {
			throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right = getRightOperand();
		return (left.isCompilable() && right.isCompilable() &&
				CodeFlow.isBooleanCompatible(left.exitTypeDescriptor) &&
				CodeFlow.isBooleanCompatible(right.exitTypeDescriptor));
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		// pseudo: if (leftOperandValue) { result=true; } else { result=rightOperandValue; }
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		cf.enterCompilationScope();
		getLeftOperand().generateCode(mv, cf);
		cf.unboxBooleanIfNecessary(mv);
		cf.exitCompilationScope();
		mv.visitJumpInsn(IFEQ, elseTarget);
		mv.visitLdcInsn(1); // TRUE
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		cf.enterCompilationScope();
		getRightOperand().generateCode(mv, cf);
		cf.unboxBooleanIfNecessary(mv);
		cf.exitCompilationScope();
		mv.visitLabel(endOfIf);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}
	
}
