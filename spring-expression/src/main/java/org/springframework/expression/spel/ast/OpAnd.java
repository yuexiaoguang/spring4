package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * 表示boolean AND操作.
 */
public class OpAnd extends Operator {

	public OpAnd(int pos, SpelNodeImpl... operands) {
		super("and", pos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		if (!getBooleanValue(state, getLeftOperand())) {
			// 无需评估正确的操作数
			return BooleanTypedValue.FALSE;
		}
		return BooleanTypedValue.forValue(getBooleanValue(state, getRightOperand()));
	}

	private boolean getBooleanValue(ExpressionState state, SpelNodeImpl operand) {
		try {
			Boolean value = operand.getValue(state, Boolean.class);
			assertValueNotNull(value);
			return value;
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(operand.getStartPosition());
			throw ex;
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
		// Pseudo: if (!leftOperandValue) { result=false; } else { result=rightOperandValue; }
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		cf.enterCompilationScope();
		getLeftOperand().generateCode(mv, cf);
		cf.unboxBooleanIfNecessary(mv);
		cf.exitCompilationScope();
		mv.visitJumpInsn(IFNE, elseTarget);
		mv.visitLdcInsn(0); // FALSE
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
