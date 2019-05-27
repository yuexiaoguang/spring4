package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;

/**
 * 表达语言AST节点, 表示真正的文字.
 */
public class RealLiteral extends Literal {

	private final TypedValue value;


	public RealLiteral(String payload, int pos, double value) {
		super(payload, pos);
		this.value = new TypedValue(value);
		this.exitTypeDescriptor = "D";
	}


	@Override
	public TypedValue getLiteralValue() {
		return this.value;
	}

	@Override
	public boolean isCompilable() {
		return true;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		mv.visitLdcInsn(this.value.getValue());
		cf.pushDescriptor(this.exitTypeDescriptor);
	}


}
