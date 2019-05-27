package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * 表示三元表达式, 例如: "someCheck()?true:false".
 */
public class Ternary extends SpelNodeImpl {

	public Ternary(int pos, SpelNodeImpl... args) {
		super(pos, args);
	}


	/**
	 * 评估条件, 如果为true, 则评估第一种替代方案, 否则评估第二种方案.
	 * 
	 * @param state 表达式状态
	 * 
	 * @throws EvaluationException 如果条件未正确评估为 boolean, 或者执行所选备选方案时出现问题
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Boolean value = this.children[0].getValue(state, Boolean.class);
		if (value == null) {
			throw new SpelEvaluationException(getChild(0).getStartPosition(),
					SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
		TypedValue result = this.children[value ? 1 : 2].getValueInternal(state);
		computeExitTypeDescriptor();
		return result;
	}
	
	@Override
	public String toStringAST() {
		return getChild(0).toStringAST() + " ? " + getChild(1).toStringAST() + " : " + getChild(2).toStringAST();
	}

	private void computeExitTypeDescriptor() {
		if (this.exitTypeDescriptor == null && this.children[1].exitTypeDescriptor != null &&
				this.children[2].exitTypeDescriptor != null) {
			String leftDescriptor = this.children[1].exitTypeDescriptor;
			String rightDescriptor = this.children[2].exitTypeDescriptor;
			if (leftDescriptor.equals(rightDescriptor)) {
				this.exitTypeDescriptor = leftDescriptor;
			}
			else {
				// 使用最简单的计算常见超类型
				this.exitTypeDescriptor = "Ljava/lang/Object";
			}
		}
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl condition = this.children[0];
		SpelNodeImpl left = this.children[1];
		SpelNodeImpl right = this.children[2];
		return (condition.isCompilable() && left.isCompilable() && right.isCompilable() &&
				CodeFlow.isBooleanCompatible(condition.exitTypeDescriptor) &&
				left.exitTypeDescriptor != null && right.exitTypeDescriptor != null);
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		// 如果所有元素都是文字, 可以在没有它的情况下到达此处
		computeExitTypeDescriptor();
		cf.enterCompilationScope();
		this.children[0].generateCode(mv, cf);
		if (!CodeFlow.isPrimitive(cf.lastDescriptor())) {
			CodeFlow.insertUnboxInsns(mv, 'Z', cf.lastDescriptor());
		}
		cf.exitCompilationScope();
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		mv.visitJumpInsn(IFEQ, elseTarget);
		cf.enterCompilationScope();
		this.children[1].generateCode(mv, cf);
		if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
			CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor().charAt(0));
		}
		cf.exitCompilationScope();
		mv.visitJumpInsn(GOTO, endOfIf);
		mv.visitLabel(elseTarget);
		cf.enterCompilationScope();
		this.children[2].generateCode(mv, cf);
		if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
			CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor().charAt(0));
		}
		cf.exitCompilationScope();
		mv.visitLabel(endOfIf);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
