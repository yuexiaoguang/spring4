package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.StringUtils;

/**
 * 表示 elvis 操作符 ?:.
 * 对于表达式 "a?:b", 如果a不为null, 则表达式的值为 "a", 如果a为null, 则表达式的值为 "b".
 */
public class Elvis extends SpelNodeImpl {

	public Elvis(int pos, SpelNodeImpl... args) {
		super(pos, args);
	}


	/**
	 * 评估条件, 如果不为null, 则返回它.
	 * 如果为null, 则返回另一个值.
	 * 
	 * @param state 表达式状态
	 * 
	 * @throws EvaluationException 如果条件未正确评估为boolean, 或者执行所选备选方案时出现问题
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue value = this.children[0].getValueInternal(state);
		// 如果更改此检查, 则generateCode方法也需要更改
		if (!StringUtils.isEmpty(value.getValue())) {
			return value;
		}
		else {
			TypedValue result = this.children[1].getValueInternal(state);
			computeExitTypeDescriptor();
			return result;
		}
	}

	@Override
	public String toStringAST() {
		return getChild(0).toStringAST() + " ?: " + getChild(1).toStringAST();
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl condition = this.children[0];
		SpelNodeImpl ifNullValue = this.children[1];
		return (condition.isCompilable() && ifNullValue.isCompilable() &&
				condition.exitTypeDescriptor != null && ifNullValue.exitTypeDescriptor != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		// 如果两个组件都是文字表达式, 则exit类型描述符可以为null
		computeExitTypeDescriptor();
		this.children[0].generateCode(mv, cf);
		CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor().charAt(0));
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		mv.visitInsn(DUP);
		mv.visitJumpInsn(IFNULL, elseTarget);
		// 还要根据解释版本中的代码检查是否为空字符串
		mv.visitInsn(DUP);
		mv.visitLdcInsn("");
		mv.visitInsn(SWAP);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",false);
		mv.visitJumpInsn(IFEQ, endOfIf);  // if not empty, drop through to elseTarget
		mv.visitLabel(elseTarget);
		mv.visitInsn(POP);
		this.children[1].generateCode(mv, cf);
		if (!CodeFlow.isPrimitive(this.exitTypeDescriptor)) {
			CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor().charAt(0));
		}
		mv.visitLabel(endOfIf);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

	private void computeExitTypeDescriptor() {
		if (this.exitTypeDescriptor == null && this.children[0].exitTypeDescriptor != null &&
				this.children[1].exitTypeDescriptor != null) {
			String conditionDescriptor = this.children[0].exitTypeDescriptor;
			String ifNullValueDescriptor = this.children[1].exitTypeDescriptor;
			if (conditionDescriptor.equals(ifNullValueDescriptor)) {
				this.exitTypeDescriptor = conditionDescriptor;
			}
			else {
				// 使用最简单的计算常见超类型
				this.exitTypeDescriptor = "Ljava/lang/Object";
			}
		}
	}
}
